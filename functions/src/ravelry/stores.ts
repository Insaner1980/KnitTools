import { getFirestore } from "firebase-admin/firestore";

import { createOAuthStateStore } from "./oauthStateStore";
import { createRavelryRateLimiter } from "./rateLimit";
import { createTokenStore } from "./tokenStore";

export function createRavelryBackendStores() {
  const firestore = getFirestore();
  return {
    stateStore: createOAuthStateStore(firestore),
    tokenStore: createTokenStore(firestore),
    rateLimiter: createRavelryRateLimiter(firestore),
  };
}
