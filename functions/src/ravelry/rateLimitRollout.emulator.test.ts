import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import { stripTypeScriptTypes } from "node:module";
import { resolve } from "node:path";
import { it } from "node:test";
import { pathToFileURL } from "node:url";
import { Firestore } from "firebase-admin/firestore";

import {
  createRavelryRateLimiter,
  createRavelryRateLimitRuntimeState,
  RAVELRY_GLOBAL_RATE_LIMIT_RULES,
} from "./rateLimit";

const emulatorHost = process.env.FIRESTORE_EMULATOR_HOST;
const bucket = "auth";
const { limit, windowMillis } = RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket];
const windowStart = 2 * windowMillis;
const testOptions = { skip: !emulatorHost, timeout: 90_000 };

type Writer = { consume(uid: string, bucket: "auth"): Promise<void> };
type LegacyModule = {
  createRavelryRateLimiter(firestore: Firestore, now: () => number): Writer;
  RAVELRY_GLOBAL_RATE_LIMIT_RULES: typeof RAVELRY_GLOBAL_RATE_LIMIT_RULES;
};

async function loadLegacy(): Promise<LegacyModule> {
  const source = execFileSync(
    "git",
    ["show", "6f8f8bfe4e922c8c75501d6df8f27e6a99f01e3f:functions/src/ravelry/rateLimit.ts"],
    { cwd: __dirname, encoding: "utf8" },
  );
  const configUrl = pathToFileURL(resolve(__dirname, "../config.js")).href;
  // The Functions Node 22 runtime supports parameter-property transformation.
  // The installed Node 26 type declarations only describe strip mode.
  const transform = stripTypeScriptTypes as (source: string, options: { mode: string }) => string;
  const output = transform(source.replace('"../config"', JSON.stringify(configUrl)), { mode: "transform" });
  // Preserve native import() for this in-memory ESM fixture in the CommonJS test build.
  const importModule = new Function("url", "return import(url)") as (url: string) => Promise<LegacyModule>;
  return importModule(`data:text/javascript;base64,${Buffer.from(output).toString("base64")}`);
}

async function withWriters(run: (firestore: Firestore, legacy: LegacyModule) => Promise<void>) {
  assert.match(emulatorHost ?? "", /^(127\.0\.0\.1|localhost):\d+$/);
  const legacy = await loadLegacy();
  assert.deepEqual(legacy.RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket], { limit, windowMillis });
  const projectId = `demo-quota-${randomUUID().slice(0, 8)}`;
  const firestore = new Firestore({ projectId, host: emulatorHost, ssl: false });
  try {
    await run(firestore, legacy);
  } finally {
    await firestore.terminate();
  }
}

function admissionCounter() {
  const admitted = { old: 0, current: 0 };
  let nextUid = 0;
  return {
    admitted,
    async consume(revision: keyof typeof admitted, writer: Writer, requests = 1) {
      for (let index = 0; index < requests; index++) {
        try {
          await writer.consume(`user-${nextUid++}`, bucket);
          admitted[revision]++;
        } catch (error) {
          assert.match(String(error), /ravelry_rate_limited/);
          assert.equal((error as { scope: string }).scope, "global");
        }
      }
    },
    assertCap() {
      console.log({ limit, ...admitted, total: admitted.old + admitted.current });
      assert.equal(admitted.old + admitted.current, limit);
    },
  };
}

for (const order of ["legacy-first", "current-first", "concurrent"] as const) {
  it(`shares one global quota with the historical writer: ${order}`, testOptions, async () => {
    await withWriters(async (firestore, legacy) => {
      const old = legacy.createRavelryRateLimiter(firestore, () => windowStart);
      const current = createRavelryRateLimiter(
        firestore, () => windowStart, () => 0, createRavelryRateLimitRuntimeState(),
      );
      const counts = admissionCounter();
      if (order === "legacy-first") {
        await counts.consume("old", old);
        await counts.consume("current", current, limit);
      } else {
        // Commit a current request before the old writer has created its document.
        await counts.consume("current", current);
        if (order === "current-first") {
          await counts.consume("old", old, limit);
        } else {
          await counts.consume("old", old, limit - 2);
          await Promise.all([counts.consume("current", current), counts.consume("old", old)]);
        }
      }
      counts.assertCap();
    });
  });
}

it("shards the full quota after draining shared writers and their window", testOptions, async () => {
  await withWriters(async (firestore, legacy) => {
    // Start a legacy window off the shard boundary so the first aligned
    // boundary after the last request is still inside that legacy window.
    let now = windowStart + 1;
    const old = legacy.createRavelryRateLimiter(firestore, () => now);
    const shared = createRavelryRateLimiter(firestore, () => now);
    const before = admissionCounter();
    await before.consume("old", old);
    await before.consume("current", shared, limit);
    before.assertCap();
    const sharedDocument = firestore.collection("ravelryRateLimits").doc("auth_global");
    const oldState = (await sharedDocument.get()).data();
    assert.equal(oldState?.count, limit);

    // No shared writer is called after this point. Enable shards at the
    // first aligned boundary at or after the legacy window has expired.
    now = windowStart + 2 * windowMillis;
    const sharded = createRavelryRateLimiter(
      firestore, () => now, () => 0, createRavelryRateLimitRuntimeState(), "sharded",
    );
    const after = admissionCounter();
    await after.consume("current", sharded, limit + 1);
    after.assertCap();
    assert.deepEqual((await sharedDocument.get()).data(), oldState);
    const documents = await sharedDocument.parent.get();
    assert.equal(
      documents.docs.filter((doc) => /^auth_global_\d+$/.test(doc.id))
        .reduce((count, doc) => count + doc.get("count"), 0),
      limit,
    );
  });
});
