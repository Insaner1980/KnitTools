export const RAVELRY_AUTHORIZATION_URL = "https://www.ravelry.com/oauth2/auth";
export const RAVELRY_TOKEN_URL = "https://www.ravelry.com/oauth2/token";

export interface OAuthTokenExchangeRequest {
  readonly code: string;
  readonly codeVerifier: string;
  readonly redirectUri: string;
}

export interface OAuthTokenExchangeResult {
  readonly accessToken: string;
  readonly refreshToken?: string;
  readonly expiresAtMillis?: number;
}

export type OAuthTokenExchange = (
  request: OAuthTokenExchangeRequest,
) => Promise<OAuthTokenExchangeResult>;

export interface OAuthTokenExchangeOptions extends OAuthTokenExchangeRequest {
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

export async function exchangeOAuth2CodeForToken({
  clientId,
  clientSecret,
  code,
  codeVerifier,
  redirectUri,
  fetchImpl = fetch,
  nowMillis = Date.now,
}: OAuthTokenExchangeOptions): Promise<OAuthTokenExchangeResult> {
  const response = await fetchImpl(RAVELRY_TOKEN_URL, {
    method: "POST",
    headers: {
      Authorization: basicAuthHeader(clientId, clientSecret),
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({
      grant_type: "authorization_code",
      code,
      redirect_uri: redirectUri,
      code_verifier: codeVerifier,
    }).toString(),
  });

  if (!response.ok) {
    throw new OAuthTokenExchangeError(response.status);
  }

  const body = (await response.json()) as {
    access_token?: unknown;
    refresh_token?: unknown;
    expires_in?: unknown;
  };

  if (typeof body.access_token !== "string" || body.access_token.length === 0) {
    throw new OAuthTokenExchangeError(response.status);
  }

  const expiresInSeconds =
    typeof body.expires_in === "number" && Number.isFinite(body.expires_in)
      ? body.expires_in
      : undefined;

  return {
    accessToken: body.access_token,
    refreshToken: typeof body.refresh_token === "string" ? body.refresh_token : undefined,
    expiresAtMillis:
      expiresInSeconds == null ? undefined : nowMillis() + Math.max(0, expiresInSeconds) * 1_000,
  };
}
