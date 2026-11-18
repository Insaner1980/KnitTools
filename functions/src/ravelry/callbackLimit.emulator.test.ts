import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { randomBytes } from "node:crypto";
import { after, describe, it, mock } from "node:test";
import { promisify } from "node:util";

import { Firestore } from "firebase-admin/firestore";

import { RAVELRY_RATE_LIMITS_COLLECTION } from "../config";
import { ravelryCallback } from "./auth";
import { completeRavelryOAuth, completeRavelryOAuthCallback, startRavelryOAuth } from "./authCore";
import { createOAuthStateStore } from "./oauthStateStore";
import {
  createRavelryRateLimiter, createRavelryRateLimitRuntimeState, RavelryRateLimitError,
} from "./rateLimit";
import * as stores from "./stores";
import { createTokenStore } from "./tokenStore";

const emulatorHost = process.env.FIRESTORE_EMULATOR_HOST;

describe("shared callback admission in Firestore", {
  skip: emulatorHost ? false : "Requires an explicitly started local Firestore emulator",
}, () => {
  const clients: Firestore[] = [];
  after(async () => { mock.restoreAll(); await Promise.all(clients.map((db) => db.terminate())); });

  function client() {
    assert.match(emulatorHost ?? "", /^(127\.0\.0\.1|localhost):\d+$/);
    const db = new Firestore({ projectId: "demo-knittools-ravelry-regression" });
    clients.push(db);
    return db;
  }

  async function clearLimits(db: Firestore) {
    const docs = await db.collection(RAVELRY_RATE_LIMITS_COLLECTION).get();
    await Promise.all(docs.docs.map((doc) => doc.ref.delete()));
  }

  it("enforces the same quota from two concurrent Node processes without shared memory", async () => {
    const db = client();
    await clearLimits(db);
    const worker = `
      const { Firestore } = require('firebase-admin/firestore');
      const { createRavelryRateLimiter, createRavelryRateLimitRuntimeState } = require('./rateLimit');
      const db = new Firestore({ projectId: 'demo-knittools-ravelry-regression' });
      (async () => {
        const limiter = createRavelryRateLimiter(db, () => 61000, Math.random, createRavelryRateLimitRuntimeState());
        const results = [];
        try {
          for (let i = 0; i < 36; i++) {
            try { await limiter.consumeGlobal('callback'); results.push(200); }
            catch (error) { if (error.httpStatus !== 429) throw error; results.push(429); }
          }
          process.stdout.write(JSON.stringify({ pid: process.pid, results }));
        } finally { await db.terminate(); }
      })().catch(() => { process.exitCode = 1; });
    `;
    try {
      const output = await Promise.all([0, 1].map(() => promisify(execFile)(process.execPath, ["-e", worker], {
        cwd: __dirname, timeout: 120_000, windowsHide: true,
      })));
      const workers = output.map(({ stdout }) => JSON.parse(stdout) as { pid: number; results: number[] });
      assert.notEqual(workers[0].pid, workers[1].pid);
      const statuses = workers.flatMap((worker) => worker.results);
      assert.equal(statuses.filter((status) => status === 200).length, 60);
      assert.equal(statuses.filter((status) => status === 429).length, 12);
    } finally { await clearLimits(db); }
  });

  it("shares 60 slots across independent clients and concurrent HTTP callbacks regardless of headers and state", async () => {
    const dbs = [client(), client()];
    await clearLimits(dbs[0]);
    let now = 61_000;
    let reads = 0;
    let sequence = 0;
    const limiters = dbs.map((db, i) => createRavelryRateLimiter(
      db, () => now, () => i / 2, createRavelryRateLimitRuntimeState(),
    ));
    mock.method(stores, "createRavelryBackendStores", () => ({
      rateLimiter: limiters[sequence++ % 2],
      stateStore: { async getState() { reads++; return null; } },
      tokenStore: {},
    }));
    async function invoke(i: number) {
      let status = 0;
      await ravelryCallback({
        method: "GET",
        query: { state: randomBytes(32).toString("base64url"), code: `code-${i}` },
        ip: `198.51.100.${i % 255}`,
        headers: { "x-forwarded-for": `${i}.fake, 2001:db8::${i}`,
          "x-real-ip": `192.0.2.${i % 255}`, forwarded: `for=client-${i}`,
          "user-agent": `agent-${i}`, "x-callback-key": `key-${i}` },
      } as unknown as Parameters<typeof ravelryCallback>[0], {
        set() { return this; },
        status(value: number) { status = value; return this; },
        json() { return this; },
      } as unknown as Parameters<typeof ravelryCallback>[1]);
      return status;
    }
    try {
      const statuses: number[] = [];
      for (let batch = 0; batch < 12; batch++) {
        statuses.push(...await Promise.all(Array.from({ length: 6 }, (_, i) => invoke(batch * 6 + i))));
      }
      assert.equal(statuses.filter((status) => status === 400).length, 60);
      assert.equal(statuses.filter((status) => status === 429).length, 12);
      assert.equal(reads, 120);
      const docs = await dbs[0].collection(RAVELRY_RATE_LIMITS_COLLECTION).get();
      assert.equal(docs.size, 10);
      assert.ok(docs.docs.every((doc) => /^callback_global_[0-9]$/.test(doc.id)));
      assert.equal(docs.docs.reduce((sum, doc) => sum + doc.data().count, 0), 60);
      const cold = createRavelryRateLimiter(dbs[1], () => now, () => 0, createRavelryRateLimitRuntimeState());
      await assert.rejects(cold.consumeGlobal("callback"), RavelryRateLimitError);
      for (const bucket of ["auth", "disconnect", "search", "import"] as const) {
        await cold.consumeGlobal(bucket);
      }
      now = 119_999;
      assert.equal(await invoke(73), 429);
      now = 120_000;
      assert.equal(await invoke(74), 400);
      assert.equal(reads, 122);
    } finally {
      mock.restoreAll();
      await clearLimits(dbs[0]);
    }
  });

  it("keeps valid login, owner UID limit, proof binding and replay protection with the real limiter", async () => {
    const db = client();
    await clearLimits(db);
    const now = 180_001;
    const stateStore = createOAuthStateStore(db);
    const tokenStore = createTokenStore(db);
    const uid = "callback-limit-owner";
    const rateLimiter = createRavelryRateLimiter(db, () => now, Math.random, createRavelryRateLimitRuntimeState());
    const createdStates: string[] = [];
    let exchanges = 0;
    try {
      for (let i = 0; i < 11; i++) {
        const started = await startRavelryOAuth({
          uid, stateStore, tokenStore, nowMillis: () => now,
          clientId: "synthetic-client", backendCallbackUrl: "https://callback.example",
        });
        createdStates.push(started.state);
        const completion = completeRavelryOAuthCallback({
          query: { state: started.state, code: "synthetic-code" },
          stateStore, tokenStore, rateLimiter, nowMillis: () => now,
          exchange: async () => { exchanges++; return { accessToken: "synthetic-token" }; },
        });
        if (i === 10) {
          await assert.rejects(completion, (error: unknown) =>
            error instanceof RavelryRateLimitError && error.scope === "uid");
          assert.equal((await stateStore.getState(started.state))?.usedAtMillis, null);
          continue;
        }
        const result = await completion;
        const proof = new URL(result.redirectUrl).searchParams.get("proof") ?? "";
        if (i === 0) {
          assert.equal(await tokenStore.getToken(uid), null);
          const options = { uid, state: started.state, completionProof: proof, tokenStore, nowMillis: () => now };
          await assert.rejects(completeRavelryOAuth({ ...options, uid: "wrong-owner" }), /invalid_completion/);
          await assert.rejects(completeRavelryOAuth({ ...options, completionProof: "X".repeat(43) }), /invalid_completion/);
          assert.deepEqual(await completeRavelryOAuth(options), { connected: true });
          await assert.rejects(completeRavelryOAuth(options), /invalid_completion/);
        }
      }
      assert.equal(exchanges, 10);
      const docs = await db.collection(RAVELRY_RATE_LIMITS_COLLECTION).get();
      const global = docs.docs.filter((doc) => /^callback_global_/.test(doc.id));
      assert.equal(global.reduce((sum, doc) => sum + doc.data().count, 0), 11);
      assert.equal(docs.docs.find((doc) => doc.data().scope === "uid")?.data().count, 10);
    } finally {
      await Promise.all(createdStates.map((state) => db.collection("ravelryOAuthStates").doc(state).delete()));
      await db.collection("ravelryTokens").doc(uid).delete();
      await clearLimits(db);
    }
  });
});
