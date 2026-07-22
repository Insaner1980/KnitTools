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
): readonly RavelryRateLimitTarget[] {
  return [
    {
      scope: "uid",
      documentId: rateLimitDocumentId(uid, bucket),
      rule: RAVELRY_RATE_LIMIT_RULES[bucket],
    },
    {
      scope: "global",
      documentId: globalRateLimitDocumentId(bucket),
      rule: RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket],
    },
  ];
}

export function createRavelryRateLimiter(
  firestore: Firestore,
  nowMillis: () => number = Date.now,
): RavelryRateLimiter {
  return {
    async consume(uid, bucket) {
      const collection = firestore.collection(RAVELRY_RATE_LIMITS_COLLECTION);
      const targets = ravelryRateLimitTargets(uid, bucket).map((target) => ({
        ...target,
        ref: collection.doc(target.documentId),
      }));
      await firestore.runTransaction(async (transaction) => {
        const snapshots = await Promise.all(targets.map((target) => transaction.get(target.ref)));
        const currentMillis = nowMillis();
        const decisions = snapshots.map((snapshot, index) => ({
          target: targets[index],
          decision: nextRavelryRateLimitState(
            snapshot.data(),
            currentMillis,
            targets[index].rule,
          ),
        }));
        const rejected = decisions.find(({ decision }) => !decision.allowed);
        if (rejected) {
          throw new RavelryRateLimitError(
            bucket,
            rejected.target.scope,
            rejected.target.rule.limit,
            rejected.target.rule.windowMillis,
          );
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

function globalRateLimitDocumentId(bucket: RavelryRateLimitBucket): string {
  return `${bucket}_global`;
}
