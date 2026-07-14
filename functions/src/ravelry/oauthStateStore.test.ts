import assert from "node:assert/strict";
import { describe, it } from "node:test";

import type { Firestore } from "firebase-admin/firestore";

import { RAVELRY_OAUTH_STATES_COLLECTION } from "../config";
import {
  createOAuthStateStore,
  type StoredOAuthState,
} from "./oauthStateStore";

class FakeDocumentReference {
  constructor(readonly id: string) {}
}

class FakeDocumentSnapshot {
  constructor(private readonly value: StoredOAuthState | undefined) {}

  data(): StoredOAuthState | undefined {
    return this.value;
  }
}

class FakeTransaction {
  constructor(
    private readonly state: StoredOAuthState | undefined,
    private readonly updates: Array<{ readonly state: string; readonly usedAtMillis: number }>,
  ) {}

  async get(_reference: FakeDocumentReference): Promise<FakeDocumentSnapshot> {
    return new FakeDocumentSnapshot(this.state);
  }

  update(reference: FakeDocumentReference, data: Partial<StoredOAuthState>): void {
    if (typeof data.usedAtMillis === "number") {
      this.updates.push({ state: reference.id, usedAtMillis: data.usedAtMillis });
    }
  }
}

class FakeFirestore {
  readonly updates: Array<{ readonly state: string; readonly usedAtMillis: number }> = [];

  constructor(private readonly attemptStates: readonly (StoredOAuthState | undefined)[]) {}

  collection(path: string): { doc(state: string): FakeDocumentReference } {
    assert.equal(path, RAVELRY_OAUTH_STATES_COLLECTION);
    return {
      doc: (state) => new FakeDocumentReference(state),
    };
  }

  async runTransaction<T>(updateFunction: (transaction: FakeTransaction) => Promise<T>): Promise<T> {
    assert.notEqual(this.attemptStates.length, 0);
    let result: T | undefined;
    for (const state of this.attemptStates) {
      result = await updateFunction(new FakeTransaction(state, this.updates));
    }
    return result as T;
  }
}

function storedState(overrides: Partial<StoredOAuthState> = {}): StoredOAuthState {
  return {
    state: "state",
    uid: "uid",
    authType: "oauth2",
    createdAtMillis: 0,
    expiresAtMillis: 2_000,
    usedAtMillis: null,
    redirectUri: "https://callback",
    codeVerifier: "verifier",
    codeChallenge: "challenge",
    codeChallengeMethod: "S256",
    ...overrides,
  };
}

describe("OAuth state Firestore store", () => {
  it("returns false when a transaction retry observes the state was already used", async () => {
    const firestore = new FakeFirestore([
      storedState({ state: "retry-race", usedAtMillis: null }),
      storedState({ state: "retry-race", usedAtMillis: 1_001 }),
    ]);
    const store = createOAuthStateStore(firestore as unknown as Firestore);

    const marked = await store.markStateUsed("retry-race", 1_000);

    assert.equal(marked, false);
  });

  it("returns true and writes usedAtMillis for an unused unexpired state", async () => {
    const firestore = new FakeFirestore([
      storedState({ state: "valid", usedAtMillis: null }),
    ]);
    const store = createOAuthStateStore(firestore as unknown as Firestore);

    const marked = await store.markStateUsed("valid", 1_000);

    assert.equal(marked, true);
    assert.deepEqual(firestore.updates, [{ state: "valid", usedAtMillis: 1_000 }]);
  });
});
