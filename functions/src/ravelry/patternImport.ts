import { getFirestore } from "firebase-admin/firestore";
import { onCall } from "firebase-functions/v2/https";

import { httpsErrorFor, requireUid } from "./callable";
import { createRavelryClient, type RavelryClient, type RavelrySearchQuery } from "./client";
import type { SanitizedPattern } from "./sanitizedTypes";
import type { RavelryTokenStore } from "./tokenStore";
import { createTokenStore } from "./tokenStore";
import { parseRavelryPatternUrl } from "./urlParsing";

interface UserPatternOptions {
  readonly uid: string;
  readonly tokenStore: RavelryTokenStore;
  readonly client: RavelryClient;
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

export class RavelryPatternImportError extends Error {
  constructor(
    readonly code: string,
    readonly httpStatus: number,
  ) {
    super(code);
  }
}

async function requireAccessToken(uid: string, tokenStore: RavelryTokenStore): Promise<string> {
  const token = await tokenStore.getToken(uid);
  if (!token) {
    throw new RavelryPatternImportError("ravelry_not_connected", 412);
  }
  return token.accessToken;
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

function optionalNumber(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function optionalString(value: unknown): string | undefined {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : undefined;
}

function dataObject(data: unknown): Record<string, unknown> {
  return typeof data === "object" && data !== null ? (data as Record<string, unknown>) : {};
}

function queryFromData(data: unknown): RavelrySearchQuery {
  const value = dataObject(data);
  const query = optionalString(value.query);
  if (!query) {
    throw new RavelryPatternImportError("missing_query", 400);
  }
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
    ...(positiveInteger(value.pageSize) != null ? { pageSize: positiveInteger(value.pageSize) } : {}),
  };
}

export async function searchPatternsForUser({
  uid,
  tokenStore,
  client,
  query,
}: SearchPatternsOptions) {
  const accessToken = await requireAccessToken(uid, tokenStore);
  return client.searchPatterns(accessToken, query);
}

export async function importPatternById({
  uid,
  tokenStore,
  client,
  ravelryPatternId,
}: ImportPatternByIdOptions): Promise<SanitizedPattern> {
  const accessToken = await requireAccessToken(uid, tokenStore);
  if (!Number.isInteger(ravelryPatternId) || ravelryPatternId <= 0) {
    throw new RavelryPatternImportError("invalid_pattern_id", 400);
  }

  const pattern = await client.getPatternById(accessToken, ravelryPatternId);
  if (!pattern) {
    throw new RavelryPatternImportError("pattern_not_found", 404);
  }
  return pattern;
}

export async function importPatternByUrl({
  uid,
  tokenStore,
  client,
  url,
}: ImportPatternByUrlOptions): Promise<SanitizedPattern> {
  const parsedUrl = parseRavelryPatternUrl(url);
  if (!parsedUrl) {
    throw new RavelryPatternImportError("invalid_ravelry_pattern_url", 400);
  }

  if (parsedUrl.ravelryPatternId != null) {
    return withOriginalUrl(
      await importPatternById({
        uid,
        tokenStore,
        client,
        ravelryPatternId: parsedUrl.ravelryPatternId,
      }),
      parsedUrl.originalUrl,
    );
  }

  const response = await searchPatternsForUser({
    uid,
    tokenStore,
    client,
    query: {
      query: parsedUrl.patternSlug ?? parsedUrl.canonicalUrl,
      page: 1,
      pageSize: 10,
    },
  });
  const matchedPattern =
    response.patterns.find((pattern) => pattern.canonicalUrl === parsedUrl.canonicalUrl) ??
    response.patterns[0];
  if (!matchedPattern) {
    throw new RavelryPatternImportError("pattern_not_found", 404);
  }

  return withOriginalUrl(
    await importPatternById({
      uid,
      tokenStore,
      client,
      ravelryPatternId: matchedPattern.ravelryPatternId,
    }),
    parsedUrl.originalUrl,
  );
}

function tokenStore() {
  return createTokenStore(getFirestore());
}

export const ravelrySearchPatterns = onCall(async (request) => {
  try {
    return await searchPatternsForUser({
      uid: requireUid(request.auth),
      tokenStore: tokenStore(),
      client: createRavelryClient(),
      query: queryFromData(request.data),
    });
  } catch (error) {
    throw httpsErrorFor(error, "ravelry_search_failed");
  }
});

export const ravelryImportPatternById = onCall(async (request) => {
  try {
    const ravelryPatternId = positiveInteger(dataObject(request.data).ravelryPatternId);
    if (ravelryPatternId == null) {
      throw new RavelryPatternImportError("invalid_pattern_id", 400);
    }
    return await importPatternById({
      uid: requireUid(request.auth),
      tokenStore: tokenStore(),
      client: createRavelryClient(),
      ravelryPatternId,
    });
  } catch (error) {
    throw httpsErrorFor(error, "ravelry_import_by_id_failed");
  }
});

export const ravelryImportPatternByUrl = onCall(async (request) => {
  try {
    const url = optionalString(dataObject(request.data).url);
    if (!url) {
      throw new RavelryPatternImportError("missing_url", 400);
    }
    return await importPatternByUrl({
      uid: requireUid(request.auth),
      tokenStore: tokenStore(),
      client: createRavelryClient(),
      url,
    });
  } catch (error) {
    throw httpsErrorFor(error, "ravelry_import_by_url_failed");
  }
});
