import { onCall, onRequest } from "firebase-functions/v2/https";
import { randomBytes } from "node:crypto";

import {
  ravelryCallbackUrl,
  ravelryClientId,
  ravelryClientSecret,
  ravelrySecretOptions,
} from "../config";
import {
  RavelryAuthFlowError,
  completeRavelryOAuth,
  completeRavelryOAuthCallback,
  disconnectRavelry,
  getRavelryAuthStatus,
  getRavelryCurrentUser,
  ravelryAuthorizeUrl,
  resolveBackendCallbackUrl,
} from "./authCore";
import {
  BROWSER_AUTH_COOKIE,
  browserSecretFromCookie,
  browserSecretHash,
  completeBrowserOAuthCallback,
  requireBrowserValue,
  startBrowserOAuth,
} from "./browserAuth";
import { httpsErrorFor, requireUid } from "./callable";
import { createRavelryClient } from "./client";
import { refreshRavelryAccessToken } from "./oauthSecretRefresh";
import { exchangeOAuth2CodeForToken } from "./oauth2";
import { RavelryRateLimitError } from "./rateLimit";
import { createRavelryBackendStores } from "./stores";

function stores() {
  return createRavelryBackendStores();
}

function callableString(data: unknown, key: string): string {
  if (typeof data !== "object" || data == null) throw new RavelryAuthFlowError("invalid_completion", 400);
  const value = (data as Record<string, unknown>)[key];
  if (typeof value !== "string" || value.length === 0) {
    throw new RavelryAuthFlowError("invalid_completion", 400);
  }
  return value;
}

export const ravelryStartAuth = onCall(ravelrySecretOptions, async (request) => {
  try {
    const uid = requireUid(request.auth);
    const { rateLimiter, stateStore, tokenStore } = stores();
    await rateLimiter.consume(uid, "auth");
    return await startBrowserOAuth({
      uid,
      stateStore,
      tokenStore,
      clientId: ravelryClientId.value(),
      backendCallbackUrl: resolveBackendCallbackUrl(ravelryCallbackUrl.value()),
    });
  } catch (error) {
    throw httpsErrorFor(error);
  }
});

export const ravelryAuthStatus = onCall(async (request) => {
  try {
    const uid = requireUid(request.auth);
    const { rateLimiter, tokenStore } = stores();
    await rateLimiter.consumeUid(uid, "auth");
    return await getRavelryAuthStatus({
      uid,
      tokenStore,
    });
  } catch (error) {
    throw httpsErrorFor(error);
  }
});

export const ravelryCompleteAuth = onCall(async (request) => {
  try {
    const uid = requireUid(request.auth);
    const { rateLimiter, tokenStore } = stores();
    await rateLimiter.consumeUid(uid, "auth");
    return await completeRavelryOAuth({
      uid,
      state: callableString(request.data, "state"),
      completionProof: callableString(request.data, "proof"),
      tokenStore,
    });
  } catch (error) {
    throw httpsErrorFor(error);
  }
});

export const ravelryDisconnect = onCall(async (request) => {
  try {
    const uid = requireUid(request.auth);
    const { rateLimiter, stateStore, tokenStore } = stores();
    await rateLimiter.consumeUid(uid, "disconnect");
    return await disconnectRavelry({
      uid,
      tokenStore,
      stateStore,
    });
  } catch (error) {
    throw httpsErrorFor(error);
  }
});

export const ravelryCurrentUser = onCall(ravelrySecretOptions, async (request) => {
  try {
    const uid = requireUid(request.auth);
    const { rateLimiter, tokenStore } = stores();
    await rateLimiter.consume(uid, "auth");
    return await getRavelryCurrentUser({
      uid,
      tokenStore,
      client: createRavelryClient(),
      refresh: refreshRavelryAccessToken,
    });
  } catch (error) {
    throw httpsErrorFor(error);
  }
});

export const ravelryCallback = onRequest(ravelrySecretOptions, async (request, response) => {
  response.set("Cache-Control", "no-store");
  response.set("Referrer-Policy", "no-referrer");
  try {
    const { rateLimiter, stateStore, tokenStore } = stores();
    if (request.method !== "GET" && request.method !== "POST") {
      response.set("Allow", "GET, POST");
      response.status(405).json({ code: "method_not_allowed" });
      return;
    }
    await rateLimiter.consumeGlobal("callback");
    const query = request.method === "POST"
      ? (request.body && typeof request.body === "object" ? request.body : {}) as Record<string, unknown>
      : request.query;
    if (query.start != null) {
      if (request.method !== "GET") throw new RavelryAuthFlowError("invalid_browser_session", 400);
      const state = requireBrowserValue(query.state);
      const ticket = requireBrowserValue(query.start);
      const secret = randomBytes(32).toString("base64url");
      const stored = await stateStore.bindBrowser(state, browserSecretHash(ticket), browserSecretHash(secret), Date.now());
      if (!stored || await tokenStore.getConnectionGeneration(stored.uid) !== (stored.connectionGeneration ?? 0)) {
        throw new RavelryAuthFlowError("invalid_browser_session", 400);
      }
      await rateLimiter.consumeUid(stored.uid, "callback");
      response.set("Set-Cookie", `${BROWSER_AUTH_COOKIE}=${secret}; Max-Age=600; Path=/; Secure; HttpOnly; SameSite=Lax`);
      response.redirect(302, ravelryAuthorizeUrl(ravelryClientId.value(), stored.redirectUri, state, stored.codeChallenge));
      return;
    }
    if (query.state != null && typeof query.state !== "string") {
      throw new RavelryAuthFlowError("invalid_state", 400);
    }
    const state = typeof query.state === "string" && /^[A-Za-z0-9_-]{43}$/.test(query.state)
      ? await stateStore.getState(query.state) : null;
    const options = {
      query,
      stateStore,
      tokenStore,
      rateLimiter: { ...rateLimiter, consumeGlobal: async () => {} },
      exchange: (exchangeRequest: Parameters<Parameters<typeof completeRavelryOAuthCallback>[0]["exchange"]>[0]) =>
        exchangeOAuth2CodeForToken({
          ...exchangeRequest,
          clientId: ravelryClientId.value(),
          clientSecret: ravelryClientSecret.value(),
        }),
    };
    const result = state?.browserBindingHash || state?.browserStartHash
      ? await completeBrowserOAuthCallback({ ...options, browserSecret: browserSecretFromCookie(request.headers.cookie) })
      : await completeRavelryOAuthCallback(options);
    response.redirect(302, result.redirectUrl);
  } catch (error) {
    const handledError = error instanceof RavelryAuthFlowError || error instanceof RavelryRateLimitError;
    const status = handledError ? error.httpStatus : 500;
    const code = handledError ? error.code : "ravelry_callback_failed";
    response.status(status).json({ code });
  }
});
