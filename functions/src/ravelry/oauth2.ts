import { readJsonResponse } from "./boundedResponse";

export const RAVELRY_AUTHORIZATION_URL = "https://www.ravelry.com/oauth2/auth";
export const RAVELRY_TOKEN_URL = "https://www.ravelry.com/oauth2/token";
const RAVELRY_TOKEN_EXCHANGE_TIMEOUT_MILLIS = 10_000;
const RAVELRY_TOKEN_RESPONSE_MAX_BYTES = 65_536;

export interface OAuthTokenExchangeRequest {
  readonly code: string;
  readonly codeVerifier: string;
  readonly redirectUri: string;
}

export interface OAuthTokenRefreshRequest {
  readonly refreshToken: string;
}

export interface OAuthTokenExchangeResult {
  readonly accessToken: string;
  readonly refreshToken?: string;
  readonly expiresAtMillis?: number;
}

export type OAuthTokenExchange = (
  request: OAuthTokenExchangeRequest,
) => Promise<OAuthTokenExchangeResult>;

export type OAuthTokenRefresh = (
  request: OAuthTokenRefreshRequest,
) => Promise<OAuthTokenExchangeResult>;

export interface OAuthTokenExchangeOptions extends OAuthTokenExchangeRequest {
  readonly clientId: string;
  readonly clientSecret: string;
  readonly fetchImpl?: typeof fetch;
  readonly nowMillis?: () => number;
}

export interface OAuthTokenRefreshOptions extends OAuthTokenRefreshRequest {
  readonly clientId: string;
  readonly clientSecret: string;
  readonly fetchImpl?: typeof fetch;
  readonly nowMillis?: () => number;
}

export class OAuthTokenExchangeError extends Error {
  constructor(readonly statusCode: number) {
    super(`ravelry_token_exchange_failed_${statusCode}`);
  }
}

function basicAuthHeader(clientId: string, clientSecret: string): string {
  return `Basic ${Buffer.from(`${clientId}:${clientSecret}`, "utf8").toString("base64")}`;
}

function errorName(error: unknown): string | undefined {
  return typeof error === "object" && error !== null && "name" in error
    ? String((error as { name: unknown }).name)
    : undefined;
}

function isAbortOrTimeoutError(error: unknown): boolean {
  const name = errorName(error);
  return name === "AbortError" || name === "TimeoutError";
}

async function requestOAuth2Token({
  clientId,
  clientSecret,
  requestBody,
  fetchImpl,
  nowMillis,
}: {
  readonly clientId: string;
  readonly clientSecret: string;
  readonly requestBody: URLSearchParams;
  readonly fetchImpl: typeof fetch;
  readonly nowMillis: () => number;
}): Promise<OAuthTokenExchangeResult> {
  let response: Response;
  try {
    response = await fetchImpl(RAVELRY_TOKEN_URL, {
      method: "POST",
      headers: {
        Authorization: basicAuthHeader(clientId, clientSecret),
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: requestBody.toString(),
      redirect: "error",
      signal: AbortSignal.timeout(RAVELRY_TOKEN_EXCHANGE_TIMEOUT_MILLIS),
    });
  } catch (error) {
    if (isAbortOrTimeoutError(error)) {
      throw new OAuthTokenExchangeError(504);
    }
    throw error;
  }

  if (!response.ok) {
    throw new OAuthTokenExchangeError(response.status);
  }

  let body: {
    access_token?: unknown;
    refresh_token?: unknown;
    expires_in?: unknown;
  };
  try {
    body = (await readJsonResponse(response, RAVELRY_TOKEN_RESPONSE_MAX_BYTES)) as typeof body;
  } catch {
    throw new OAuthTokenExchangeError(503);
  }

  if (typeof body.access_token !== "string" || body.access_token.length === 0) {
    throw new OAuthTokenExchangeError(response.status);
  }

  const expiresInSeconds =
    typeof body.expires_in === "number" && Number.isFinite(body.expires_in)
      ? body.expires_in
      : undefined;

  return {
    accessToken: body.access_token,
    refreshToken:
      typeof body.refresh_token === "string" && body.refresh_token.length > 0
        ? body.refresh_token
        : undefined,
    expiresAtMillis:
      expiresInSeconds == null ? undefined : nowMillis() + Math.max(0, expiresInSeconds) * 1_000,
  };
}

export async function exchangeOAuth2CodeForToken({
  clientId,
  clientSecret,
  code,
  codeVerifier,
  redirectUri,
  fetchImpl = fetch,
  nowMillis = Date.now,
}: OAuthTokenExchangeOptions): Promise<OAuthTokenExchangeResult> {
  return requestOAuth2Token({
    clientId,
    clientSecret,
    fetchImpl,
    nowMillis,
    requestBody: new URLSearchParams({
      grant_type: "authorization_code",
      code,
      redirect_uri: redirectUri,
      code_verifier: codeVerifier,
    }),
  });
}

export async function refreshOAuth2AccessToken({
  clientId,
  clientSecret,
  refreshToken,
  fetchImpl = fetch,
  nowMillis = Date.now,
}: OAuthTokenRefreshOptions): Promise<OAuthTokenExchangeResult> {
  return requestOAuth2Token({
    clientId,
    clientSecret,
    fetchImpl,
    nowMillis,
    requestBody: new URLSearchParams({
      grant_type: "refresh_token",
      refresh_token: refreshToken,
    }),
  });
}
