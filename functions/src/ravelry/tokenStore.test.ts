import assert from "node:assert/strict";
import { describe, it } from "node:test";

import type { Firestore } from "firebase-admin/firestore";

import { createTokenStore } from "./tokenStore";

describe("Ravelry token Firestore store", () => {
  it("keeps callback credentials pending until the matching uid and proof activate them", async () => {
    let stored: Record<string, unknown> = {
      uid: "uid",
      connectionGeneration: 3,
      disconnectedAtMillis: 100,
    };
    const reference = {};
    const firestore = {
      collection() {
        return {
          doc() {
            return reference;
          },
        };
      },
      async runTransaction<T>(operation: (transaction: unknown) => Promise<T>): Promise<T> {
        return operation({
          async get() {
            return { data: () => stored };
          },
          set(
            _reference: unknown,
            data: Record<string, unknown>,
            options?: { readonly merge?: boolean },
          ) {
            stored = options?.merge ? { ...stored, ...data } : data;
          },
        });
      },
    } as unknown as Firestore;
    const tokenStore = createTokenStore(firestore);

    const saved = await tokenStore.savePendingTokenIfGenerationCurrent(
      {
        token: {
          uid: "uid",
          authType: "oauth2",
          accessToken: "new-access-token",
          createdAtMillis: 1_000,
          updatedAtMillis: 1_000,
          connectionGeneration: 3,
        },
        state: "state",
        completionProofHash: "proof-hash",
        expiresAtMillis: 2_000,
      },
      3,
    );

    assert.equal(saved, true);
    assert.equal(stored.accessToken, undefined);
    assert.equal(await tokenStore.activatePendingToken("other-uid", "state", "proof-hash", 1_500), false);
    assert.equal(await tokenStore.activatePendingToken("uid", "state", "wrong-proof", 1_500), false);
    assert.equal(stored.accessToken, undefined);
    assert.equal(await tokenStore.activatePendingToken("uid", "state", "proof-hash", 1_500), true);
    assert.equal(stored.accessToken, "new-access-token");
    assert.equal(stored.pending, undefined);
    assert.equal(await tokenStore.activatePendingToken("uid", "state", "proof-hash", 1_500), false);
  });

  it("updates only current-user metadata fields after validating generation", async () => {
    const updates: Record<string, unknown>[] = [];
    const reference = {};
    const current = {
      uid: "uid",
      authType: "oauth2",
      accessToken: "access-token",
      refreshToken: "refresh-token",
      expiresAtMillis: 2_000,
      createdAtMillis: 100,
      updatedAtMillis: 100,
      connectionGeneration: 3,
    };
    const firestore = {
      collection() {
        return {
          doc() {
            return reference;
          },
        };
      },
      async runTransaction<T>(operation: (transaction: unknown) => Promise<T>): Promise<T> {
        return operation({
          async get() {
            return { data: () => current };
          },
          update(_reference: unknown, data: Record<string, unknown>) {
            updates.push(data);
          },
        });
      },
    } as unknown as Firestore;

    const saved = await createTokenStore(firestore).updateUserMetadataIfGenerationCurrent(
      "uid",
      {
        ravelryUserId: "42",
        ravelryUsername: "ada",
        verifiedAtMillis: 1_500,
      },
      3,
    );

    assert.equal(saved, true);
    assert.equal(updates.length, 1);
    assert.deepEqual(Object.keys(updates[0] ?? {}).sort(), [
      "lastVerifiedAtMillis",
      "ravelryUserId",
      "ravelryUsername",
      "updatedAtMillis",
    ]);
  });
});
