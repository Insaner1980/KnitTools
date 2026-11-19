import { getFirestore } from "firebase-admin/firestore";

import { createOAuthStateStore } from "./oauthStateStore";
import { createRavelryRateLimiter } from "./rateLimit";
import { createTokenStore } from "./tokenStore";

export function createRavelryBackendStores() {
  const firestore = getFirestore();
  return {
    stateStore: createOAuthStateStore(firestore),
    tokenStore: createTokenStore(firestore),
    rateLimiter: createRavelryRateLimiter(
      firestore, undefined, undefined, undefined,
      process.env.RAVELRY_GLOBAL_RATE_LIMIT_MODE === "sharded" ? "sharded" : "shared",
    ),
  };
}
