import assert from "node:assert/strict";
import { after, describe, it } from "node:test";

import { Firestore } from "firebase-admin/firestore";

import {
  completeRavelryOAuth,
  completeRavelryOAuthCallback,
  disconnectRavelry,
  startRavelryOAuth,
} from "./authCore";
import { createOAuthStateStore } from "./oauthStateStore";
import { getUsableRavelryToken } from "./tokenAccess";
import { createTokenStore } from "./tokenStore";

const emulatorHost = process.env.FIRESTORE_EMULATOR_HOST;

describe("Pending auth during refresh in local Firestore", {
  skip: emulatorHost ? false : "Requires an explicitly started local Firestore emulator",
}, () => {
  let firestore: Firestore;
  let sequence = 0;
  after(async () => { await firestore?.terminate(); });

  async function fixture() {
    assert.match(emulatorHost ?? "", /^(127\.0\.0\.1|localhost):\d+$/);
    firestore ??= new Firestore({ projectId: "demo-knittools-ravelry-regression" });
    const uid = `pending-refresh-${++sequence}`;
    const tokenStore = createTokenStore(firestore);
    const stateStore = createOAuthStateStore(firestore);
    const ref = firestore.collection(tokenStore.collectionPath).doc(uid);
    const now = 1_000_000;
    const options = { uid, tokenStore, stateStore, nowMillis: () => now };
    await tokenStore.saveToken({
      uid, authType: "oauth2", accessToken: "synthetic-old-access",
      refreshToken: "synthetic-old-refresh", expiresAtMillis: now,
      createdAtMillis: 1, updatedAtMillis: 1, connectionGeneration: 3,
    });

    async function pending(label = "new") {
      const started = await startRavelryOAuth({
        ...options, clientId: "synthetic-client", backendCallbackUrl: "https://callback.example",
      });
      const callback = await completeRavelryOAuthCallback({
        ...options, query: { state: started.state, code: "synthetic-code" },
        rateLimiter: { consume: async () => {}, consumeUid: async () => {}, consumeGlobal: async () => {} },
        exchange: async () => ({
          accessToken: `synthetic-${label}-access`, refreshToken: `synthetic-${label}-refresh`,
          expiresAtMillis: now + 3_600_000,
        }),
      });
      const url = new URL(callback.redirectUrl);
      return {
        ...options, state: started.state, completionProof: url.searchParams.get("proof") ?? "",
      };
    }

    async function refresh(during: () => Promise<void> = async () => {}) {
      return getUsableRavelryToken({
        ...options,
        refresh: async (request) => {
          assert.equal(request.refreshToken, "synthetic-old-refresh");
          await during();
          return { accessToken: "synthetic-rotated-access", refreshToken: "synthetic-rotated-refresh" };
        },
      });
    }
    return { ...options, ref, now, pending, refresh };
  }

  it("preserves pending through refresh, rotates credentials, then activates proof once", async () => {
    const f = await fixture();
    const proof = await f.pending();
    const before = (await f.ref.get()).data()?.pending;
    await f.ref.update({ obsoleteField: "synthetic-obsolete" });
    assert.equal((await f.refresh())?.refreshToken, "synthetic-rotated-refresh");
    const afterRefresh = (await f.ref.get()).data();
    assert.deepEqual(afterRefresh?.pending, before);
    assert.equal(afterRefresh?.expiresAtMillis, undefined);
    assert.equal(afterRefresh?.obsoleteField, undefined);
    assert.deepEqual(await completeRavelryOAuth(proof), { connected: true });
    assert.equal((await f.ref.get()).data()?.pending, undefined);
    assert.equal((await f.tokenStore.getToken(f.uid))?.accessToken, "synthetic-new-access");
    await assert.rejects(completeRavelryOAuth(proof), { code: "invalid_completion" });
  });

  it("rejects a delayed refresh after proof activation without restoring pending", async () => {
    const f = await fixture();
    const proof = await f.pending();
    const refreshed = await f.refresh(async () => { await completeRavelryOAuth(proof); });
    assert.equal(refreshed?.accessToken, "synthetic-new-access");
    const data = (await f.ref.get()).data();
    assert.equal(data?.refreshToken, "synthetic-new-refresh");
    assert.equal(data?.pending, undefined);
  });

  it("keeps the disconnect tombstone when a refresh finishes late", async () => {
    const f = await fixture();
    const proof = await f.pending();
    assert.equal(await f.refresh(async () => { await disconnectRavelry(f); }), null);
    assert.deepEqual((await f.ref.get()).data(), {
      uid: f.uid, connectionGeneration: 4, disconnectedAtMillis: f.now, updatedAtMillis: f.now,
    });
    await assert.rejects(completeRavelryOAuth(proof), { code: "invalid_completion" });
  });

  it("rejects activation when disconnect commits during the completion call", async () => {
    const f = await fixture();
    const proof = await f.pending();
    await assert.rejects(completeRavelryOAuth({
      ...proof,
      tokenStore: {
        ...f.tokenStore,
        activatePendingToken: async (...args) => {
          await disconnectRavelry(f);
          return f.tokenStore.activatePendingToken(...args);
        },
      },
    }), { code: "invalid_completion" });
    assert.equal(await f.tokenStore.getToken(f.uid), null);
    assert.equal(await f.tokenStore.getConnectionGeneration(f.uid), 4);
  });

  it("serializes concurrent activation and disconnect to a disconnected final state", async () => {
    const f = await fixture();
    const proof = await f.pending();
    const [activation, disconnect] = await Promise.allSettled([
      completeRavelryOAuth(proof), disconnectRavelry(f),
    ]);
    assert.equal(disconnect.status, "fulfilled");
    if (activation.status === "rejected") assert.equal(activation.reason.code, "invalid_completion");
    assert.equal(await f.tokenStore.getToken(f.uid), null);
    assert.equal(await f.tokenStore.getConnectionGeneration(f.uid), 4);
    assert.equal((await f.ref.get()).data()?.pending, undefined);
    await assert.rejects(completeRavelryOAuth(proof), { code: "invalid_completion" });
  });

  it("preserves the newest pending when another login replaces it during refresh", async () => {
    const f = await fixture();
    const oldProof = await f.pending("first");
    let newProof = oldProof;
    await f.refresh(async () => { newProof = await f.pending("second"); });
    assert.equal((await f.ref.get()).data()?.pending.state, newProof.state);
    await assert.rejects(completeRavelryOAuth(oldProof), { code: "invalid_completion" });
    await completeRavelryOAuth(newProof);
    assert.equal((await f.tokenStore.getToken(f.uid))?.accessToken, "synthetic-second-access");
  });

  it("continues rejecting invalid uid, state, proof and expired proof after refresh", async () => {
    const f = await fixture();
    const proof = await f.pending();
    await f.refresh();
    for (const override of [
      { uid: "synthetic-other-uid" }, { state: "z".repeat(43) },
      { completionProof: "z".repeat(43) }, { completionProof: "invalid" },
      { nowMillis: () => f.now + 600_000 },
    ]) {
      await assert.rejects(completeRavelryOAuth({ ...proof, ...override }), { code: "invalid_completion" });
    }
    assert.equal((await f.ref.get()).data()?.pending.state, proof.state);
    await completeRavelryOAuth({ ...proof, nowMillis: () => f.now + 599_999 });
  });

  it("does not carry a pending token from another generation into refreshed state", async () => {
    const f = await fixture();
    const proof = await f.pending();
    await f.ref.update({ "pending.token.connectionGeneration": 2 });
    await assert.rejects(completeRavelryOAuth(proof), { code: "invalid_completion" });
    await f.refresh();
    assert.equal((await f.ref.get()).data()?.pending, undefined);
    await assert.rejects(completeRavelryOAuth(proof), { code: "invalid_completion" });
  });
});
