import assert from "node:assert/strict";
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
import type { RavelryTokenStore, StoredRavelryToken } from "./tokenStore";

class MemoryOAuthStateStore implements OAuthStateStore {
  readonly states = new Map<string, Awaited<ReturnType<OAuthStateStore["getState"]>>>();

  async saveState(state: NonNullable<Awaited<ReturnType<OAuthStateStore["getState"]>>>): Promise<void> {
    this.states.set(state.state, state);
  }

  async getState(state: string): Promise<Awaited<ReturnType<OAuthStateStore["getState"]>>> {
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

  it("rejects missing and used callback params while redirecting expired states before token exchange", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();
    let exchangeCount = 0;
    const exchange: OAuthTokenExchange = async () => {
      exchangeCount += 1;
      throw new Error("should not exchange");
    };

    await assert.rejects(
      completeRavelryOAuthCallback({
        query: { code: "code" },
        stateStore,
        tokenStore,
        exchange,
        nowMillis: () => 1_000,
      }),
      /missing_state/,
    );

    await stateStore.saveState({
      state: "expired",
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

    const expiredResult = await completeRavelryOAuthCallback({
      query: { state: "expired", code: "code" },
      stateStore,
      tokenStore,
      exchange,
      nowMillis: () => 1_000,
    });

    assert.equal(
      expiredResult.redirectUrl,
      "knittools://ravelry-auth-complete?state=expired&error=state_expired",
    );

    await stateStore.saveState({
      state: "used",
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
      completeRavelryOAuthCallback({
        query: { state: "used", code: "code" },
        stateStore,
        tokenStore,
        exchange,
        nowMillis: () => 1_000,
      }),
      /used_state/,
    );

    assert.equal(exchangeCount, 0);
  });

  it("exchanges a valid callback code, stores tokens, marks state used, and redirects to the app", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();

    await stateStore.saveState({
      state: "valid",
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

    const result = await completeRavelryOAuthCallback({
      query: { state: "valid", code: "auth-code" },
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

    assert.equal(result.redirectUrl, "knittools://ravelry-auth-complete?state=valid");
    assert.equal((await stateStore.getState("valid"))?.usedAtMillis, 1_000);
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
      state: "disconnect-race",
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

    const result = await completeRavelryOAuthCallback({
      query: { state: "disconnect-race", code: "auth-code" },
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
      "knittools://ravelry-auth-complete?state=disconnect-race&error=state_expired",
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
      state: "generation-race",
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

    const result = await completeRavelryOAuthCallback({
      query: { state: "generation-race", code: "auth-code" },
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
      "knittools://ravelry-auth-complete?state=generation-race&error=state_expired",
    );
    assert.equal(exchangeCount, 0);
    assert.equal(await tokenStore.getToken("uid"), null);
    assert.equal(await tokenStore.getConnectionGeneration("uid"), 1);
  });

  it("does not persist undefined optional token fields", async () => {
    const stateStore = new MemoryOAuthStateStore();
    const tokenStore = new MemoryTokenStore();

    await stateStore.saveState({
      state: "minimal-token",
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

    await completeRavelryOAuthCallback({
      query: { state: "minimal-token", code: "auth-code" },
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
      state: "race",
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
      completeRavelryOAuthCallback({
        query: { state: "race", code: "auth-code" },
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
      state: "expires-before-consume",
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

    const result = await completeRavelryOAuthCallback({
      query: { state: "expires-before-consume", code: "auth-code" },
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
      "knittools://ravelry-auth-complete?state=expires-before-consume&error=state_expired",
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
