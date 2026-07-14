import type { Firestore } from "firebase-admin/firestore";

import { RAVELRY_RATE_LIMITS_COLLECTION } from "../config";

export type RavelryRateLimitBucket = "auth" | "search" | "import";

export interface RavelryRateLimitRule {
  readonly limit: number;
  readonly windowMillis: number;
}

export interface StoredRavelryRateLimit {
  readonly windowStartMillis: number;
  readonly count: number;
}

export interface RavelryRateLimitDecision {
  readonly allowed: boolean;
  readonly state: StoredRavelryRateLimit;
}

export interface RavelryRateLimiter {
  consume(uid: string, bucket: RavelryRateLimitBucket): Promise<void>;
}

export const RAVELRY_RATE_LIMIT_RULES: Record<RavelryRateLimitBucket, RavelryRateLimitRule> = {
  auth: { limit: 10, windowMillis: 60_000 },
  search: { limit: 30, windowMillis: 60_000 },
  import: { limit: 20, windowMillis: 60_000 },
};

export const disabledRavelryRateLimiter: RavelryRateLimiter = {
  async consume() {
    return;
  },
};

export class RavelryRateLimitError extends Error {
  readonly code = "ravelry_rate_limited";
  readonly httpStatus = 429;

  constructor(
    readonly bucket: RavelryRateLimitBucket,
    readonly limit: number,
    readonly windowMillis: number,
  ) {
    super("ravelry_rate_limited");
  }
}

export function nextRavelryRateLimitState(
  stored: unknown,
  nowMillis: number,
  rule: RavelryRateLimitRule,
): RavelryRateLimitDecision {
  const current = storedRavelryRateLimit(stored);
  if (
    current != null &&
    nowMillis >= current.windowStartMillis &&
    nowMillis - current.windowStartMillis < rule.windowMillis
  ) {
    const state = {
      windowStartMillis: current.windowStartMillis,
      count: current.count + 1,
    };
    return {
      allowed: state.count <= rule.limit,
      state,
    };
  }

  const state = {
    windowStartMillis: nowMillis,
    count: 1,
  };
  return {
    allowed: true,
    state,
  };
}

export function createRavelryRateLimiter(
  firestore: Firestore,
  nowMillis: () => number = Date.now,
): RavelryRateLimiter {
  return {
    async consume(uid, bucket) {
      const rule = RAVELRY_RATE_LIMIT_RULES[bucket];
      const ref = firestore.collection(RAVELRY_RATE_LIMITS_COLLECTION).doc(rateLimitDocumentId(uid, bucket));
      await firestore.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(ref);
        const currentMillis = nowMillis();
        const decision = nextRavelryRateLimitState(snapshot.data(), currentMillis, rule);
        if (!decision.allowed) {
          throw new RavelryRateLimitError(bucket, rule.limit, rule.windowMillis);
        }

        transaction.set(ref, {
          uid,
          bucket,
          windowStartMillis: decision.state.windowStartMillis,
          count: decision.state.count,
          updatedAtMillis: currentMillis,
        });
      });
    },
  };
}

function storedRavelryRateLimit(stored: unknown): StoredRavelryRateLimit | null {
  if (typeof stored !== "object" || stored == null) {
    return null;
  }
  const value = stored as Record<string, unknown>;
  const windowStartMillis = numberField(value.windowStartMillis);
  const count = numberField(value.count);
  return windowStartMillis == null || count == null ? null : { windowStartMillis, count };
}

function numberField(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function rateLimitDocumentId(uid: string, bucket: RavelryRateLimitBucket): string {
  return `${bucket}_${Buffer.from(uid).toString("base64url")}`;
}
