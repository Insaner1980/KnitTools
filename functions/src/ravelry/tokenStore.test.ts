import assert from "node:assert/strict";
import { describe, it } from "node:test";

import type { Firestore } from "firebase-admin/firestore";

import { createTokenStore } from "./tokenStore";

describe("Ravelry token Firestore store", () => {
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
