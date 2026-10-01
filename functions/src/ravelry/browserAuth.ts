import { createHash, createHmac, randomBytes, timingSafeEqual } from "node:crypto";
import { setTimeout as delay } from "node:timers/promises";

import { KNITTOOLS_RAVELRY_AUTH_COMPLETE_DEEP_LINK } from "../config";
import { RavelryAuthFlowError, completeRavelryOAuthCallback, startRavelryOAuth } from "./authCore";
import type { BrowserOAuthStateStore, StoredOAuthState } from "./oauthStateStore";

export const BROWSER_AUTH_COOKIE = "__Host-knittools-ravelry-auth";

export function browserSecretHash(value: string): string {
  return createHash("sha256").update(value).digest("base64url");
}

export function requireBrowserValue(value: unknown): string {
  if (typeof value !== "string" || !/^[A-Za-z0-9_-]{43}$/.test(value)) {
    throw new RavelryAuthFlowError("invalid_browser_session", 400);
  }
  return value;
}

export function browserSecretFromCookie(header: string | undefined): string {
  const values = (header ?? "").split(";").map(part => part.trim())
    .filter(part => part.startsWith(`${BROWSER_AUTH_COOKIE}=`))
    .map(part => part.slice(BROWSER_AUTH_COOKIE.length + 1));
  return requireBrowserValue(values.length === 1 ? values[0] : undefined);
}

export async function startBrowserOAuth(options: Parameters<typeof startRavelryOAuth>[0]) {
  const ticket = randomBytes(32).toString("base64url");
  const result = await startRavelryOAuth({
    ...options,
    stateStore: {
      ...options.stateStore,
      saveState: state => options.stateStore.saveState({ ...state, browserStartHash: browserSecretHash(ticket) }),
    },
  });
  const url = new URL(options.backendCallbackUrl);
  url.searchParams.set("state", result.state);
  url.searchParams.set("start", ticket);
  return { ...result, authorizeUrl: url.toString() };
}

function requireBoundState(stored: StoredOAuthState | null, secret: string, now: number): StoredOAuthState {
  if (!stored || stored.expiresAtMillis <= now) throw new RavelryAuthFlowError("expired_state", 400);
  const expected = Buffer.from(stored.browserBindingHash ?? "");
  const actual = Buffer.from(browserSecretHash(secret));
  if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) {
    throw new RavelryAuthFlowError("invalid_browser_session", 400);
  }
  return stored;
}

function completionProof(secret: string, stored: StoredOAuthState): string {
  return createHmac("sha256", Buffer.from(secret, "base64url"))
    .update(`knittools:completion-proof:v1\0${stored.state}\0${stored.codeVerifier}`).digest("base64url");
}

export async function completeBrowserOAuthCallback(
  options: Parameters<typeof completeRavelryOAuthCallback>[0] & {
    stateStore: BrowserOAuthStateStore;
    browserSecret: string;
  },
) {
  const state = requireBrowserValue(options.query.state);
  const secret = requireBrowserValue(options.browserSecret);
  const now = options.nowMillis ?? Date.now;
  const stored = requireBoundState(await options.stateStore.getState(state), secret, now());

  async function savedResult() {
    const current = requireBoundState(await options.stateStore.getState(state), secret, now());
    if (await options.tokenStore.getConnectionGeneration(current.uid) !== (current.connectionGeneration ?? 0)) {
      throw new RavelryAuthFlowError("expired_state", 400);
    }
    if (!current.browserResult) return null;
    if (current.usedAtMillis == null) throw new RavelryAuthFlowError("invalid_completion", 400);
    const url = new URL(KNITTOOLS_RAVELRY_AUTH_COMPLETE_DEEP_LINK);
    url.searchParams.set("state", state);
    if (current.browserResult === "proof_ready") {
      url.searchParams.set("proof", completionProof(secret, current));
    } else if (["oauth_error", "state_expired", "access_denied", "cancelled", "canceled"].includes(current.browserResult)) {
      url.searchParams.set("error", current.browserResult);
    } else {
      throw new RavelryAuthFlowError("invalid_completion", 400);
    }
    return { redirectUrl: url.toString() };
  }

  async function awaitResult() {
    const deadline = performance.now() + 24_000;
    for (let count = 0; count < 32 && performance.now() < deadline; count++) {
      const result = await savedResult();
      if (result) return result;
      await delay(750);
    }
    return null;
  }

  if (stored.usedAtMillis != null || options.query.error != null) {
    await options.rateLimiter.consumeUid(stored.uid, "callback");
    const result = await awaitResult();
    if (result) return result;
    if (stored.usedAtMillis != null) throw new RavelryAuthFlowError("callback_in_progress", 409);
  }

  try {
    const result = await completeRavelryOAuthCallback({ ...options, randomString: () => completionProof(secret, stored) });
    const redirect = new URL(result.redirectUrl);
    const outcome = redirect.searchParams.has("proof") ? "proof_ready" : redirect.searchParams.get("error");
    if (!outcome) throw new RavelryAuthFlowError("invalid_completion", 400);
    const saved = await options.stateStore.saveBrowserResult(
      state, browserSecretHash(secret), outcome, now(),
    );
    if (!saved) throw new RavelryAuthFlowError("expired_state", 400);
    return result;
  } catch (error) {
    if (!(error instanceof RavelryAuthFlowError) || error.code !== "used_state") throw error;
    await options.rateLimiter.consumeUid(stored.uid, "callback");
    const result = await awaitResult();
    if (!result) throw new RavelryAuthFlowError("callback_in_progress", 409);
    return result;
  }
}
