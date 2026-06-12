import { getFirestore } from "firebase-admin/firestore";
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
import { createOAuthStateStore } from "./oauthStateStore";
import { exchangeOAuth2CodeForToken } from "./oauth2";
import { createTokenStore } from "./tokenStore";

const ravelrySecretOptions = {
  secrets: [ravelryClientId, ravelryClientSecret],
};

function stores() {
  const firestore = getFirestore();
  return {
    stateStore: createOAuthStateStore(firestore),
    tokenStore: createTokenStore(firestore),
  };
}

export const ravelryStartAuth = onCall(ravelrySecretOptions, async (request) => {
  try {
    const { stateStore } = stores();
    return await startRavelryOAuth({
      uid: requireUid(request.auth),
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
    const { tokenStore } = stores();
    return await getRavelryCurrentUser({
      uid: requireUid(request.auth),
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
