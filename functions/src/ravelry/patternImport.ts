import { onCall } from "firebase-functions/v2/https";

import { ravelrySecretOptions } from "../config";
import { httpsErrorFor, requireUid } from "./callable";
import { createRavelryClient, type RavelryClient, type RavelrySearchQuery } from "./client";
import { refreshRavelryAccessToken } from "./oauthSecretRefresh";
import type { OAuthTokenRefresh } from "./oauth2";
import {
  disabledRavelryRateLimiter,
  type RavelryRateLimitBucket,
  type RavelryRateLimiter,
} from "./rateLimit";
import type { SanitizedPattern } from "./sanitizedTypes";
import { createRavelryBackendStores } from "./stores";
import { getUsableRavelryToken } from "./tokenAccess";
import type { RavelryTokenStore } from "./tokenStore";
import { parseRavelryPatternUrl } from "./urlParsing";

interface UserPatternOptions {
  readonly uid: string;
  readonly tokenStore: RavelryTokenStore;
  readonly client: RavelryClient;
  readonly rateLimiter?: RavelryRateLimiter;
  readonly refresh?: OAuthTokenRefresh;
  readonly nowMillis?: () => number;
}

interface SearchPatternsOptions extends UserPatternOptions {
  readonly query: RavelrySearchQuery;
}

interface ImportPatternByIdOptions extends UserPatternOptions {
  readonly ravelryPatternId: number;
}

interface ImportPatternByUrlOptions extends UserPatternOptions {
  readonly url: string;
}

export const RAVELRY_SEARCH_MAX_PAGE_SIZE = 50;

export class RavelryPatternImportError extends Error {
  constructor(
    readonly code: string,
    readonly httpStatus: number,
  ) {
    super(code);
  }
}

async function requireAccessToken({
  uid,
  tokenStore,
  refresh,
  beforeRefresh,
  nowMillis,
}: {
  readonly uid: string;
  readonly tokenStore: RavelryTokenStore;
  readonly refresh?: OAuthTokenRefresh;
  readonly beforeRefresh?: () => Promise<void>;
  readonly nowMillis?: () => number;
}): Promise<string> {
  const token = await getUsableRavelryToken({
    uid,
    tokenStore,
    refresh,
    beforeRefresh,
    nowMillis,
  });
  if (!token) {
    throw new RavelryPatternImportError("ravelry_not_connected", 412);
  }
  return token.accessToken;
}

async function requireAccessTokenForOperation(
  options: UserPatternOptions,
  bucket: RavelryRateLimitBucket,
): Promise<string> {
  let consumed = false;
  const consumeRateLimit = async () => {
    if (!consumed) {
      await rateLimiterFor(options).consume(options.uid, bucket);
      consumed = true;
    }
  };
  const accessToken = await requireAccessToken({
    ...options,
    beforeRefresh: consumeRateLimit,
  });
  await consumeRateLimit();
  return accessToken;
}

function withOriginalUrl(pattern: SanitizedPattern, originalUrl: string): SanitizedPattern {
  return {
    ...pattern,
    originalUrl,
  };
}

function positiveInteger(value: unknown): number | undefined {
  return typeof value === "number" && Number.isInteger(value) && value > 0 ? value : undefined;
}

function optionalBoundedPositiveInteger(
  value: unknown,
  maxValue: number,
  errorCode: string,
): number | undefined {
  if (value == null) {
    return undefined;
  }
  const integer = positiveInteger(value);
  if (integer == null || integer > maxValue) {
    throw new RavelryPatternImportError(errorCode, 400);
  }
  return integer;
}

function optionalNumber(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function optionalString(value: unknown): string | undefined {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : undefined;
}

function dataObject(data: unknown): Record<string, unknown> {
  return typeof data === "object" && data !== null ? (data as Record<string, unknown>) : {};
}

export function ravelrySearchQueryFromData(data: unknown): RavelrySearchQuery {
  const value = dataObject(data);
  const query = optionalString(value.query);
  if (!query) {
    throw new RavelryPatternImportError("missing_query", 400);
  }
  const pageSize = optionalBoundedPositiveInteger(
    value.pageSize,
    RAVELRY_SEARCH_MAX_PAGE_SIZE,
    "invalid_page_size",
  );
  return {
    query,
    ...(optionalString(value.craft) ? { craft: optionalString(value.craft) } : {}),
    ...(optionalString(value.availability) ? { availability: optionalString(value.availability) } : {}),
    ...(optionalString(value.pc) ? { pc: optionalString(value.pc) } : {}),
    ...(optionalString(value.weight) ? { weight: optionalString(value.weight) } : {}),
    ...(optionalNumber(value.difficultyFrom) != null
      ? { difficultyFrom: optionalNumber(value.difficultyFrom) }
      : {}),
    ...(optionalNumber(value.difficultyTo) != null ? { difficultyTo: optionalNumber(value.difficultyTo) } : {}),
    ...(positiveInteger(value.page) != null ? { page: positiveInteger(value.page) } : {}),
    ...(pageSize != null ? { pageSize } : {}),
  };
}

export async function searchPatternsForUser(options: SearchPatternsOptions) {
  const accessToken = await requireAccessTokenForOperation(options, "search");
  return options.client.searchPatterns(accessToken, options.query);
}

export async function importPatternById(options: ImportPatternByIdOptions): Promise<SanitizedPattern> {
  if (!Number.isInteger(options.ravelryPatternId) || options.ravelryPatternId <= 0) {
    throw new RavelryPatternImportError("invalid_pattern_id", 400);
  }

  const accessToken = await requireAccessTokenForOperation(options, "import");
  const pattern = await options.client.getPatternById(accessToken, options.ravelryPatternId);
  if (!pattern) {
    throw new RavelryPatternImportError("pattern_not_found", 404);
  }
  return pattern;
}

export async function importPatternByUrl(options: ImportPatternByUrlOptions): Promise<SanitizedPattern> {
  const parsedUrl = parseRavelryPatternUrl(options.url);
  if (!parsedUrl) {
    throw new RavelryPatternImportError("invalid_ravelry_pattern_url", 400);
  }

  if (parsedUrl.ravelryPatternId != null) {
    return withOriginalUrl(
      await importPatternById({
        uid: options.uid,
        tokenStore: options.tokenStore,
        client: options.client,
        rateLimiter: options.rateLimiter,
        refresh: options.refresh,
        nowMillis: options.nowMillis,
        ravelryPatternId: parsedUrl.ravelryPatternId,
      }),
      parsedUrl.originalUrl,
    );
  }

  const response = await searchPatternsForUser({
    uid: options.uid,
    tokenStore: options.tokenStore,
    client: options.client,
    rateLimiter: options.rateLimiter,
    refresh: options.refresh,
    nowMillis: options.nowMillis,
    query: {
      query: parsedUrl.patternSlug ?? parsedUrl.canonicalUrl,
      page: 1,
      pageSize: 10,
    },
  });
  const matchedPattern = response.patterns.find(
    (pattern) => pattern.canonicalUrl === parsedUrl.canonicalUrl,
  );
  if (!matchedPattern) {
    throw new RavelryPatternImportError("pattern_not_found", 404);
  }

  return withOriginalUrl(
    await importPatternById({
      uid: options.uid,
      tokenStore: options.tokenStore,
      client: options.client,
      rateLimiter: options.rateLimiter,
      refresh: options.refresh,
      nowMillis: options.nowMillis,
      ravelryPatternId: matchedPattern.ravelryPatternId,
    }),
    parsedUrl.originalUrl,
  );
}

function stores() {
  return createRavelryBackendStores();
}

function rateLimiterFor(options: UserPatternOptions): RavelryRateLimiter {
  return options.rateLimiter ?? disabledRavelryRateLimiter;
}

export const ravelrySearchPatterns = onCall(ravelrySecretOptions, async (request) => {
  try {
    const uid = requireUid(request.auth);
    const { rateLimiter, tokenStore } = stores();
    return await searchPatternsForUser({
      uid,
      tokenStore,
      rateLimiter,
      client: createRavelryClient(),
      refresh: refreshRavelryAccessToken,
      query: ravelrySearchQueryFromData(request.data),
    });
  } catch (error) {
    throw httpsErrorFor(error, "ravelry_search_failed");
  }
});

export const ravelryImportPatternById = onCall(ravelrySecretOptions, async (request) => {
  try {
    const uid = requireUid(request.auth);
    const { rateLimiter, tokenStore } = stores();
    const ravelryPatternId = positiveInteger(dataObject(request.data).ravelryPatternId);
    if (ravelryPatternId == null) {
      throw new RavelryPatternImportError("invalid_pattern_id", 400);
    }
    return await importPatternById({
      uid,
      tokenStore,
      rateLimiter,
      client: createRavelryClient(),
      refresh: refreshRavelryAccessToken,
      ravelryPatternId,
    });
  } catch (error) {
    throw httpsErrorFor(error, "ravelry_import_by_id_failed");
  }
});

export const ravelryImportPatternByUrl = onCall(ravelrySecretOptions, async (request) => {
  try {
    const uid = requireUid(request.auth);
    const { rateLimiter, tokenStore } = stores();
    const url = optionalString(dataObject(request.data).url);
    if (!url) {
      throw new RavelryPatternImportError("missing_url", 400);
    }
    return await importPatternByUrl({
      uid,
      tokenStore,
      rateLimiter,
      client: createRavelryClient(),
      refresh: refreshRavelryAccessToken,
      url,
    });
  } catch (error) {
    throw httpsErrorFor(error, "ravelry_import_by_url_failed");
  }
});
