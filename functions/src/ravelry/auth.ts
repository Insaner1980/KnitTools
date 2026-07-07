import { onCall, onRequest } from "firebase-functions/v2/https";

import { ravelryCallbackUrl, ravelryClientId, ravelryClientSecret } from "../config";
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
import { exchangeOAuth2CodeForToken } from "./oauth2";
import { createRavelryBackendStores } from "./stores";

const ravelrySecretOptions = {
  secrets: [ravelryClientId, ravelryClientSecret],
};

function stores() {
  return createRavelryBackendStores();
}

export const ravelryStartAuth = onCall(ravelrySecretOptions, async (request) => {
  try {
    const { rateLimiter, stateStore } = stores();
    const uid = requireUid(request.auth);
    await rateLimiter.consume(uid, "auth");
    return await startRavelryOAuth({
      uid,
      stateStore,
      clientId: ravelryClientId.value(),
      backendCallbackUrl: resolveBackendCallbackUrl(ravelryCallbackUrl.value()),
    });
  } catch (error) {
    throw httpsErrorFor(error);
  }
});

export const ravelryAuthStatus = onCall(async (request) => {
  try {
    const { tokenStore } = stores();
    return await getRavelryAuthStatus({
      uid: requireUid(request.auth),
      tokenStore,
    });
  } catch (error) {
    throw httpsErrorFor(error);
  }
});

export const ravelryDisconnect = onCall(async (request) => {
  try {
    const { stateStore, tokenStore } = stores();
    return await disconnectRavelry({
      uid: requireUid(request.auth),
      tokenStore,
      stateStore,
    });
  } catch (error) {
    throw httpsErrorFor(error);
  }
});

export const ravelryCurrentUser = onCall(async (request) => {
  try {
    const { rateLimiter, tokenStore } = stores();
    const uid = requireUid(request.auth);
    await rateLimiter.consume(uid, "auth");
    return await getRavelryCurrentUser({
      uid,
      tokenStore,
      client: createRavelryClient(),
    });
  } catch (error) {
    throw httpsErrorFor(error);
  }
});

export const ravelryCallback = onRequest(ravelrySecretOptions, async (request, response) => {
  try {
    const { stateStore, tokenStore } = stores();
    const result = await completeRavelryOAuthCallback({
      query: request.query,
      stateStore,
      tokenStore,
      exchange: (exchangeRequest) =>
        exchangeOAuth2CodeForToken({
          ...exchangeRequest,
          clientId: ravelryClientId.value(),
          clientSecret: ravelryClientSecret.value(),
        }),
    });
    response.redirect(302, result.redirectUrl);
  } catch (error) {
    const status = error instanceof RavelryAuthFlowError ? error.httpStatus : 500;
    const code = error instanceof RavelryAuthFlowError ? error.code : "ravelry_callback_failed";
    response.status(status).json({ code });
  }
});
