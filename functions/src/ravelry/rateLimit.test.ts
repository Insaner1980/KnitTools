import assert from "node:assert/strict";
import { describe, it } from "node:test";

import {
  RAVELRY_RATE_LIMIT_RULES,
  RavelryRateLimitError,
  nextRavelryRateLimitState,
} from "./rateLimit";

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
    const error = new RavelryRateLimitError("auth", rule.limit, rule.windowMillis);

    assert.equal(error.code, "ravelry_rate_limited");
    assert.equal(error.httpStatus, 429);
  });
});
