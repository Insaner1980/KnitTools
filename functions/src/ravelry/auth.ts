import { createHash } from "node:crypto";

import { onCall, onRequest } from "firebase-functions/v2/https";

import {
  ravelryCallbackUrl,
  ravelryClientId,
  ravelryClientSecret,
  ravelrySecretOptions,
} from "../config";
import {
  RavelryAuthFlowError,
  completeRavelryOAuthCallback,
  disconnectRavelry,
  getRavelryAuthStatus,
  getRavelryCurrentUser,
  resolveBackendCallbackUrl,
  startRavelryOAuth,
} from "./authCore";
import { httpsErrorFor, requireUid } from "./callable";
import { createRavelryClient } from "./client";
import { refreshRavelryAccessToken } from "./oauthSecretRefresh";
import { exchangeOAuth2CodeForToken } from "./oauth2";
import { RavelryRateLimitError } from "./rateLimit";
import { createRavelryBackendStores } from "./stores";

function stores() {
  return createRavelryBackendStores();
}

function callbackRateLimitKey(ipAddress: string | undefined): string {
  return `callback_${createHash("sha256").update(ipAddress?.trim() || "unknown").digest("base64url")}`;
}

export const ravelryStartAuth = onCall(ravelrySecretOptions, async (request) => {
  try {
    const uid = requireUid(request.auth);
    const { rateLimiter, stateStore, tokenStore } = stores();
    await rateLimiter.consume(uid, "auth");
    return await startRavelryOAuth({
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
    const { tokenStore } = stores();
    return await getRavelryAuthStatus({
      uid,
      tokenStore,
    });
  } catch (error) {
    throw httpsErrorFor(error);
  }
});

export const ravelryDisconnect = onCall(async (request) => {
  try {
    const uid = requireUid(request.auth);
    const { stateStore, tokenStore } = stores();
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
  try {
    const { rateLimiter, stateStore, tokenStore } = stores();
    const result = await completeRavelryOAuthCallback({
      query: request.query,
      stateStore,
      tokenStore,
      rateLimiter,
      rateLimitKey: callbackRateLimitKey(request.ip),
      exchange: (exchangeRequest) =>
        exchangeOAuth2CodeForToken({
          ...exchangeRequest,
          clientId: ravelryClientId.value(),
          clientSecret: ravelryClientSecret.value(),
        }),
    });
    response.redirect(302, result.redirectUrl);
  } catch (error) {
    const handledError = error instanceof RavelryAuthFlowError || error instanceof RavelryRateLimitError;
    const status = handledError ? error.httpStatus : 500;
    const code = handledError ? error.code : "ravelry_callback_failed";
    response.status(status).json({ code });
  }
});
