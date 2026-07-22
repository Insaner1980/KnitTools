import assert from "node:assert/strict";
import { describe, it } from "node:test";
import type { Firestore } from "firebase-admin/firestore";

import {
  RAVELRY_GLOBAL_RATE_LIMIT_RULES,
  RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT,
  RAVELRY_RATE_LIMIT_RULES,
  RavelryRateLimitError,
  createRavelryRateLimiter,
  fixedWindowStartMillis,
  nextRavelryRateLimitState,
  ravelryGlobalShardOrder,
  ravelryRateLimitTargets,
} from "./rateLimit";

class FakeDocumentReference {
  constructor(readonly id: string) {}
}

class FakeTransaction {
  private readonly writes = new Map<string, Record<string, unknown>>();

  constructor(private readonly documents: Map<string, Record<string, unknown>>) {}

  async get(reference: FakeDocumentReference) {
    return {
      data: () => this.documents.get(reference.id),
    };
  }

  set(reference: FakeDocumentReference, value: Record<string, unknown>) {
    this.writes.set(reference.id, value);
  }

  commit() {
    for (const [id, value] of this.writes) {
      this.documents.set(id, value);
    }
  }
}

class FakeFirestore {
  readonly documents = new Map<string, Record<string, unknown>>();

  collection() {
    return {
      doc: (id: string) => new FakeDocumentReference(id),
    };
  }

  async runTransaction<T>(updateFunction: (transaction: FakeTransaction) => Promise<T>): Promise<T> {
    const transaction = new FakeTransaction(this.documents);
    const result = await updateFunction(transaction);
    transaction.commit();
    return result;
  }
}

describe("Ravelry callable rate limits", () => {
  it("allows requests up to the bucket limit and rejects the next request in the same window", () => {
    const rule = RAVELRY_RATE_LIMIT_RULES.search;
    let stored: unknown = null;

    for (let call = 1; call <= rule.limit; call += 1) {
      const decision = nextRavelryRateLimitState(stored, 1_000, rule);
      assert.equal(decision.allowed, true);
      assert.equal(decision.state.count, call);
      stored = decision.state;
    }

    const blocked = nextRavelryRateLimitState(stored, 1_000, rule);
    assert.equal(blocked.allowed, false);
    assert.equal(blocked.state.count, rule.limit + 1);
  });

  it("starts a new bucket window after the configured duration", () => {
    const rule = RAVELRY_RATE_LIMIT_RULES.import;
    const blocked = {
      windowStartMillis: 1_000,
      count: rule.limit + 1,
    };

    const decision = nextRavelryRateLimitState(blocked, 1_000 + rule.windowMillis, rule);

    assert.equal(decision.allowed, true);
    assert.deepEqual(decision.state, {
      windowStartMillis: 1_000 + rule.windowMillis,
      count: 1,
    });
  });

  it("maps exhausted buckets to a backend HTTP error", () => {
    const rule = RAVELRY_RATE_LIMIT_RULES.auth;
    const error = new RavelryRateLimitError("auth", "global", rule.limit, rule.windowMillis);

    assert.equal(error.code, "ravelry_rate_limited");
    assert.equal(error.httpStatus, 429);
  });

  it("splits each global limit exactly across independent shard targets", () => {
    const firstUidTargets = ravelryRateLimitTargets("first-uid", "search", 0);
    const secondUidTargets = ravelryRateLimitTargets("second-uid", "search", 1);

    assert.deepEqual(firstUidTargets.map((target) => target.scope), ["uid", "global"]);
    assert.notEqual(firstUidTargets[0].documentId, secondUidTargets[0].documentId);
    assert.notEqual(firstUidTargets[1].documentId, secondUidTargets[1].documentId);
    assert.deepEqual(firstUidTargets[0].rule, RAVELRY_RATE_LIMIT_RULES.search);
    for (const bucket of ["auth", "search", "import"] as const) {
      const globalTarget = ravelryRateLimitTargets("uid", bucket, 0)[1];
      assert.equal(
        globalTarget.rule.limit * RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT,
        RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket].limit,
      );
    }
  });

  it("visits every global shard once from the selected starting shard", () => {
    const order = ravelryGlobalShardOrder(7);

    assert.deepEqual(order, [7, 8, 9, 0, 1, 2, 3, 4, 5, 6]);
    assert.equal(new Set(order).size, RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT);
  });

  it("aligns global shard windows so their combined cap covers one fixed window", () => {
    assert.equal(fixedWindowStartMillis(61_234, 60_000), 60_000);
    assert.equal(fixedWindowStartMillis(119_999, 60_000), 60_000);
    assert.equal(fixedWindowStartMillis(120_000, 60_000), 120_000);
  });

  it("falls through a full global shard without consuming the uid bucket", async () => {
    const firestore = new FakeFirestore();
    const globalShardRule = ravelryRateLimitTargets("uid", "search", 0)[1].rule;
    firestore.documents.set("search_global_0", {
      windowStartMillis: 60_000,
      count: globalShardRule.limit,
    });
    const limiter = createRavelryRateLimiter(
      firestore as unknown as Firestore,
      () => 61_000,
      () => 0,
    );

    await limiter.consume("uid", "search");

    assert.equal(firestore.documents.get("search_global_0")?.count, globalShardRule.limit);
    assert.equal(firestore.documents.get("search_global_1")?.count, 1);
    assert.equal(firestore.documents.get("search_dWlk")?.count, 1);
  });

  it("reports the configured global limit only after every shard is full", async () => {
    const firestore = new FakeFirestore();
    const globalShardRule = ravelryRateLimitTargets("uid", "import", 0)[1].rule;
    for (let shard = 0; shard < RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT; shard += 1) {
      firestore.documents.set(`import_global_${shard}`, {
        windowStartMillis: 60_000,
        count: globalShardRule.limit,
      });
    }
    const limiter = createRavelryRateLimiter(
      firestore as unknown as Firestore,
      () => 61_000,
      () => 0,
    );

    await assert.rejects(
      limiter.consume("uid", "import"),
      (error: unknown) =>
        error instanceof RavelryRateLimitError &&
        error.scope === "global" &&
        error.limit === RAVELRY_GLOBAL_RATE_LIMIT_RULES.import.limit,
    );
  });
});
