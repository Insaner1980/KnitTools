import assert from "node:assert/strict";
import { after, describe, it, mock } from "node:test";

import { Firestore, Query } from "firebase-admin/firestore";

import { RAVELRY_OAUTH_STATES_COLLECTION, RAVELRY_TOKENS_COLLECTION } from "../config";
import { disconnectRavelry } from "./authCore";
import { createOAuthStateStore, type StoredOAuthState } from "./oauthStateStore";
import { createTokenStore, type StoredRavelryToken } from "./tokenStore";

const emulatorHost = process.env.FIRESTORE_EMULATOR_HOST;

describe("OAuth state invalidation in local Firestore", {
  skip: emulatorHost ? false : "Requires an explicitly started local Firestore emulator",
}, () => {
  let firestore: Firestore;
  after(async () => { await firestore?.terminate(); });

  it("reads and expires only this user's live unused states, including repeated disconnect", async () => {
    assert.match(emulatorHost ?? "", /^(127\.0\.0\.1|localhost):\d+$/);
    firestore = new Firestore({ projectId: "demo-knittools-ravelry-regression" });
    const stateStore = createOAuthStateStore(firestore);
    const tokenStore = createTokenStore(firestore);
    const now = 10_000;
    const uid = "disconnect-owner";
    const fixtures: StoredOAuthState[] = [
      { state: "live-boundary", uid, expiresAtMillis: now + 1, usedAtMillis: null },
      { state: "live-later", uid, expiresAtMillis: now + 60_000, usedAtMillis: null },
      { state: "expired", uid, expiresAtMillis: now - 1, usedAtMillis: null },
      { state: "expired-boundary", uid, expiresAtMillis: now, usedAtMillis: null },
      { state: "used", uid, expiresAtMillis: now + 1, usedAtMillis: now - 1 },
      { state: "used-expired", uid, expiresAtMillis: now - 1, usedAtMillis: now - 2 },
      { state: "other-user", uid: "other-owner", expiresAtMillis: now + 1, usedAtMillis: null },
    ].map((value) => ({
      ...value, authType: "oauth2", createdAtMillis: 0,
      redirectUri: "https://callback.example", codeVerifier: "synthetic-verifier",
      codeChallenge: "synthetic-challenge", codeChallengeMethod: "S256", connectionGeneration: 7,
    }));
    const token: StoredRavelryToken = {
      uid, authType: "oauth2", accessToken: "synthetic-access", refreshToken: "synthetic-refresh",
      expiresAtMillis: now + 1, createdAtMillis: 0, updatedAtMillis: 0, connectionGeneration: 7,
    };
    const states = firestore.collection(RAVELRY_OAUTH_STATES_COLLECTION);
    const tokenRef = firestore.collection(RAVELRY_TOKENS_COLLECTION).doc(uid);
    try {
      await Promise.all(fixtures.map((value) => stateStore.saveState(value)));
      await tokenStore.saveToken(token);
      const before = await Promise.all(fixtures.map((value) => states.doc(value.state).get()));
      const reads: string[][] = [];
      const originalGet = Query.prototype.get;
      mock.method(Query.prototype, "get", async function(this: Query) {
        const snapshot = await originalGet.call(this);
        reads.push(snapshot.docs.map((doc) => doc.id).sort());
        return snapshot;
      });

      assert.deepEqual(await disconnectRavelry({ uid, tokenStore, stateStore, nowMillis: () => now }),
        { disconnected: true });
      assert.deepEqual(reads, [["live-boundary", "live-later"]]);
      const first = await Promise.all(fixtures.map((value) => states.doc(value.state).get()));
      for (let i = 0; i < fixtures.length; i++) {
        const expected = i < 2 ? { ...fixtures[i], expiresAtMillis: now } : fixtures[i];
        assert.deepEqual(first[i].data(), expected);
        if (i >= 2) assert.ok(first[i].updateTime?.isEqual(before[i].updateTime!));
      }
      assert.deepEqual((await tokenRef.get()).data(), {
        uid, connectionGeneration: 8, disconnectedAtMillis: now, updatedAtMillis: now,
      });
      assert.equal(await tokenStore.getToken(uid), null);

      for (const repeatedAt of [now, now + 1]) {
        await disconnectRavelry({ uid, tokenStore, stateStore, nowMillis: () => repeatedAt });
        assert.deepEqual(reads.at(-1), []);
        const repeated = await Promise.all(fixtures.map((value) => states.doc(value.state).get()));
        for (let i = 0; i < fixtures.length; i++) {
          assert.deepEqual(repeated[i].data(), first[i].data());
          assert.ok(repeated[i].updateTime?.isEqual(first[i].updateTime!));
        }
      }
      assert.equal(await tokenStore.getConnectionGeneration(uid), 10);
      assert.equal(await tokenStore.savePendingTokenIfGenerationCurrent({
        token, state: "live-boundary", completionProofHash: "synthetic-proof", expiresAtMillis: now + 60_000,
      }, 7), false);
      assert.equal(await tokenStore.saveRefreshedTokenIfCurrent({ ...token, accessToken: "synthetic-new" }, token), null);
      assert.equal(await tokenStore.saveTokenIfGenerationCurrent(token, 7), false);
      assert.equal(await tokenStore.getToken(uid), null);
      assert.equal((await tokenRef.get()).data()?.pending, undefined);
      assert.equal(await stateStore.markStateUsed("live-boundary", now), false);
      assert.equal(await stateStore.markStateUsed("expired-boundary", now), false);
      assert.equal(await stateStore.markStateUsed("other-user", now), true);
    } finally {
      mock.restoreAll();
      await Promise.all(fixtures.map((value) => states.doc(value.state).delete()));
      await tokenRef.delete();
    }
  });
});
