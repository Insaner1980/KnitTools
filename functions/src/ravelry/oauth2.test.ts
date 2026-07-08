import assert from "node:assert/strict";
import { describe, it } from "node:test";

import {
  OAuthTokenExchangeError,
  exchangeOAuth2CodeForToken,
  refreshOAuth2AccessToken,
} from "./oauth2";

describe("Ravelry OAuth2 token exchange", () => {
  it("passes a timeout signal to the token request", async () => {
    let capturedSignal: AbortSignal | undefined;
    const fetchImpl: typeof fetch = async (_input, init) => {
      capturedSignal = init?.signal ?? undefined;
      return new Response(
        JSON.stringify({
          access_token: "access-token",
          refresh_token: "refresh-token",
          expires_in: 60,
        }),
        { status: 200 },
      );
    };

    const token = await exchangeOAuth2CodeForToken({
      clientId: "client-id",
      clientSecret: "client-secret",
      code: "auth-code",
      codeVerifier: "code-verifier",
      redirectUri: "https://callback.example/ravelry",
      fetchImpl,
      nowMillis: () => 1_000,
    });

    assert.ok(capturedSignal instanceof AbortSignal);
    assert.equal(capturedSignal.aborted, false);
    assert.deepEqual(token, {
      accessToken: "access-token",
      refreshToken: "refresh-token",
      expiresAtMillis: 61_000,
    });
  });

  it("maps timeout aborts to a sanitized exchange error", async () => {
    const tokenLikeErrorText = "access-token refresh-token client-secret";
    const fetchImpl: typeof fetch = async () => {
      throw new DOMException(`timed out with ${tokenLikeErrorText}`, "TimeoutError");
    };

    await assert.rejects(
      exchangeOAuth2CodeForToken({
        clientId: "client-id",
        clientSecret: "client-secret",
        code: "auth-code",
        codeVerifier: "code-verifier",
        redirectUri: "https://callback.example/ravelry",
        fetchImpl,
      }),
      (error: unknown) => {
        assert.ok(error instanceof OAuthTokenExchangeError);
        assert.equal(error.statusCode, 504);
        assert.equal(error.message, "ravelry_token_exchange_failed_504");
        assert.equal(error.message.includes(tokenLikeErrorText), false);
        return true;
      },
    );
  });

  it("refreshes access tokens with the refresh token grant", async () => {
    let capturedAuthorization: string | undefined;
    let capturedBody: string | undefined;
    const fetchImpl: typeof fetch = async (_input, init) => {
      if (init?.headers instanceof Headers) {
        capturedAuthorization = init.headers.get("Authorization") ?? undefined;
      } else if (Array.isArray(init?.headers)) {
        capturedAuthorization = init.headers.find(([name]) => name === "Authorization")?.[1];
      } else {
        const headers = init?.headers as Record<string, string> | undefined;
        capturedAuthorization = headers?.Authorization ?? headers?.authorization;
      }
      capturedBody = typeof init?.body === "string" ? init.body : undefined;
      return new Response(
        JSON.stringify({
          access_token: "fresh-access-token",
          refresh_token: "rotated-refresh-token",
          expires_in: 120,
        }),
        { status: 200 },
      );
    };

    const token = await refreshOAuth2AccessToken({
      clientId: "client-id",
      clientSecret: "client-secret",
      refreshToken: "old-refresh-token",
      fetchImpl,
      nowMillis: () => 1_000,
    });

    const body = new URLSearchParams(capturedBody);
    assert.equal(capturedAuthorization, "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=");
    assert.equal(body.get("grant_type"), "refresh_token");
    assert.equal(body.get("refresh_token"), "old-refresh-token");
    assert.deepEqual(token, {
      accessToken: "fresh-access-token",
      refreshToken: "rotated-refresh-token",
      expiresAtMillis: 121_000,
    });
  });
});
