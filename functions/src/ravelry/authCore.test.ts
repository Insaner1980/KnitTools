import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { describe, it } from "node:test";

import {
  completeRavelryOAuthCallback,
  disconnectRavelry,
  getRavelryAuthStatus,
  getRavelryCurrentUser,
  startRavelryOAuth,
} from "./authCore";
import type { RavelryCurrentUserClient } from "./client";
import type { OAuthStateStore } from "./oauthStateStore";
import type { OAuthTokenExchange, OAuthTokenRefresh } from "./oauth2";
import { disabledRavelryRateLimiter } from "./rateLimit";
import type { RavelryTokenStore, StoredRavelryToken } from "./tokenStore";

function callbackState(label: string): string {
  return createHash("sha256").update(`callback-test:${label}`).digest("base64url");
}

function completeCallback(
  options: Omit<
    Parameters<typeof completeRavelryOAuthCallback>[0],
    "rateLimiter" | "rateLimitKey"
  >,
) {
  return completeRavelryOAuthCallback({
    ...options,
    rateLimiter: disabledRavelryRateLimiter,
    rateLimitKey: "callback-test-client",
  });
}

class MemoryOAuthStateStore implements OAuthStateStore {
  readonly states = new Map<string, Awaited<ReturnType<OAuthStateStore["getState"]>>>();
  getStateCalls = 0;

  async saveState(state: NonNullable<Awaited<ReturnType<OAuthStateStore["getState"]>>>): Promise<void> {
    this.states.set(state.state, state);
  }

  async getState(state: string): Promise<Awaited<ReturnType<OAuthStateStore["getState"]>>> {
    this.getStateCalls += 1;
    return this.states.get(state) ?? null;
  }

  async markStateUsed(state: string, usedAtMillis: number): Promise<boolean> {
    const existing = this.states.get(state);
    if (!existing || existing.usedAtMillis != null || existing.expiresAtMillis <= usedAtMillis) {
      return false;
    }
    this.states.set(state, { ...existing, usedAtMillis });
    return true;
  }

  async expireUnusedStatesForUid(uid: string, expiresAtMillis: number): Promise<void> {
    this.states.forEach((state, key) => {
      if (state?.uid === uid && state.usedAtMillis == null) {
        this.states.set(key, { ...state, expiresAtMillis });
      }
    });
  }
}

class MemoryTokenStore implements RavelryTokenStore {
  readonly collectionPath = "ravelryTokens";
  readonly tokens = new Map<string, StoredRavelryToken>();
  private readonly generations = new Map<string, number>();

  async getToken(uid: string): Promise<StoredRavelryToken | null> {
    return this.tokens.get(uid) ?? null;
  }

  async getConnectionGeneration(uid: string): Promise<number> {
    return this.generations.get(uid) ?? this.tokens.get(uid)?.connectionGeneration ?? 0;
  }

  async saveToken(token: StoredRavelryToken): Promise<void> {
    const connectionGeneration = token.connectionGeneration ?? await this.getConnectionGeneration(token.uid);
    this.generations.set(token.uid, connectionGeneration);
    this.tokens.set(token.uid, { ...token, connectionGeneration });
  }

  async saveTokenIfGenerationCurrent(
    token: StoredRavelryToken,
    expectedGeneration: number,
  ): Promise<boolean> {
    if (await this.getConnectionGeneration(token.uid) !== expectedGeneration) {
      return false;
    }
    this.generations.set(token.uid, expectedGeneration);
    this.tokens.set(token.uid, { ...token, connectionGeneration: expectedGeneration });
    return true;
  }

  async saveRefreshedTokenIfCurrent(
    token: StoredRavelryToken,
    expectedToken: StoredRavelryToken,
  ): Promise<StoredRavelryToken | null> {
    const current = this.tokens.get(token.uid);
    if (!current ||
      current.accessToken !== expectedToken.accessToken ||
      current.refreshToken !== expectedToken.refreshToken ||
      current.expiresAtMillis !== expectedToken.expiresAtMillis ||
      (current.connectionGeneration ?? 0) !== (expectedToken.connectionGeneration ?? 0)) {
      return null;
    }
    const persisted = {
      ...token,
      ravelryUserId: current.ravelryUserId,
      ravelryUsername: current.ravelryUsername,
      lastVerifiedAtMillis: current.lastVerifiedAtMillis,
      connectionGeneration: current.connectionGeneration ?? 0,
    };
    this.tokens.set(token.uid, persisted);
    return persisted;
  }

  async updateUserMetadataIfGenerationCurrent(
    uid: string,
    update: {
      readonly ravelryUserId?: string;
      readonly ravelryUsername?: string;
      readonly verifiedAtMillis: number;
    },
    expectedGeneration: number,
  ): Promise<boolean> {
    const current = this.tokens.get(uid);
    if (!current || (current.connectionGeneration ?? 0) !== expectedGeneration) {
      return false;
    }
    this.tokens.set(uid, {
      ...current,
      ravelryUserId: update.ravelryUserId,
      ravelryUsername: update.ravelryUsername,
      updatedAtMillis: update.verifiedAtMillis,
      lastVerifiedAtMillis: update.verifiedAtMillis,
    });
    return true;
  }

  async deleteToken(uid: string, _nowMillis?: number): Promise<void> {
    this.generations.set(uid, await this.getConnectionGeneration(uid) + 1);
    this.tokens.delete(uid);
  }
}

describe("Ravelry OAuth2 auth core", () => {
  it("starts OAuth with state, PKCE, expiry, and no returned secrets", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();

    const result = await startRavelryOAuth({
      uid: "firebase-uid",
      stateStore,
      tokenStore,
      clientId: "client-id",
      backendCallbackUrl: "https://example.cloudfunctions.net/ravelryCallback",
      nowMillis: () => 1_000,
      randomString: (label) => `${label}-random`,
    });

    const authorizeUrl = new URL(result.authorizeUrl);
    assert.equal(authorizeUrl.origin + authorizeUrl.pathname, "https://www.ravelry.com/oauth2/auth");
    assert.equal(authorizeUrl.searchParams.get("response_type"), "code");
    assert.equal(authorizeUrl.searchParams.get("client_id"), "client-id");
    assert.equal(authorizeUrl.searchParams.get("redirect_uri"), "https://example.cloudfunctions.net/ravelryCallback");
    assert.equal(authorizeUrl.searchParams.get("scope"), "offline");
    assert.equal(authorizeUrl.searchParams.get("state"), "state-random");
    assert.equal(authorizeUrl.searchParams.get("code_challenge_method"), "S256");
    assert.equal(result.state, "state-random");
    assert.equal(result.expiresAtMillis, 601_000);
    assert.equal(JSON.stringify(result).includes("code-verifier"), false);

    const storedState = await stateStore.getState("state-random");
    assert.equal(storedState?.uid, "firebase-uid");
    assert.equal(storedState?.authType, "oauth2");
    assert.equal(storedState?.codeVerifier, "code-verifier-random");
    assert.equal(storedState?.connectionGeneration, 0);
    assert.equal(storedState?.usedAtMillis, null);
  });

  it("rejects malformed callback states before reading the state store", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();

    for (const state of ["A".repeat(42), "A".repeat(44), "invalid/state"]) {
      await assert.rejects(
        completeCallback({
          query: { state, code: "code" },
          stateStore,
          tokenStore,
          exchange: async () => ({ accessToken: "not-used" }),
        }),
        (error: unknown) => error instanceof Error && error.message === "invalid_state",
      );
    }

    assert.equal(stateStore.getStateCalls, 0);
  });

  it("rate limits valid callback states before reading the state store", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();
    const rateLimitError = new Error("ravelry_rate_limited");
    const calls: Array<{ readonly key: string; readonly bucket: string }> = [];
    const rateLimiter = {
      async consume(key: string, bucket: string): Promise<void> {
        calls.push({ key, bucket });
        throw rateLimitError;
      },
    };
    const options = {
      query: { state: "A".repeat(43), code: "code" },
      stateStore,
      tokenStore,
      exchange: async () => ({ accessToken: "not-used" }),
      rateLimiter,
      rateLimitKey: "callback-client",
    } as Parameters<typeof completeRavelryOAuthCallback>[0] & {
      readonly rateLimiter: typeof rateLimiter;
      readonly rateLimitKey: string;
    };

    await assert.rejects(
      completeRavelryOAuthCallback(options),
      (error: unknown) => error === rateLimitError,
    );

    assert.deepEqual(calls, [{ key: "callback-client", bucket: "callback" }]);
    assert.equal(stateStore.getStateCalls, 0);
  });

  it("rejects missing and used callback params while redirecting expired states before token exchange", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();
    let exchangeCount = 0;
    const exchange: OAuthTokenExchange = async () => {
      exchangeCount += 1;
      throw new Error("should not exchange");
    };

    await assert.rejects(
      completeCallback({
        query: { code: "code" },
        stateStore,
        tokenStore,
        exchange,
        nowMillis: () => 1_000,
      }),
      /missing_state/,
    );

    await stateStore.saveState({
      state: callbackState("expired"),
      uid: "uid",
      authType: "oauth2",
      createdAtMillis: 0,
      expiresAtMillis: 999,
      usedAtMillis: null,
      redirectUri: "https://callback",
      codeVerifier: "verifier",
      codeChallenge: "challenge",
      codeChallengeMethod: "S256",
    });

    const expiredResult = await completeCallback({
      query: { state: callbackState("expired"), code: "code" },
      stateStore,
      tokenStore,
      exchange,
      nowMillis: () => 1_000,
    });

    assert.equal(
      expiredResult.redirectUrl,
      `knittools://ravelry-auth-complete?state=${callbackState("expired")}&error=state_expired`,
    );

    await stateStore.saveState({
      state: callbackState("used"),
      uid: "uid",
      authType: "oauth2",
      createdAtMillis: 0,
      expiresAtMillis: 2_000,
      usedAtMillis: 1_500,
      redirectUri: "https://callback",
      codeVerifier: "verifier",
      codeChallenge: "challenge",
      codeChallengeMethod: "S256",
    });

    await assert.rejects(
      completeCallback({
        query: { state: callbackState("used"), code: "code" },
        stateStore,
        tokenStore,
        exchange,
        nowMillis: () => 1_000,
      }),
      /used_state/,
    );

    assert.equal(exchangeCount, 0);
  });

  it("bounds callback values and does not reflect arbitrary OAuth errors to the app", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();
    const state = callbackState("bounded-callback");
    await stateStore.saveState({
      state,
      uid: "uid",
      authType: "oauth2",
      createdAtMillis: 0,
      expiresAtMillis: 2_000,
      usedAtMillis: null,
      redirectUri: "https://callback",
      codeVerifier: "verifier",
      codeChallenge: "challenge",
      codeChallengeMethod: "S256",
    });

    const result = await completeCallback({
      query: { state, error: "token-shaped-private-error" },
      stateStore,
      tokenStore,
      exchange: async () => ({ accessToken: "not-used" }),
      nowMillis: () => 1_000,
    });

    assert.equal(
      result.redirectUrl,
      `knittools://ravelry-auth-complete?state=${state}&error=oauth_error`,
    );
    assert.equal(result.redirectUrl.includes("token-shaped-private-error"), false);

    const codeState = callbackState("oversized-code");
    await stateStore.saveState({
      state: codeState,
      uid: "uid",
      authType: "oauth2",
      createdAtMillis: 0,
      expiresAtMillis: 2_000,
      usedAtMillis: null,
      redirectUri: "https://callback",
      codeVerifier: "verifier",
      codeChallenge: "challenge",
      codeChallengeMethod: "S256",
    });
    await assert.rejects(
      completeCallback({
        query: { state: codeState, code: "A".repeat(2_049) },
        stateStore,
        tokenStore,
        exchange: async () => ({ accessToken: "not-used" }),
        nowMillis: () => 1_000,
      }),
      /invalid_code/,
    );
  });

  it("exchanges a valid callback code, stores tokens, marks state used, and redirects to the app", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();

    await stateStore.saveState({
      state: callbackState("valid"),
      uid: "uid",
      authType: "oauth2",
      createdAtMillis: 0,
      expiresAtMillis: 2_000,
      usedAtMillis: null,
      redirectUri: "https://callback",
      codeVerifier: "verifier",
      codeChallenge: "challenge",
      codeChallengeMethod: "S256",
    });

    const result = await completeCallback({
      query: { state: callbackState("valid"), code: "auth-code" },
      stateStore,
      tokenStore,
      exchange: async ({ code, codeVerifier, redirectUri }) => {
        assert.equal(code, "auth-code");
        assert.equal(codeVerifier, "verifier");
        assert.equal(redirectUri, "https://callback");
        return {
          accessToken: "access-token",
          refreshToken: "refresh-token",
          expiresAtMillis: 8_000,
        };
      },
      nowMillis: () => 1_000,
    });

    assert.equal(
      result.redirectUrl,
      `knittools://ravelry-auth-complete?state=${callbackState("valid")}`,
    );
    assert.equal((await stateStore.getState(callbackState("valid")))?.usedAtMillis, 1_000);
    assert.deepEqual(await tokenStore.getToken("uid"), {
      uid: "uid",
      authType: "oauth2",
      accessToken: "access-token",
      refreshToken: "refresh-token",
      expiresAtMillis: 8_000,
      createdAtMillis: 1_000,
      updatedAtMillis: 1_000,
      connectionGeneration: 0,
    });
  });

  it("does not restore a disconnected token when OAuth callback exchange finishes late", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();

    await stateStore.saveState({
      state: callbackState("disconnect-race"),
      uid: "uid",
      authType: "oauth2",
      createdAtMillis: 0,
      expiresAtMillis: 2_000,
      usedAtMillis: null,
      redirectUri: "https://callback",
      codeVerifier: "verifier",
      codeChallenge: "challenge",
      codeChallengeMethod: "S256",
    });

    const result = await completeCallback({
      query: { state: callbackState("disconnect-race"), code: "auth-code" },
      stateStore,
      tokenStore,
      exchange: async () => {
        await disconnectRavelry({
          uid: "uid",
          tokenStore,
          stateStore,
          nowMillis: () => 1_500,
        });
        return {
          accessToken: "late-access-token",
          refreshToken: "late-refresh-token",
        };
      },
      nowMillis: () => 1_000,
    });

    assert.equal(
      result.redirectUrl,
      `knittools://ravelry-auth-complete?state=${callbackState("disconnect-race")}&error=state_expired`,
    );
    assert.equal(await tokenStore.getToken("uid"), null);
    assert.equal(await tokenStore.getConnectionGeneration("uid"), 1);
  });

  it("does not restore a disconnected token when disconnect wins before callback generation is selected", async () => {
    const stateStore = new MemoryOAuthStateStore();
    let disconnected = false;
    const tokenStore = new class extends MemoryTokenStore {
      async getConnectionGeneration(uid: string): Promise<number> {
        if (!disconnected) {
          disconnected = true;
          await disconnectRavelry({
            uid,
            tokenStore: this,
            stateStore,
            nowMillis: () => 1_500,
          });
        }
        return super.getConnectionGeneration(uid);
      }
    }();
    let exchangeCount = 0;

    await stateStore.saveState({
      state: callbackState("generation-race"),
      uid: "uid",
      authType: "oauth2",
      createdAtMillis: 0,
      expiresAtMillis: 2_000,
      usedAtMillis: null,
      redirectUri: "https://callback",
      codeVerifier: "verifier",
      codeChallenge: "challenge",
      codeChallengeMethod: "S256",
    });

    const result = await completeCallback({
      query: { state: callbackState("generation-race"), code: "auth-code" },
      stateStore,
      tokenStore,
      exchange: async () => {
        exchangeCount += 1;
        return {
          accessToken: "late-access-token",
          refreshToken: "late-refresh-token",
        };
      },
      nowMillis: () => 1_000,
    });

    assert.equal(
      result.redirectUrl,
      `knittools://ravelry-auth-complete?state=${callbackState("generation-race")}&error=state_expired`,
    );
    assert.equal(exchangeCount, 0);
    assert.equal(await tokenStore.getToken("uid"), null);
    assert.equal(await tokenStore.getConnectionGeneration("uid"), 1);
  });

  it("does not persist undefined optional token fields", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();

    await stateStore.saveState({
      state: callbackState("minimal-token"),
      uid: "uid",
      authType: "oauth2",
      createdAtMillis: 0,
      expiresAtMillis: 2_000,
      usedAtMillis: null,
      redirectUri: "https://callback",
      codeVerifier: "verifier",
      codeChallenge: "challenge",
      codeChallengeMethod: "S256",
    });

    await completeCallback({
      query: { state: callbackState("minimal-token"), code: "auth-code" },
      stateStore,
      tokenStore,
      exchange: async () => ({ accessToken: "access-token" }),
      nowMillis: () => 1_000,
    });

    const token = await tokenStore.getToken("uid");
    assert.equal(Object.hasOwn(token ?? {}, "refreshToken"), false);
    assert.equal(Object.hasOwn(token ?? {}, "expiresAtMillis"), false);
  });

  it("does not exchange when state consumption loses a replay race", async () => {
    class ReplayRaceOAuthStateStore extends MemoryOAuthStateStore {
      override async markStateUsed(state: string, usedAtMillis: number): Promise<boolean> {
        await super.markStateUsed(state, usedAtMillis);
        return false;
      }
    }

    const stateStore = new ReplayRaceOAuthStateStore();
    const tokenStore = new MemoryTokenStore();
    let exchangeCount = 0;

    await stateStore.saveState({
      state: callbackState("race"),
      uid: "uid",
      authType: "oauth2",
      createdAtMillis: 0,
      expiresAtMillis: 2_000,
      usedAtMillis: null,
      redirectUri: "https://callback",
      codeVerifier: "verifier",
      codeChallenge: "challenge",
      codeChallengeMethod: "S256",
    });

    await assert.rejects(
      completeCallback({
        query: { state: callbackState("race"), code: "auth-code" },
        stateStore,
        tokenStore,
        exchange: async () => {
          exchangeCount += 1;
          return { accessToken: "access-token" };
        },
        nowMillis: () => 1_000,
      }),
      /used_state/,
    );

    assert.equal(exchangeCount, 0);
  });

  it("does not exchange when a state expires before atomic consumption", async () => {
    class ExpiringBeforeConsumeOAuthStateStore extends MemoryOAuthStateStore {
      override async markStateUsed(state: string, usedAtMillis: number): Promise<boolean> {
        const existing = this.states.get(state);
        if (existing) {
          this.states.set(state, { ...existing, expiresAtMillis: usedAtMillis });
        }
        return super.markStateUsed(state, usedAtMillis);
      }
    }

    const stateStore = new ExpiringBeforeConsumeOAuthStateStore();
    const tokenStore = new MemoryTokenStore();
    let exchangeCount = 0;

    await stateStore.saveState({
      state: callbackState("expires-before-consume"),
      uid: "uid",
      authType: "oauth2",
      createdAtMillis: 0,
      expiresAtMillis: 2_000,
      usedAtMillis: null,
      redirectUri: "https://callback",
      codeVerifier: "verifier",
      codeChallenge: "challenge",
      codeChallengeMethod: "S256",
    });

    const result = await completeCallback({
      query: { state: callbackState("expires-before-consume"), code: "auth-code" },
      stateStore,
      tokenStore,
      exchange: async () => {
        exchangeCount += 1;
        return { accessToken: "access-token" };
      },
      nowMillis: () => 1_000,
    });

    assert.equal(
      result.redirectUrl,
      `knittools://ravelry-auth-complete?state=${callbackState("expires-before-consume")}&error=state_expired`,
    );

    assert.equal(exchangeCount, 0);
  });

  it("reports status, fetches current user, and disconnects without exposing tokens", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();
    const client: RavelryCurrentUserClient = {
      async getCurrentUser(accessToken) {
        assert.equal(accessToken, "access-token");
        return { ravelryUserId: "42", ravelryUsername: "knitter" };
      },
    };

    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "access-token",
      refreshToken: "refresh-token",
      createdAtMillis: 1_000,
      updatedAtMillis: 1_000,
    });

    assert.deepEqual(await getRavelryAuthStatus({ uid: "missing", tokenStore }), { connected: false });
    assert.deepEqual(await getRavelryAuthStatus({ uid: "uid", tokenStore }), {
      connected: true,
    });

    assert.deepEqual(
      await getRavelryCurrentUser({
        uid: "uid",
        tokenStore,
        client,
        nowMillis: () => 2_000,
      }),
      {
        connected: true,
        ravelryUserId: "42",
        ravelryUsername: "knitter",
      },
    );

    assert.deepEqual(await getRavelryAuthStatus({ uid: "uid", tokenStore }), {
      connected: true,
      username: "knitter",
      lastVerifiedAtMillis: 2_000,
    });

    assert.deepEqual(
      await disconnectRavelry({
        uid: "uid",
        tokenStore,
        stateStore,
        nowMillis: () => 3_000,
      }),
      { disconnected: true },
    );
    assert.equal(await tokenStore.getToken("uid"), null);
  });

  it("does not restore a disconnected token when current-user verification finishes late", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();
    const client: RavelryCurrentUserClient = {
      async getCurrentUser(accessToken) {
        assert.equal(accessToken, "access-token");
        await disconnectRavelry({
          uid: "uid",
          tokenStore,
          stateStore,
          nowMillis: () => 1_500,
        });
        return { ravelryUserId: "42", ravelryUsername: "knitter" };
      },
    };

    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "access-token",
      refreshToken: "refresh-token",
      createdAtMillis: 1_000,
      updatedAtMillis: 1_000,
    });

    assert.deepEqual(
      await getRavelryCurrentUser({
        uid: "uid",
        tokenStore,
        client,
        nowMillis: () => 2_000,
      }),
      { connected: false },
    );
    assert.equal(await tokenStore.getToken("uid"), null);
    assert.equal(await tokenStore.getConnectionGeneration("uid"), 1);
  });

  it("preserves credentials refreshed while current-user verification is in flight", async () => {
    const tokenStore = new MemoryTokenStore();
    const client: RavelryCurrentUserClient = {
      async getCurrentUser(accessToken) {
        assert.equal(accessToken, "old-access-token");
        await tokenStore.saveToken({
          uid: "uid",
          authType: "oauth2",
          accessToken: "fresh-access-token",
          refreshToken: "rotated-refresh-token",
          expiresAtMillis: 10_000,
          createdAtMillis: 100,
          updatedAtMillis: 1_500,
          connectionGeneration: 0,
        });
        return { ravelryUserId: "42", ravelryUsername: "knitter" };
      },
    };

    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "old-access-token",
      refreshToken: "old-refresh-token",
      expiresAtMillis: 5_000,
      createdAtMillis: 100,
      updatedAtMillis: 100,
    });

    assert.deepEqual(
      await getRavelryCurrentUser({
        uid: "uid",
        tokenStore,
        client,
        nowMillis: () => 2_000,
      }),
      {
        connected: true,
        ravelryUserId: "42",
        ravelryUsername: "knitter",
      },
    );
    assert.deepEqual(await tokenStore.getToken("uid"), {
      uid: "uid",
      authType: "oauth2",
      accessToken: "fresh-access-token",
      refreshToken: "rotated-refresh-token",
      expiresAtMillis: 10_000,
      ravelryUserId: "42",
      ravelryUsername: "knitter",
      createdAtMillis: 100,
      updatedAtMillis: 2_000,
      lastVerifiedAtMillis: 2_000,
      connectionGeneration: 0,
    });
  });

  it("refreshes an expired token before fetching the current Ravelry user", async () => {
    const tokenStore = new MemoryTokenStore();
    const client: RavelryCurrentUserClient = {
      async getCurrentUser(accessToken) {
        assert.equal(accessToken, "fresh-access-token");
        return { ravelryUserId: "42", ravelryUsername: "knitter" };
      },
    };
    const refresh: OAuthTokenRefresh = async ({ refreshToken }) => {
      assert.equal(refreshToken, "old-refresh-token");
      return {
        accessToken: "fresh-access-token",
        refreshToken: "rotated-refresh-token",
        expiresAtMillis: 10_000,
      };
    };

    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "expired-access-token",
      refreshToken: "old-refresh-token",
      expiresAtMillis: 999,
      createdAtMillis: 100,
      updatedAtMillis: 100,
    });

    assert.deepEqual(
      await getRavelryCurrentUser({
        uid: "uid",
        tokenStore,
        client,
        refresh,
        nowMillis: () => 1_000,
      }),
      {
        connected: true,
        ravelryUserId: "42",
        ravelryUsername: "knitter",
      },
    );
    assert.deepEqual(await tokenStore.getToken("uid"), {
      uid: "uid",
      authType: "oauth2",
      accessToken: "fresh-access-token",
      refreshToken: "rotated-refresh-token",
      expiresAtMillis: 10_000,
      ravelryUserId: "42",
      ravelryUsername: "knitter",
      createdAtMillis: 100,
      updatedAtMillis: 1_000,
      lastVerifiedAtMillis: 1_000,
      connectionGeneration: 0,
    });
  });

  it("does not use a refreshed token when disconnect wins during refresh", async () => {
    const tokenStore = new MemoryTokenStore();
    let currentUserCalled = false;
    const client: RavelryCurrentUserClient = {
      async getCurrentUser() {
        currentUserCalled = true;
        return { ravelryUserId: "42", ravelryUsername: "knitter" };
      },
    };
    const refresh: OAuthTokenRefresh = async ({ refreshToken }) => {
      assert.equal(refreshToken, "old-refresh-token");
      await tokenStore.deleteToken("uid", 1_500);
      return {
        accessToken: "fresh-access-token",
        refreshToken: "rotated-refresh-token",
        expiresAtMillis: 10_000,
      };
    };

    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "expired-access-token",
      refreshToken: "old-refresh-token",
      expiresAtMillis: 999,
      createdAtMillis: 100,
      updatedAtMillis: 100,
    });

    assert.deepEqual(
      await getRavelryCurrentUser({
        uid: "uid",
        tokenStore,
        client,
        refresh,
        nowMillis: () => 1_000,
      }),
      { connected: false },
    );
    assert.equal(currentUserCalled, false);
    assert.equal(await tokenStore.getToken("uid"), null);
    assert.equal(await tokenStore.getConnectionGeneration("uid"), 1);
  });
});
