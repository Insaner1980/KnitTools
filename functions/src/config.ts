import { defineSecret, defineString } from "firebase-functions/params";

export const firebaseRegion = "europe-west1";

export const RAVELRY_OAUTH_STATES_COLLECTION = "ravelryOAuthStates";
export const RAVELRY_TOKENS_COLLECTION = "ravelryTokens";
export const RAVELRY_RATE_LIMITS_COLLECTION = "ravelryRateLimits";

export const ravelryClientId = defineSecret("RAVELRY_CLIENT_ID");
export const ravelryClientSecret = defineSecret("RAVELRY_CLIENT_SECRET");
export const ravelryCallbackUrl = defineString("RAVELRY_CALLBACK_URL", { default: "" });

export const RAVELRY_OAUTH_STATE_TTL_MILLIS = 10 * 60 * 1_000;
export const KNITTOOLS_RAVELRY_AUTH_COMPLETE_DEEP_LINK = "knittools://ravelry-auth-complete";
