import { FieldValue, type Firestore } from "firebase-admin/firestore";

import { RAVELRY_OAUTH_STATES_COLLECTION } from "../config";
import { connectionGenerationFromData } from "./connectionGeneration";

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
  readonly connectionGeneration?: number;
  readonly browserStartHash?: string;
  readonly browserBindingHash?: string;
  readonly browserResult?: string;
}

export interface OAuthStateStore {
  saveState(state: StoredOAuthState): Promise<void>;
  getState(state: string): Promise<StoredOAuthState | null>;
  markStateUsed(state: string, usedAtMillis: number): Promise<boolean>;
  expireUnusedStatesForUid(uid: string, expiresAtMillis: number): Promise<void>;
}

export interface BrowserOAuthStateStore extends OAuthStateStore {
  bindBrowser(state: string, startHash: string, bindingHash: string, now: number): Promise<StoredOAuthState | null>;
  saveBrowserResult(state: string, bindingHash: string, result: string, now: number): Promise<boolean>;
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
    connectionGeneration: connectionGenerationFromData(value),
    ...(typeof value.browserStartHash === "string" ? { browserStartHash: value.browserStartHash } : {}),
    ...(typeof value.browserBindingHash === "string" ? { browserBindingHash: value.browserBindingHash } : {}),
    ...(typeof value.browserResult === "string" ? { browserResult: value.browserResult } : {}),
  };
}

export function createOAuthStateStore(firestore: Firestore): BrowserOAuthStateStore {
  const collection = firestore.collection(RAVELRY_OAUTH_STATES_COLLECTION);

  return {
    async bindBrowser(state, startHash, bindingHash, now) {
      const reference = collection.doc(state);
      return firestore.runTransaction(async (transaction) => {
        const stored = toStoredOAuthState((await transaction.get(reference)).data());
        if (!stored || stored.usedAtMillis != null || stored.expiresAtMillis <= now ||
            stored.browserStartHash !== startHash || stored.browserBindingHash != null) return null;
        transaction.update(reference, { browserStartHash: FieldValue.delete(), browserBindingHash: bindingHash });
        const { browserStartHash: _startHash, ...bound } = stored;
        return { ...bound, browserBindingHash: bindingHash };
      });
    },
    async saveBrowserResult(state, bindingHash, result, now) {
      const reference = collection.doc(state);
      return firestore.runTransaction(async (transaction) => {
        const stored = toStoredOAuthState((await transaction.get(reference)).data());
        if (!stored || stored.usedAtMillis == null || stored.expiresAtMillis <= now ||
            stored.browserBindingHash !== bindingHash || stored.browserResult != null) return false;
        transaction.update(reference, { browserResult: result });
        return true;
      });
    },
    async saveState(state) {
      await collection.doc(state.state).set(state);
    },
    async getState(state) {
      const snapshot = await collection.doc(state).get();
      return toStoredOAuthState(snapshot.data());
    },
    async markStateUsed(state, usedAtMillis) {
      const reference = collection.doc(state);
      return firestore.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(reference);
        const storedState = toStoredOAuthState(snapshot.data());
        if (
          !storedState ||
          storedState.usedAtMillis != null ||
          storedState.expiresAtMillis <= usedAtMillis
        ) {
          return false;
        }
        transaction.update(reference, { usedAtMillis });
        return true;
      });
    },
    async expireUnusedStatesForUid(uid, expiresAtMillis) {
      const snapshot = await collection
        .where("uid", "==", uid)
        .where("usedAtMillis", "==", null)
        .where("expiresAtMillis", ">", expiresAtMillis)
        .get();
      if (snapshot.empty) return;
      const batch = firestore.batch();
      snapshot.docs.forEach((doc) => {
        batch.update(doc.ref, { expiresAtMillis });
      });
      await batch.commit();
    },
  };
}
