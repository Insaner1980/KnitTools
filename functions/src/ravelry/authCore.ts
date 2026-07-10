import { createHash, randomBytes } from "node:crypto";

import {
  KNITTOOLS_RAVELRY_AUTH_COMPLETE_DEEP_LINK,
  RAVELRY_OAUTH_STATE_TTL_MILLIS,
  firebaseRegion,
} from "../config";
import type { RavelryCurrentUserClient } from "./client";
import type { OAuthStateStore } from "./oauthStateStore";
import type { OAuthTokenExchange, OAuthTokenRefresh } from "./oauth2";
import { RAVELRY_AUTHORIZATION_URL } from "./oauth2";
import { getUsableRavelryToken } from "./tokenAccess";
import type { RavelryTokenStore } from "./tokenStore";

type RandomLabel = "state" | "code-verifier";

interface StartOAuthOptions {
  readonly uid: string;
  readonly stateStore: OAuthStateStore;
  readonly clientId: string;
  readonly backendCallbackUrl: string;
  readonly nowMillis?: () => number;
  readonly randomString?: (label: RandomLabel) => string;
}

interface CompleteCallbackOptions {
  readonly query: Record<string, unknown>;
  readonly stateStore: OAuthStateStore;
  readonly tokenStore: RavelryTokenStore;
  readonly exchange: OAuthTokenExchange;
  readonly nowMillis?: () => number;
}

interface UserTokenOptions {
  readonly uid: string;
  readonly tokenStore: RavelryTokenStore;
}

interface CurrentUserOptions extends UserTokenOptions {
  readonly client: RavelryCurrentUserClient;
  readonly refresh?: OAuthTokenRefresh;
  readonly nowMillis?: () => number;
}

interface DisconnectOptions extends UserTokenOptions {
  readonly stateStore: OAuthStateStore;
  readonly nowMillis?: () => number;
}

export interface StartOAuthResponse {
  readonly authorizeUrl: string;
  readonly state: string;
  readonly expiresAtMillis: number;
}

export interface CallbackResponse {
  readonly redirectUrl: string;
}

export interface AuthStatusResponse {
  readonly connected: boolean;
  readonly username?: string;
  readonly lastVerifiedAtMillis?: number;
}

export interface CurrentUserResponse {
  readonly connected: boolean;
  readonly ravelryUserId?: string;
  readonly ravelryUsername?: string;
}

export class RavelryAuthFlowError extends Error {
  constructor(
    readonly code: string,
    readonly httpStatus: number,
  ) {
    super(code);
  }
}

function randomBase64Url(_label: RandomLabel): string {
  return randomBytes(32).toString("base64url");
}

function codeChallengeFor(verifier: string): string {
  return createHash("sha256").update(verifier).digest("base64url");
}

function appRedirectUrl(state: string, error?: string): string {
  const url = new URL(KNITTOOLS_RAVELRY_AUTH_COMPLETE_DEEP_LINK);
  url.searchParams.set("state", state);
  if (error) {
    url.searchParams.set("error", error);
  }
  return url.toString();
}

function queryString(query: Record<string, unknown>, key: string): string | undefined {
  const value = query[key];
  if (Array.isArray(value)) {
    return typeof value[0] === "string" ? value[0] : undefined;
  }
  return typeof value === "string" && value.length > 0 ? value : undefined;
}

function requireState(value: string | undefined): string {
  if (!value) {
    throw new RavelryAuthFlowError("missing_state", 400);
  }
  return value;
}

function redirectExpiredStateOrThrow(error: unknown, state: string): CallbackResponse {
  if (error instanceof RavelryAuthFlowError && error.code === "expired_state") {
    return { redirectUrl: appRedirectUrl(state, "state_expired") };
  }
  throw error;
}

function tokenForStorage(
  uid: string,
  token: Awaited<ReturnType<OAuthTokenExchange>>,
  nowMillis: number,
) {
  return {
    uid,
    authType: "oauth2" as const,
    accessToken: token.accessToken,
    ...(token.refreshToken ? { refreshToken: token.refreshToken } : {}),
    ...(token.expiresAtMillis != null ? { expiresAtMillis: token.expiresAtMillis } : {}),
    createdAtMillis: nowMillis,
    updatedAtMillis: nowMillis,
  };
}

async function loadUsableState(
  stateStore: OAuthStateStore,
  state: string,
  nowMillis: number,
) {
  const storedState = await stateStore.getState(state);
  if (!storedState) {
    throw new RavelryAuthFlowError("invalid_state", 400);
  }
  if (storedState.usedAtMillis != null) {
    throw new RavelryAuthFlowError("used_state", 400);
  }
  if (storedState.expiresAtMillis <= nowMillis) {
    throw new RavelryAuthFlowError("expired_state", 400);
  }
  return storedState;
}

async function markStateUsedOrReject(
  stateStore: OAuthStateStore,
  state: string,
  nowMillis: number,
) {
  const marked = await stateStore.markStateUsed(state, nowMillis);
  if (!marked) {
    const storedState = await stateStore.getState(state);
    if (!storedState) {
      throw new RavelryAuthFlowError("invalid_state", 400);
    }
    if (storedState.usedAtMillis != null) {
      throw new RavelryAuthFlowError("used_state", 400);
    }
    if (storedState.expiresAtMillis <= nowMillis) {
      throw new RavelryAuthFlowError("expired_state", 400);
    }
    throw new RavelryAuthFlowError("used_state", 400);
  }
}

export function resolveBackendCallbackUrl(
  configuredCallbackUrl: string,
  env: NodeJS.ProcessEnv = process.env,
): string {
  const trimmedCallbackUrl = configuredCallbackUrl.trim();
  if (trimmedCallbackUrl.length > 0) {
    return trimmedCallbackUrl;
  }

  const firebaseConfigProjectId =
    typeof env.FIREBASE_CONFIG === "string"
      ? (JSON.parse(env.FIREBASE_CONFIG) as { projectId?: unknown }).projectId
      : undefined;
  const projectId =
    env.GCLOUD_PROJECT ??
    env.GCP_PROJECT ??
    (typeof firebaseConfigProjectId === "string" ? firebaseConfigProjectId : undefined);

  if (!projectId) {
    throw new RavelryAuthFlowError("missing_callback_url", 500);
  }

  return `https://${firebaseRegion}-${projectId}.cloudfunctions.net/ravelryCallback`;
}

export async function startRavelryOAuth({
  uid,
  stateStore,
  clientId,
  backendCallbackUrl,
  nowMillis = Date.now,
  randomString = randomBase64Url,
}: StartOAuthOptions): Promise<StartOAuthResponse> {
  const createdAtMillis = nowMillis();
  const expiresAtMillis = createdAtMillis + RAVELRY_OAUTH_STATE_TTL_MILLIS;
  const state = randomString("state");
  const codeVerifier = randomString("code-verifier");
  const codeChallenge = codeChallengeFor(codeVerifier);

  await stateStore.saveState({
    state,
    uid,
    authType: "oauth2",
    createdAtMillis,
    expiresAtMillis,
    usedAtMillis: null,
    redirectUri: backendCallbackUrl,
    codeVerifier,
    codeChallenge,
    codeChallengeMethod: "S256",
  });

  const authorizeUrl = new URL(RAVELRY_AUTHORIZATION_URL);
  authorizeUrl.searchParams.set("response_type", "code");
  authorizeUrl.searchParams.set("client_id", clientId);
  authorizeUrl.searchParams.set("redirect_uri", backendCallbackUrl);
  authorizeUrl.searchParams.set("scope", "offline");
  authorizeUrl.searchParams.set("state", state);
  authorizeUrl.searchParams.set("code_challenge", codeChallenge);
  authorizeUrl.searchParams.set("code_challenge_method", "S256");

  return {
    authorizeUrl: authorizeUrl.toString(),
    state,
    expiresAtMillis,
  };
}

export async function completeRavelryOAuthCallback({
  query,
  stateStore,
  tokenStore,
  exchange,
  nowMillis = Date.now,
}: CompleteCallbackOptions): Promise<CallbackResponse> {
  const now = nowMillis();
  const state = requireState(queryString(query, "state"));
  const storedState = await loadUsableState(stateStore, state, now).catch((error: unknown) =>
    redirectExpiredStateOrThrow(error, state),
  );
  if ("redirectUrl" in storedState) {
    return storedState;
  }
  const ravelryError = queryString(query, "error");

  if (ravelryError) {
    const expiredResult = await markStateUsedOrReject(stateStore, state, now).catch((error: unknown) =>
      redirectExpiredStateOrThrow(error, state),
    );
    if (expiredResult) {
      return expiredResult;
    }
    return { redirectUrl: appRedirectUrl(state, ravelryError) };
  }

  const code = queryString(query, "code");
  if (!code) {
    throw new RavelryAuthFlowError("missing_code", 400);
  }

  const expiredResult = await markStateUsedOrReject(stateStore, state, now).catch((error: unknown) =>
    redirectExpiredStateOrThrow(error, state),
  );
  if (expiredResult) {
    return expiredResult;
  }
  const token = await exchange({
    code,
    codeVerifier: storedState.codeVerifier,
    redirectUri: storedState.redirectUri,
  });

  await tokenStore.saveToken(tokenForStorage(storedState.uid, token, now));

  return { redirectUrl: appRedirectUrl(state) };
}

export async function getRavelryAuthStatus({
  uid,
  tokenStore,
}: UserTokenOptions): Promise<AuthStatusResponse> {
  const token = await tokenStore.getToken(uid);
  if (!token) {
    return { connected: false };
  }

  return {
    connected: true,
    ...(token.ravelryUsername ? { username: token.ravelryUsername } : {}),
    ...(token.lastVerifiedAtMillis != null
      ? { lastVerifiedAtMillis: token.lastVerifiedAtMillis }
      : {}),
  };
}

export async function getRavelryCurrentUser({
  uid,
  tokenStore,
  client,
  refresh,
  nowMillis = Date.now,
}: CurrentUserOptions): Promise<CurrentUserResponse> {
  const token = await getUsableRavelryToken({
    uid,
    tokenStore,
    refresh,
    nowMillis,
  });
  if (!token) {
    return { connected: false };
  }

  const currentUser = await client.getCurrentUser(token.accessToken);
  const now = nowMillis();
  await tokenStore.saveToken({
    ...token,
    ravelryUserId: currentUser.ravelryUserId,
    ravelryUsername: currentUser.ravelryUsername,
    updatedAtMillis: now,
    lastVerifiedAtMillis: now,
  });

  return {
    connected: true,
    ravelryUserId: currentUser.ravelryUserId,
    ravelryUsername: currentUser.ravelryUsername,
  };
}

export async function disconnectRavelry({
  uid,
  tokenStore,
  stateStore,
  nowMillis = Date.now,
}: DisconnectOptions): Promise<{ disconnected: true }> {
  const now = nowMillis();
  await tokenStore.deleteToken(uid);
  await stateStore.expireUnusedStatesForUid(uid, now);
  return { disconnected: true };
}
