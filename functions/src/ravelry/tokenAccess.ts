import type { OAuthTokenRefresh } from "./oauth2";
import type { RavelryTokenStore, StoredRavelryToken } from "./tokenStore";

interface UsableTokenOptions {
  readonly uid: string;
  readonly tokenStore: RavelryTokenStore;
  readonly refresh?: OAuthTokenRefresh;
  readonly nowMillis?: () => number;
}

function requiresRefresh(token: StoredRavelryToken, nowMillis: number): boolean {
  return token.expiresAtMillis != null && token.expiresAtMillis <= nowMillis;
}

function refreshedToken(
  token: StoredRavelryToken,
  refreshed: Awaited<ReturnType<OAuthTokenRefresh>>,
  updatedAtMillis: number,
): StoredRavelryToken {
  return {
    uid: token.uid,
    authType: token.authType,
    accessToken: refreshed.accessToken,
    ...(refreshed.refreshToken ?? token.refreshToken
      ? { refreshToken: refreshed.refreshToken ?? token.refreshToken }
      : {}),
    ...(refreshed.expiresAtMillis != null ? { expiresAtMillis: refreshed.expiresAtMillis } : {}),
    ...(token.ravelryUserId ? { ravelryUserId: token.ravelryUserId } : {}),
    ...(token.ravelryUsername ? { ravelryUsername: token.ravelryUsername } : {}),
    createdAtMillis: token.createdAtMillis,
    updatedAtMillis,
    ...(token.lastVerifiedAtMillis != null ? { lastVerifiedAtMillis: token.lastVerifiedAtMillis } : {}),
  };
}

export async function getUsableRavelryToken({
  uid,
  tokenStore,
  refresh,
  nowMillis = Date.now,
}: UsableTokenOptions): Promise<StoredRavelryToken | null> {
  const token = await tokenStore.getToken(uid);
  if (!token) {
    return null;
  }

  const now = nowMillis();
  if (!requiresRefresh(token, now)) {
    return token;
  }

  if (!token.refreshToken || !refresh) {
    return null;
  }

  const nextToken = refreshedToken(token, await refresh({ refreshToken: token.refreshToken }), now);
  await tokenStore.saveToken(nextToken);
  return nextToken;
}
