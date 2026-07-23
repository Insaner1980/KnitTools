import type { DocumentReference, Firestore } from "firebase-admin/firestore";

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

export interface RavelryRateLimitRuntimeState {
  readonly completedLegacyMigrations: Set<RavelryRateLimitBucket>;
  readonly saturatedGlobalWindows: Map<RavelryRateLimitBucket, number>;
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

export function createRavelryRateLimitRuntimeState(): RavelryRateLimitRuntimeState {
  return {
    completedLegacyMigrations: new Set(),
    saturatedGlobalWindows: new Map(),
  };
}

const processRateLimitRuntimeState = createRavelryRateLimitRuntimeState();

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
  const current = activeRavelryRateLimitState(stored, nowMillis, rule);
  if (current != null) {
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
    uidRateLimitTarget(uid, bucket),
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

type RateLimitTransactionOutcome = "consumed" | "global-full" | "global-inactive";

interface ReferencedRateLimitTarget extends RavelryRateLimitTarget {
  readonly ref: DocumentReference;
}

export function createRavelryRateLimiter(
  firestore: Firestore,
  nowMillis: () => number = Date.now,
  random: () => number = Math.random,
  runtimeState: RavelryRateLimitRuntimeState = processRateLimitRuntimeState,
): RavelryRateLimiter {
  return {
    async consume(uid, bucket) {
      const collection = firestore.collection(RAVELRY_RATE_LIMITS_COLLECTION);
      const currentMillis = nowMillis();
      const globalRule = RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket];

      if (!runtimeState.completedLegacyMigrations.has(bucket)) {
        const legacyTargets = referencedTargets(
          collection,
          ravelryLegacyRateLimitTargets(uid, bucket),
        );
        const legacyOutcome = await consumeRateLimitTargets({
          firestore,
          targets: legacyTargets,
          uid,
          bucket,
          currentMillis,
          globalDecisionMillis: currentMillis,
          requireActiveGlobalWindow: true,
        });
        if (legacyOutcome === "consumed") {
          return;
        }
        if (legacyOutcome === "global-full") {
          throw globalRateLimitError(bucket);
        }
        runtimeState.completedLegacyMigrations.add(bucket);
      }

      const globalWindowStart = fixedWindowStartMillis(
        currentMillis,
        globalRule.windowMillis,
      );
      const saturatedWindowStart = runtimeState.saturatedGlobalWindows.get(bucket);
      if (saturatedWindowStart === globalWindowStart) {
        throw globalRateLimitError(bucket);
      }
      if (saturatedWindowStart != null) {
        runtimeState.saturatedGlobalWindows.delete(bucket);
      }

      const startShard = Math.min(
        Math.floor(Math.max(random(), 0) * RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT),
        RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT - 1,
      );

      for (const globalShard of ravelryGlobalShardOrder(startShard)) {
        const targets = referencedTargets(
          collection,
          ravelryRateLimitTargets(uid, bucket, globalShard),
        );
        const outcome = await consumeRateLimitTargets({
          firestore,
          targets,
          uid,
          bucket,
          currentMillis,
          globalDecisionMillis: globalWindowStart,
          requireActiveGlobalWindow: false,
        });
        if (outcome === "consumed") {
          return;
        }
      }

      runtimeState.saturatedGlobalWindows.set(bucket, globalWindowStart);
      throw globalRateLimitError(bucket);
    },
  };
}

async function consumeRateLimitTargets({
  firestore,
  targets,
  uid,
  bucket,
  currentMillis,
  globalDecisionMillis,
  requireActiveGlobalWindow,
}: {
  readonly firestore: Firestore;
  readonly targets: readonly ReferencedRateLimitTarget[];
  readonly uid: string;
  readonly bucket: RavelryRateLimitBucket;
  readonly currentMillis: number;
  readonly globalDecisionMillis: number;
  readonly requireActiveGlobalWindow: boolean;
}): Promise<RateLimitTransactionOutcome> {
  return firestore.runTransaction(async (transaction) => {
    const snapshots = await Promise.all(
      targets.map((target) => transaction.get(target.ref)),
    );
    const globalTarget = targets[1];
    const globalStored = snapshots[1].data();
    if (
      requireActiveGlobalWindow &&
      activeRavelryRateLimitState(globalStored, currentMillis, globalTarget.rule) == null
    ) {
      return "global-inactive";
    }

    const decisions = snapshots.map((snapshot, index) => {
      const target = targets[index];
      return {
        target,
        decision: nextRavelryRateLimitState(
          snapshot.data(),
          target.scope === "global" ? globalDecisionMillis : currentMillis,
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
      return "global-full";
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
    return "consumed";
  });
}

function referencedTargets(
  collection: ReturnType<Firestore["collection"]>,
  targets: readonly RavelryRateLimitTarget[],
): readonly ReferencedRateLimitTarget[] {
  return targets.map((target) => ({
    ...target,
    ref: collection.doc(target.documentId),
  }));
}

function ravelryLegacyRateLimitTargets(
  uid: string,
  bucket: RavelryRateLimitBucket,
): readonly RavelryRateLimitTarget[] {
  return [
    uidRateLimitTarget(uid, bucket),
    {
      scope: "global",
      documentId: legacyGlobalRateLimitDocumentId(bucket),
      rule: RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket],
    },
  ];
}

function uidRateLimitTarget(
  uid: string,
  bucket: RavelryRateLimitBucket,
): RavelryRateLimitTarget {
  return {
    scope: "uid",
    documentId: rateLimitDocumentId(uid, bucket),
    rule: RAVELRY_RATE_LIMIT_RULES[bucket],
  };
}

function globalRateLimitError(bucket: RavelryRateLimitBucket): RavelryRateLimitError {
  const rule = RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket];
  return new RavelryRateLimitError(bucket, "global", rule.limit, rule.windowMillis);
}

function activeRavelryRateLimitState(
  stored: unknown,
  nowMillis: number,
  rule: RavelryRateLimitRule,
): StoredRavelryRateLimit | null {
  const current = storedRavelryRateLimit(stored);
  return current != null &&
    nowMillis >= current.windowStartMillis &&
    nowMillis - current.windowStartMillis < rule.windowMillis
    ? current
    : null;
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

function legacyGlobalRateLimitDocumentId(bucket: RavelryRateLimitBucket): string {
  return `${bucket}_global`;
}
