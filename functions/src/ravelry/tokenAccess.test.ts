import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { getUsableRavelryToken } from "./tokenAccess";
import type { PendingRavelryToken, RavelryTokenStore, StoredRavelryToken } from "./tokenStore";

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

  async savePendingTokenIfGenerationCurrent(
    _pending: PendingRavelryToken,
    _expectedGeneration: number,
  ): Promise<boolean> {
    return false;
  }

  async activatePendingToken(): Promise<boolean> {
    return false;
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
  it("uses the persisted rotation when a concurrent obsolete refresh is rejected", async () => {
    const tokenStore = new MemoryTokenStore();
    await tokenStore.saveToken({
      uid: "uid", authType: "oauth2", accessToken: "expired-access", refreshToken: "expired-refresh",
      expiresAtMillis: 999, createdAtMillis: 100, updatedAtMillis: 100,
    });
    let release: (() => void) | undefined;
    let started: (() => void) | undefined;
    const gate = new Promise<void>(resolve => { release = resolve; });
    const firstStarted = new Promise<void>(resolve => { started = resolve; });
    let calls = 0;
    const refresh = async () => {
      if (++calls === 1) {
        started?.();
        await gate;
        throw new Error("Synthetic obsolete refresh rejection");
      }
      return { accessToken: "current-access", refreshToken: "current-refresh", expiresAtMillis: 10_000 };
    };
    const losing = getUsableRavelryToken({ uid: "uid", tokenStore, refresh, nowMillis: () => 1_000 })
      .then(value => value, error => error);
    await firstStarted;
    const winner = await getUsableRavelryToken({ uid: "uid", tokenStore, refresh, nowMillis: () => 1_000 });
    release?.();
    assert.deepEqual(await losing, winner);
    assert.equal(winner?.accessToken, "current-access");
    assert.equal((await tokenStore.getToken("uid"))?.refreshToken, "current-refresh");
  });

  it("preserves refresh rejection without a usable changed token, including failed readback", async () => {
    for (const replacement of ["unchanged", "expired", "disconnected", "reconnected", "unavailable"]) {
      let reads = 0;
      const tokenStore = new class extends MemoryTokenStore {
        override async getToken(uid: string) {
          if (++reads > 1 && replacement === "unavailable") throw new Error("Synthetic readback failure");
          return super.getToken(uid);
        }
      }();
      const original: StoredRavelryToken = {
        uid: "uid", authType: "oauth2", accessToken: "expired-access", refreshToken: "expired-refresh",
        expiresAtMillis: 999, createdAtMillis: 100, updatedAtMillis: 100,
      };
      await tokenStore.saveToken(original);
      const rejection = new Error("Synthetic refresh rejection");
      await assert.rejects(getUsableRavelryToken({
        uid: "uid", tokenStore, nowMillis: () => 1_000,
        refresh: async () => {
          if (replacement === "expired") await tokenStore.saveToken({ ...original, accessToken: "new-but-expired" });
          if (replacement === "disconnected") await tokenStore.deleteToken();
          if (replacement === "reconnected") {
            await tokenStore.deleteToken();
            await tokenStore.saveToken({
              ...original, accessToken: "new-connection", expiresAtMillis: 10_000, connectionGeneration: 1,
            });
          }
          throw rejection;
        },
      }), error => error === rejection, replacement);
    }
  });

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
