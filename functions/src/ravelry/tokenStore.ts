import type { Firestore } from "firebase-admin/firestore";

import { RAVELRY_TOKENS_COLLECTION } from "../config";

export interface StoredRavelryToken {
  readonly uid: string;
  readonly authType: "oauth2";
  readonly accessToken: string;
  readonly refreshToken?: string;
  readonly expiresAtMillis?: number;
  readonly ravelryUserId?: string;
  readonly ravelryUsername?: string;
  readonly createdAtMillis: number;
  readonly updatedAtMillis: number;
  readonly lastVerifiedAtMillis?: number;
}

export interface RavelryTokenStore {
  readonly collectionPath: string;
  getToken(uid: string): Promise<StoredRavelryToken | null>;
  saveToken(token: StoredRavelryToken): Promise<void>;
  deleteToken(uid: string): Promise<void>;
}

function toStoredToken(value: FirebaseFirestore.DocumentData | undefined): StoredRavelryToken | null {
  if (!value || typeof value.uid !== "string" || typeof value.accessToken !== "string") {
    return null;
  }

  return {
    uid: value.uid,
    authType: "oauth2",
    accessToken: value.accessToken,
    refreshToken: typeof value.refreshToken === "string" ? value.refreshToken : undefined,
    expiresAtMillis: value.expiresAtMillis == null ? undefined : Number(value.expiresAtMillis),
    ravelryUserId: typeof value.ravelryUserId === "string" ? value.ravelryUserId : undefined,
    ravelryUsername: typeof value.ravelryUsername === "string" ? value.ravelryUsername : undefined,
    createdAtMillis: Number(value.createdAtMillis),
    updatedAtMillis: Number(value.updatedAtMillis),
    lastVerifiedAtMillis: value.lastVerifiedAtMillis == null ? undefined : Number(value.lastVerifiedAtMillis),
  };
}

function withoutUndefinedValues(token: StoredRavelryToken): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(token).filter(([, value]) => value !== undefined),
  );
}

export function createTokenStore(firestore: Firestore): RavelryTokenStore {
  const collection = firestore.collection(RAVELRY_TOKENS_COLLECTION);

  return {
    collectionPath: RAVELRY_TOKENS_COLLECTION,
    async getToken(uid) {
      const snapshot = await collection.doc(uid).get();
      return toStoredToken(snapshot.data());
    },
    async saveToken(token) {
      await collection.doc(token.uid).set(withoutUndefinedValues(token));
    },
    async deleteToken(uid) {
      await collection.doc(uid).delete();
    },
  };
}
