import type { Firestore } from "firebase-admin/firestore";

import { RAVELRY_OAUTH_STATES_COLLECTION } from "../config";

export interface StoredOAuthState {
  readonly state: string;
  readonly uid: string;
  readonly authType: "oauth2";
  readonly createdAtMillis: number;
  readonly expiresAtMillis: number;
  readonly usedAtMillis: number | null;
  readonly redirectUri: string;
  readonly codeVerifier: string;
  readonly codeChallenge: string;
  readonly codeChallengeMethod: "S256";
}

export interface OAuthStateStore {
  saveState(state: StoredOAuthState): Promise<void>;
  getState(state: string): Promise<StoredOAuthState | null>;
  markStateUsed(state: string, usedAtMillis: number): Promise<boolean>;
  expireUnusedStatesForUid(uid: string, expiresAtMillis: number): Promise<void>;
}

function toStoredOAuthState(value: FirebaseFirestore.DocumentData | undefined): StoredOAuthState | null {
  if (!value || typeof value.state !== "string" || typeof value.uid !== "string") {
    return null;
  }

  return {
    state: value.state,
    uid: value.uid,
    authType: "oauth2",
    createdAtMillis: Number(value.createdAtMillis),
    expiresAtMillis: Number(value.expiresAtMillis),
    usedAtMillis: value.usedAtMillis == null ? null : Number(value.usedAtMillis),
    redirectUri: String(value.redirectUri),
    codeVerifier: String(value.codeVerifier),
    codeChallenge: String(value.codeChallenge),
    codeChallengeMethod: "S256",
  };
}

export function createOAuthStateStore(firestore: Firestore): OAuthStateStore {
  const collection = firestore.collection(RAVELRY_OAUTH_STATES_COLLECTION);

  return {
    async saveState(state) {
      await collection.doc(state.state).set(state);
    },
    async getState(state) {
      const snapshot = await collection.doc(state).get();
      return toStoredOAuthState(snapshot.data());
    },
    async markStateUsed(state, usedAtMillis) {
      const reference = collection.doc(state);
      let marked = false;
      await firestore.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(reference);
        const storedState = toStoredOAuthState(snapshot.data());
        if (!storedState || storedState.usedAtMillis != null) {
          return;
        }
        transaction.update(reference, { usedAtMillis });
        marked = true;
      });
      return marked;
    },
    async expireUnusedStatesForUid(uid, expiresAtMillis) {
      const snapshot = await collection.where("uid", "==", uid).where("usedAtMillis", "==", null).get();
      const batch = firestore.batch();
      snapshot.docs.forEach((doc) => {
        batch.update(doc.ref, { expiresAtMillis });
      });
      await batch.commit();
    },
  };
}
