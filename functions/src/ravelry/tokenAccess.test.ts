import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { getUsableRavelryToken } from "./tokenAccess";
import type { RavelryTokenStore, StoredRavelryToken } from "./tokenStore";

class MemoryTokenStore implements RavelryTokenStore {
  readonly collectionPath = "ravelryTokens";
  private token: StoredRavelryToken | null = null;
  private connectionGeneration = 0;

  async getToken(uid: string): Promise<StoredRavelryToken | null> {
    return this.token?.uid === uid ? this.token : null;
  }

  async getConnectionGeneration(): Promise<number> {
    return this.connectionGeneration;
  }

  async saveToken(token: StoredRavelryToken): Promise<void> {
    this.connectionGeneration = token.connectionGeneration ?? this.connectionGeneration;
    this.token = { ...token, connectionGeneration: this.connectionGeneration };
  }

  async saveTokenIfGenerationCurrent(
    token: StoredRavelryToken,
    expectedGeneration: number,
  ): Promise<boolean> {
    if (this.connectionGeneration !== expectedGeneration) {
      return false;
    }
    this.token = { ...token, connectionGeneration: expectedGeneration };
    return true;
  }

  async saveRefreshedTokenIfCurrent(
    token: StoredRavelryToken,
    expectedToken: StoredRavelryToken,
  ): Promise<StoredRavelryToken | null> {
    if (!this.token ||
      this.token.accessToken !== expectedToken.accessToken ||
      this.token.refreshToken !== expectedToken.refreshToken ||
      this.token.expiresAtMillis !== expectedToken.expiresAtMillis ||
      (this.token.connectionGeneration ?? 0) !== (expectedToken.connectionGeneration ?? 0)) {
      return null;
    }
    this.token = { ...token, connectionGeneration: this.connectionGeneration };
    return this.token;
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
    if (!this.token || this.token.uid !== uid || this.connectionGeneration !== expectedGeneration) {
      return false;
    }
    this.token = {
      ...this.token,
      ravelryUserId: update.ravelryUserId,
      ravelryUsername: update.ravelryUsername,
      updatedAtMillis: update.verifiedAtMillis,
      lastVerifiedAtMillis: update.verifiedAtMillis,
    };
    return true;
  }

  async deleteToken(): Promise<void> {
    this.connectionGeneration += 1;
    this.token = null;
  }
}

describe("Ravelry token access", () => {
  it("keeps the first persisted rotation when concurrent refreshes finish out of order", async () => {
    const tokenStore = new MemoryTokenStore();
    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "expired-access-token",
      refreshToken: "old-refresh-token",
      expiresAtMillis: 999,
      createdAtMillis: 100,
      updatedAtMillis: 100,
    });

    let releaseFirstRefresh: (() => void) | undefined;
    const firstRefreshGate = new Promise<void>((resolve) => {
      releaseFirstRefresh = resolve;
    });
    let firstRefreshStarted: (() => void) | undefined;
    const firstRefreshStart = new Promise<void>((resolve) => {
      firstRefreshStarted = resolve;
    });
    let refreshCalls = 0;
    const refresh = async () => {
      refreshCalls += 1;
      if (refreshCalls === 1) {
        firstRefreshStarted?.();
        await firstRefreshGate;
        return {
          accessToken: "late-access-token",
          refreshToken: "late-refresh-token",
          expiresAtMillis: 20_000,
        };
      }
      return {
        accessToken: "persisted-access-token",
        refreshToken: "persisted-refresh-token",
        expiresAtMillis: 10_000,
      };
    };

    const lateRefresh = getUsableRavelryToken({
      uid: "uid",
      tokenStore,
      refresh,
      nowMillis: () => 1_000,
    });
    await firstRefreshStart;
    const persistedRefresh = await getUsableRavelryToken({
      uid: "uid",
      tokenStore,
      refresh,
      nowMillis: () => 1_000,
    });
    releaseFirstRefresh?.();
    const lateResult = await lateRefresh;

    assert.equal(persistedRefresh?.accessToken, "persisted-access-token");
    assert.equal(lateResult?.accessToken, "persisted-access-token");
    assert.equal((await tokenStore.getToken("uid"))?.refreshToken, "persisted-refresh-token");
  });

  it("rejects a concurrently persisted token that expires before the losing refresh reloads it", async () => {
    const tokenStore = new MemoryTokenStore();
    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "expired-access-token",
      refreshToken: "old-refresh-token",
      expiresAtMillis: 999,
      createdAtMillis: 100,
      updatedAtMillis: 100,
    });

    let now = 1_000;
    let releaseFirstRefresh: (() => void) | undefined;
    const firstRefreshGate = new Promise<void>((resolve) => {
      releaseFirstRefresh = resolve;
    });
    let firstRefreshStarted: (() => void) | undefined;
    const firstRefreshStart = new Promise<void>((resolve) => {
      firstRefreshStarted = resolve;
    });
    let refreshCalls = 0;
    const refresh = async () => {
      refreshCalls += 1;
      if (refreshCalls === 1) {
        firstRefreshStarted?.();
        await firstRefreshGate;
        return {
          accessToken: "late-access-token",
          refreshToken: "late-refresh-token",
          expiresAtMillis: 20_000,
        };
      }
      return {
        accessToken: "brief-access-token",
        refreshToken: "brief-refresh-token",
        expiresAtMillis: 1_500,
      };
    };

    const lateRefresh = getUsableRavelryToken({
      uid: "uid",
      tokenStore,
      refresh,
      nowMillis: () => now,
    });
    await firstRefreshStart;
    await getUsableRavelryToken({
      uid: "uid",
      tokenStore,
      refresh,
      nowMillis: () => now,
    });
    now = 2_000;
    releaseFirstRefresh?.();

    assert.equal(await lateRefresh, null);
    assert.equal((await tokenStore.getToken("uid"))?.accessToken, "brief-access-token");
  });
});
