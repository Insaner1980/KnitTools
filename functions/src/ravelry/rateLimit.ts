import type { DocumentReference, Firestore } from "firebase-admin/firestore";

import { RAVELRY_RATE_LIMITS_COLLECTION } from "../config";

export type RavelryRateLimitBucket = "auth" | "callback" | "disconnect" | "search" | "import";
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
  consumeUid(uid: string, bucket: RavelryRateLimitBucket): Promise<void>;
  consumeGlobal(bucket: RavelryRateLimitBucket): Promise<void>;
}

export interface RavelryRateLimitRuntimeState {
  readonly saturatedGlobalWindows: Map<RavelryRateLimitBucket, number>;
}

export const RAVELRY_RATE_LIMIT_RULES: Record<RavelryRateLimitBucket, RavelryRateLimitRule> = {
  auth: { limit: 10, windowMillis: 60_000 },
  callback: { limit: 10, windowMillis: 60_000 },
  disconnect: { limit: 10, windowMillis: 60_000 },
  search: { limit: 30, windowMillis: 60_000 },
  import: { limit: 20, windowMillis: 60_000 },
};

export const RAVELRY_GLOBAL_RATE_LIMIT_RULES: Record<RavelryRateLimitBucket, RavelryRateLimitRule> = {
  auth: { limit: 60, windowMillis: 60_000 },
  callback: { limit: 60, windowMillis: 60_000 },
  disconnect: { limit: 60, windowMillis: 60_000 },
  search: { limit: 120, windowMillis: 60_000 },
  import: { limit: 80, windowMillis: 60_000 },
};

export const RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT = 10;

export function createRavelryRateLimitRuntimeState(): RavelryRateLimitRuntimeState {
  return {
    saturatedGlobalWindows: new Map(),
  };
}

const processRateLimitRuntimeState = createRavelryRateLimitRuntimeState();

export const disabledRavelryRateLimiter: RavelryRateLimiter = {
  async consume() {
    return;
  },
  async consumeUid() {
    return;
  },
  async consumeGlobal() {
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
  return [
    uidRateLimitTarget(uid, bucket),
    globalRateLimitTarget(bucket, globalShard),
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

interface ReferencedRateLimitTargets {
  readonly uid: ReferencedRateLimitTarget;
  readonly global: ReferencedRateLimitTarget;
}

export function createRavelryRateLimiter(
  firestore: Firestore,
  nowMillis: () => number = Date.now,
  random: () => number = Math.random,
  runtimeState: RavelryRateLimitRuntimeState = processRateLimitRuntimeState,
): RavelryRateLimiter {
  const consumeGlobalCapacity = async (
    bucket: RavelryRateLimitBucket,
    uid?: string,
  ): Promise<void> => {
    const collection = firestore.collection(RAVELRY_RATE_LIMITS_COLLECTION);
    const currentMillis = nowMillis();
    const globalRule = RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket];
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

    const consumeTarget = async (
      globalTarget: RavelryRateLimitTarget,
      globalDecisionMillis: number,
      requireActiveGlobalWindow: boolean,
    ): Promise<RateLimitTransactionOutcome> => {
      if (uid == null) {
        return consumeGlobalRateLimitTarget({
          firestore,
          target: referencedRateLimitTarget(collection, globalTarget),
          bucket,
          currentMillis,
          globalDecisionMillis,
          requireActiveGlobalWindow,
        });
      }
      return consumeRateLimitTargets({
        firestore,
        targets: referencedTargets(collection, [uidRateLimitTarget(uid, bucket), globalTarget]),
        uid,
        bucket,
        currentMillis,
        globalDecisionMillis,
        requireActiveGlobalWindow,
      });
    };

    // Väliaikainen rollout-suoja poistetaan vasta, kun legacy-dokumentteja
    // kirjoittavat revisiot on vahvistettu poistuneiksi liikenteestä.
    const legacyOutcome = await consumeTarget(
      legacyGlobalRateLimitTarget(bucket),
      currentMillis,
      true,
    );
    if (legacyOutcome === "consumed") {
      return;
    }
    if (legacyOutcome === "global-full") {
      throw globalRateLimitError(bucket);
    }

    const startShard = Math.min(
      Math.floor(Math.max(random(), 0) * RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT),
      RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT - 1,
    );

    for (const globalShard of ravelryGlobalShardOrder(startShard)) {
      const outcome = await consumeTarget(
        globalRateLimitTarget(bucket, globalShard),
        globalWindowStart,
        false,
      );
      if (outcome === "consumed") {
        return;
      }
    }

    runtimeState.saturatedGlobalWindows.set(bucket, globalWindowStart);
    throw globalRateLimitError(bucket);
  };

  return {
    async consumeUid(uid, bucket) {
      const collection = firestore.collection(RAVELRY_RATE_LIMITS_COLLECTION);
      const currentMillis = nowMillis();
      const target = uidRateLimitTarget(uid, bucket);
      const ref = collection.doc(target.documentId);
      await firestore.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(ref);
        const decision = nextRavelryRateLimitState(snapshot.data(), currentMillis, target.rule);
        if (!decision.allowed) {
          throw new RavelryRateLimitError(bucket, "uid", target.rule.limit, target.rule.windowMillis);
        }
        transaction.set(ref, {
          uid,
          bucket,
          scope: "uid",
          windowStartMillis: decision.state.windowStartMillis,
          count: decision.state.count,
          updatedAtMillis: currentMillis,
        });
      });
    },
    async consume(uid, bucket) {
      await consumeGlobalCapacity(bucket, uid);
    },
    async consumeGlobal(bucket) {
      await consumeGlobalCapacity(bucket);
    },
  };
}

async function consumeGlobalRateLimitTarget({
  firestore,
  target,
  bucket,
  currentMillis,
  globalDecisionMillis,
  requireActiveGlobalWindow,
}: {
  readonly firestore: Firestore;
  readonly target: ReferencedRateLimitTarget;
  readonly bucket: RavelryRateLimitBucket;
  readonly currentMillis: number;
  readonly globalDecisionMillis: number;
  readonly requireActiveGlobalWindow: boolean;
}): Promise<RateLimitTransactionOutcome> {
  return firestore.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(target.ref);
    const stored = snapshot.data();
    if (
      requireActiveGlobalWindow &&
      activeRavelryRateLimitState(stored, currentMillis, target.rule) == null
    ) {
      return "global-inactive";
    }

    const decision = nextRavelryRateLimitState(stored, globalDecisionMillis, target.rule);
    if (!decision.allowed) {
      return "global-full";
    }
    transaction.set(target.ref, {
      bucket,
      scope: "global",
      windowStartMillis: decision.state.windowStartMillis,
      count: decision.state.count,
      updatedAtMillis: currentMillis,
    });
    return "consumed";
  });
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
  readonly targets: ReferencedRateLimitTargets;
  readonly uid: string;
  readonly bucket: RavelryRateLimitBucket;
  readonly currentMillis: number;
  readonly globalDecisionMillis: number;
  readonly requireActiveGlobalWindow: boolean;
}): Promise<RateLimitTransactionOutcome> {
  return firestore.runTransaction(async (transaction) => {
    const [uidSnapshot, globalSnapshot] = await transaction.getAll(
      targets.uid.ref,
      targets.global.ref,
    );
    const globalStored = globalSnapshot.data();
    if (
      requireActiveGlobalWindow &&
      activeRavelryRateLimitState(globalStored, currentMillis, targets.global.rule) == null
    ) {
      return "global-inactive";
    }

    const uidDecision = {
      target: targets.uid,
      decision: nextRavelryRateLimitState(
        uidSnapshot.data(),
        currentMillis,
        targets.uid.rule,
      ),
    };
    const globalDecision = {
      target: targets.global,
      decision: nextRavelryRateLimitState(
        globalStored,
        globalDecisionMillis,
        targets.global.rule,
      ),
    };
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

    for (const { target, decision } of [uidDecision, globalDecision]) {
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
): ReferencedRateLimitTargets {
  const referencedTarget = (scope: RavelryRateLimitScope): ReferencedRateLimitTarget => {
    const matchingTargets = targets.filter((target) => target.scope === scope);
    if (matchingTargets.length !== 1) {
      throw new Error(`ravelry_rate_limit_requires_one_${scope}_target`);
    }
    return referencedRateLimitTarget(collection, matchingTargets[0]);
  };
  return {
    uid: referencedTarget("uid"),
    global: referencedTarget("global"),
  };
}

function referencedRateLimitTarget(
  collection: ReturnType<Firestore["collection"]>,
  target: RavelryRateLimitTarget,
): ReferencedRateLimitTarget {
  return {
    ...target,
    ref: collection.doc(target.documentId),
  };
}

function legacyGlobalRateLimitTarget(
  bucket: RavelryRateLimitBucket,
): RavelryRateLimitTarget {
  return {
    scope: "global",
    documentId: legacyGlobalRateLimitDocumentId(bucket),
    rule: RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket],
  };
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

function globalRateLimitTarget(
  bucket: RavelryRateLimitBucket,
  globalShard: number,
): RavelryRateLimitTarget {
  const globalRule = RAVELRY_GLOBAL_RATE_LIMIT_RULES[bucket];
  const globalShardLimit = globalRule.limit / RAVELRY_GLOBAL_RATE_LIMIT_SHARD_COUNT;
  if (!Number.isInteger(globalShardLimit)) {
    throw new Error("ravelry_global_rate_limit_must_divide_evenly_across_shards");
  }
  return {
    scope: "global",
    documentId: globalRateLimitDocumentId(bucket, globalShard),
    rule: {
      limit: globalShardLimit,
      windowMillis: globalRule.windowMillis,
    },
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
