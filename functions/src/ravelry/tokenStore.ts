import { FieldValue, type Firestore } from "firebase-admin/firestore";

import { RAVELRY_TOKENS_COLLECTION } from "../config";
import { connectionGenerationFromData } from "./connectionGeneration";

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
  readonly connectionGeneration?: number;
}

export interface RavelryTokenUserMetadataUpdate {
  readonly ravelryUserId?: string;
  readonly ravelryUsername?: string;
  readonly verifiedAtMillis: number;
}

export interface RavelryTokenStore {
  readonly collectionPath: string;
  getToken(uid: string): Promise<StoredRavelryToken | null>;
  getConnectionGeneration(uid: string): Promise<number>;
  saveToken(token: StoredRavelryToken): Promise<void>;
  saveTokenIfGenerationCurrent(
    token: StoredRavelryToken,
    expectedGeneration: number,
  ): Promise<boolean>;
  saveRefreshedTokenIfCurrent(
    token: StoredRavelryToken,
    expectedToken: StoredRavelryToken,
  ): Promise<StoredRavelryToken | null>;
  updateUserMetadataIfGenerationCurrent(
    uid: string,
    update: RavelryTokenUserMetadataUpdate,
    expectedGeneration: number,
  ): Promise<boolean>;
  deleteToken(uid: string, nowMillis?: number): Promise<void>;
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
    connectionGeneration: connectionGenerationFromData(value),
  };
}

function withoutUndefinedValues(token: StoredRavelryToken): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries({
      ...token,
      connectionGeneration: token.connectionGeneration ?? 0,
    }).filter(([, value]) => value !== undefined),
  );
}

function hasSameCredentials(
  current: StoredRavelryToken | null,
  expected: StoredRavelryToken,
): current is StoredRavelryToken {
  return current != null &&
    current.uid === expected.uid &&
    current.accessToken === expected.accessToken &&
    current.refreshToken === expected.refreshToken &&
    current.expiresAtMillis === expected.expiresAtMillis &&
    (current.connectionGeneration ?? 0) === (expected.connectionGeneration ?? 0);
}

function disconnectedTokenMarker(
  uid: string,
  connectionGeneration: number,
  nowMillis?: number,
): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries({
      uid,
      connectionGeneration,
      disconnectedAtMillis: nowMillis,
      updatedAtMillis: nowMillis,
    }).filter(([, value]) => value !== undefined),
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
    async getConnectionGeneration(uid) {
      const snapshot = await collection.doc(uid).get();
      return connectionGenerationFromData(snapshot.data());
    },
    async saveToken(token) {
      const ref = collection.doc(token.uid);
      await firestore.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(ref);
        const connectionGeneration =
          token.connectionGeneration ?? connectionGenerationFromData(snapshot.data());
        transaction.set(ref, withoutUndefinedValues({ ...token, connectionGeneration }));
      });
    },
    async saveTokenIfGenerationCurrent(token, expectedGeneration) {
      const ref = collection.doc(token.uid);
      return firestore.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(ref);
        const currentGeneration = connectionGenerationFromData(snapshot.data());
        if (currentGeneration !== expectedGeneration) {
          return false;
        }

        transaction.set(
          ref,
          withoutUndefinedValues({ ...token, connectionGeneration: expectedGeneration }),
        );
        return true;
      });
    },
    async saveRefreshedTokenIfCurrent(token, expectedToken) {
      const ref = collection.doc(token.uid);
      return firestore.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(ref);
        const current = toStoredToken(snapshot.data());
        if (!hasSameCredentials(current, expectedToken)) {
          return null;
        }

        const persisted: StoredRavelryToken = {
          uid: current.uid,
          authType: current.authType,
          accessToken: token.accessToken,
          ...(token.refreshToken ? { refreshToken: token.refreshToken } : {}),
          ...(token.expiresAtMillis != null ? { expiresAtMillis: token.expiresAtMillis } : {}),
          ...(current.ravelryUserId ? { ravelryUserId: current.ravelryUserId } : {}),
          ...(current.ravelryUsername ? { ravelryUsername: current.ravelryUsername } : {}),
          createdAtMillis: current.createdAtMillis,
          updatedAtMillis: token.updatedAtMillis,
          ...(current.lastVerifiedAtMillis != null
            ? { lastVerifiedAtMillis: current.lastVerifiedAtMillis }
            : {}),
          connectionGeneration: current.connectionGeneration ?? 0,
        };
        transaction.set(ref, withoutUndefinedValues(persisted));
        return persisted;
      });
    },
    async updateUserMetadataIfGenerationCurrent(uid, update, expectedGeneration) {
      const ref = collection.doc(uid);
      return firestore.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(ref);
        const current = toStoredToken(snapshot.data());
        if (!current || (current.connectionGeneration ?? 0) !== expectedGeneration) {
          return false;
        }

        transaction.update(ref, {
          ravelryUserId: update.ravelryUserId ?? FieldValue.delete(),
          ravelryUsername: update.ravelryUsername ?? FieldValue.delete(),
          updatedAtMillis: update.verifiedAtMillis,
          lastVerifiedAtMillis: update.verifiedAtMillis,
        });
        return true;
      });
    },
    async deleteToken(uid, nowMillis) {
      const ref = collection.doc(uid);
      await firestore.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(ref);
        const nextGeneration = connectionGenerationFromData(snapshot.data()) + 1;
        transaction.set(ref, disconnectedTokenMarker(uid, nextGeneration, nowMillis));
      });
    },
  };
}
