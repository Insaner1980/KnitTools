import assert from "node:assert/strict";
import { describe, it } from "node:test";

import {
  RAVELRY_OAUTH_STATES_COLLECTION,
  RAVELRY_TOKENS_COLLECTION,
  firebaseRegion,
  ravelryClientId,
  ravelryClientSecret,
} from "../config";
import {
  ravelryAuthStatus,
  ravelryCallback,
  ravelryCurrentUser,
  ravelryDisconnect,
  ravelryStartAuth,
} from "./auth";
import { createRavelryClient } from "./client";
import {
  importPatternById,
  importPatternByUrl,
  ravelryImportPatternById,
  ravelryImportPatternByUrl,
  ravelrySearchPatterns,
  searchPatternsForUser,
} from "./patternImport";
import { toSanitizedPattern } from "./sanitizedTypes";
import { createTokenStore } from "./tokenStore";
import { parseRavelryPatternUrl } from "./urlParsing";

describe("Ravelry Firebase backend surface", () => {
  it("exports the expected region, secret bindings, collections, functions, and modules", () => {
    assert.equal(firebaseRegion, "europe-west1");
    assert.equal(RAVELRY_OAUTH_STATES_COLLECTION, "ravelryOAuthStates");
    assert.equal(RAVELRY_TOKENS_COLLECTION, "ravelryTokens");
    assert.equal(ravelryClientId.name, "RAVELRY_CLIENT_ID");
    assert.equal(ravelryClientSecret.name, "RAVELRY_CLIENT_SECRET");

    [
      ravelryStartAuth,
      ravelryCallback,
      ravelryAuthStatus,
      ravelryDisconnect,
      ravelryCurrentUser,
      ravelrySearchPatterns,
      ravelryImportPatternById,
      ravelryImportPatternByUrl,
      searchPatternsForUser,
      createRavelryClient,
      createTokenStore,
      parseRavelryPatternUrl,
      importPatternByUrl,
      importPatternById,
      toSanitizedPattern,
    ].forEach((exportedValue) => assert.equal(typeof exportedValue, "function"));
  });
});
