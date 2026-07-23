import type { Firestore } from "firebase-admin/firestore";

import { RAVELRY_RATE_LIMITS_COLLECTION } from "../config";

export type RavelryRateLimitBucket = "auth" | "search" | "import";
export type RavelryRateLimitScope = "uid" | "global";

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

export interface RavelryRateLimitTarget {
  readonly scope: RavelryRateLimitScope;
  readonly documentId: string;
  readonly rule: RavelryRateLimitRule;
}

export interface RavelryRateLimiter {
  consume(uid: string, bucket: RavelryRateLimitBucket): Promise<void>;
}

export const RAVELRY_RATE_LIMIT_RULES: Record<RavelryRateLimitBucket, RavelryRateLimitRule> = {
  auth: { limit: 10, windowMillis: 60_000 },
  search: { limit: 30, windowMillis: 60_000 },
  import: { limit: 20, windowMillis: 60_000 },
};

export const RAVELRY_GLOBAL_RATE_LIMIT_RULES: Record<RavelryRateLimitBucket, RavelryRateLimitRule> = {
  auth: { limit: 60, windowMillis: 60_000 },
  search: { limit: 120, windowMillis: 60_000 },
  import: { limit: 80, windowMillis: 60_000 },
};

export const RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT = 10;

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
    readonly scope: RavelryRateLimitScope,
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

export function ravelryRateLimitTargets(
  uid: string,
  bucket: RavelryRateLimitBucket,
  globalShard: number,
): readonly RavelryRateLimitTarget[] {
  const globalRule = RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket];
  const globalShardLimit = globalRule.limit / RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT;
  if (!Number.isInteger(globalShardLimit)) {
    throw new Error("ravelry_global_rate_limit_must_divide_evenly_across_shards");
  }
  return [
    {
      scope: "uid",
      documentId: rateLimitDocumentId(uid, bucket),
      rule: RAVELRY_RATE_LIMIT_RULES[bucket],
    },
    {
      scope: "global",
      documentId: globalRateLimitDocumentId(bucket, globalShard),
      rule: {
        limit: globalShardLimit,
        windowMillis: globalRule.windowMillis,
      },
    },
  ];
}

export function ravelryGlobalShardOrder(startShard: number): readonly number[] {
  return Array.from(
    { length: RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT },
    (_, offset) => (startShard + offset) % RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT,
  );
}

export function fixedWindowStartMillis(nowMillis: number, windowMillis: number): number {
  return Math.floor(nowMillis / windowMillis) * windowMillis;
}

class RavelryGlobalShardFullError extends Error {
  constructor() {
    super("ravelry_global_rate_limit_shard_full");
  }
}

export function createRavelryRateLimiter(
  firestore: Firestore,
  nowMillis: () => number = Date.now,
  random: () => number = Math.random,
): RavelryRateLimiter {
  return {
    async consume(uid, bucket) {
      const collection = firestore.collection(RAVELRY_RATE_LIMITS_COLLECTION);
      const currentMillis = nowMillis();
      const startShard = Math.min(
        Math.floor(Math.max(random(), 0) * RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT),
        RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT - 1,
      );

      for (const globalShard of ravelryGlobalShardOrder(startShard)) {
        const targets = ravelryRateLimitTargets(uid, bucket, globalShard).map((target) => ({
          ...target,
          ref: collection.doc(target.documentId),
        }));
        try {
          await firestore.runTransaction(async (transaction) => {
            const snapshots = await Promise.all(
              targets.map((target) => transaction.get(target.ref)),
            );
            const decisions = snapshots.map((snapshot, index) => {
              const target = targets[index];
              const decisionMillis = target.scope === "global"
                ? fixedWindowStartMillis(currentMillis, target.rule.windowMillis)
                : currentMillis;
              return {
                target,
                decision: nextRavelryRateLimitState(
                  snapshot.data(),
                  decisionMillis,
                  target.rule,
                ),
              };
            });
            const uidDecision = decisions[0];
            const globalDecision = decisions[1];
            if (!uidDecision.decision.allowed) {
              throw new RavelryRateLimitError(
                bucket,
                "uid",
                uidDecision.target.rule.limit,
                uidDecision.target.rule.windowMillis,
              );
            }
            if (!globalDecision.decision.allowed) {
              throw new RavelryGlobalShardFullError();
            }

            for (const { target, decision } of decisions) {
              transaction.set(target.ref, {
                ...(target.scope === "uid" ? { uid } : {}),
                bucket,
                scope: target.scope,
                windowStartMillis: decision.state.windowStartMillis,
                count: decision.state.count,
                updatedAtMillis: currentMillis,
              });
            }
          });
          return;
        } catch (error) {
          if (!(error instanceof RavelryGlobalShardFullError)) {
            throw error;
          }
        }
      }

      const globalRule = RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket];
      throw new RavelryRateLimitError(
        bucket,
        "global",
        globalRule.limit,
        globalRule.windowMillis,
      );
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

function globalRateLimitDocumentId(bucket: RavelryRateLimitBucket, shard: number): string {
  return `${bucket}_global_${shard}`;
}
