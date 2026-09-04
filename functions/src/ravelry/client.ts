import type {
  RavelryAvailability,
  SanitizedPagination,
  SanitizedPattern,
  SanitizedPatternSearchResponse,
} from "./sanitizedTypes";
import { readJsonResponse } from "./boundedResponse";
import { toSanitizedPattern } from "./sanitizedTypes";

const RAVELRY_API_BASE_URL = "https://api.ravelry.com";
const RAVELRY_API_REQUEST_TIMEOUT_MILLIS = 10_000;
const RAVELRY_API_RESPONSE_MAX_BYTES = 1_048_576;
const MAX_RAVELRY_PATTERN_ID = 2_147_483_647;
const MAX_PATTERN_TITLE_LENGTH = 500;
const MAX_DESIGNER_NAME_LENGTH = 300;
const MAX_RAVELRY_USER_FIELD_LENGTH = 200;
const MAX_PATTERN_PERMALINK_LENGTH = 512;
const MAX_REMOTE_URL_LENGTH = 2_048;
const MAX_PAGINATION_VALUE = 2_147_483_647;
const CONTROL_CHARACTERS = /[\u0000-\u001F\u007F-\u009F\u2028\u2029]/g;

export class RavelryClientHttpError extends Error {
  readonly code: string;
  readonly httpStatus: number;

  constructor(httpStatus: number) {
    super(`ravelry_http_${httpStatus}`);
    this.code = `ravelry_http_${httpStatus}`;
    this.httpStatus = httpStatus;
  }
}

export interface RavelrySearchQuery {
  readonly query: string;
  readonly craft?: string;
  readonly availability?: string;
  readonly pc?: string;
  readonly weight?: string;
  readonly difficultyFrom?: number;
  readonly difficultyTo?: number;
  readonly page?: number;
  readonly pageSize?: number;
}

export interface RavelryCurrentUser {
  readonly ravelryUserId?: string;
  readonly ravelryUsername?: string;
}

export interface RavelryCurrentUserClient {
  getCurrentUser(accessToken: string): Promise<RavelryCurrentUser>;
}

export interface RavelryClient {
  getCurrentUser(accessToken: string): Promise<RavelryCurrentUser>;
  searchPatterns(accessToken: string, query: RavelrySearchQuery): Promise<SanitizedPatternSearchResponse>;
  getPatternById(accessToken: string, ravelryPatternId: number): Promise<SanitizedPattern | null>;
}

function sanitizedTextOrUndefined(value: unknown, maxLength: number): string | undefined {
  const stringValue =
    typeof value === "number" && Number.isFinite(value)
      ? String(value)
      : typeof value === "string"
        ? value
        : undefined;
  if (stringValue == null) {
    return undefined;
  }
  const sanitized = stringValue.replace(CONTROL_CHARACTERS, " ").replace(/\s+/gu, " ").trim();
  if (sanitized.length === 0) {
    return undefined;
  }
  return Array.from(sanitized).slice(0, maxLength).join("");
}

function thumbnailUrlOrUndefined(value: unknown): string | undefined {
  if (typeof value !== "string") {
    return undefined;
  }
  const trimmedValue = value.trim();
  if (trimmedValue.length === 0 || trimmedValue.length > MAX_REMOTE_URL_LENGTH) {
    return undefined;
  }
  try {
    const parsed = new URL(trimmedValue);
    return parsed.protocol === "https:" &&
      parsed.hostname.length > 0 &&
      parsed.username.length === 0 &&
      parsed.password.length === 0
      ? trimmedValue
      : undefined;
  } catch {
    return undefined;
  }
}

function currentUserFromResponse(value: unknown): RavelryCurrentUser {
  const root = value as { user?: unknown };
  const user = (root.user ?? value) as {
    id?: unknown;
    username?: unknown;
    login?: unknown;
    name?: unknown;
  };

  return {
    ravelryUserId: sanitizedTextOrUndefined(user.id, MAX_RAVELRY_USER_FIELD_LENGTH),
    ravelryUsername:
      sanitizedTextOrUndefined(user.username, MAX_RAVELRY_USER_FIELD_LENGTH) ??
      sanitizedTextOrUndefined(user.login, MAX_RAVELRY_USER_FIELD_LENGTH) ??
      sanitizedTextOrUndefined(user.name, MAX_RAVELRY_USER_FIELD_LENGTH),
  };
}

function objectOrNull(value: unknown): Record<string, unknown> | null {
  return typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;
}

function integerInRangeOrUndefined(
  value: unknown,
  minimum: number,
  maximum: number,
): number | undefined {
  return typeof value === "number" &&
    Number.isSafeInteger(value) &&
    value >= minimum &&
    value <= maximum
    ? value
    : undefined;
}

function positiveIntegerOrUndefined(value: unknown): number | undefined {
  return integerInRangeOrUndefined(value, 1, MAX_RAVELRY_PATTERN_ID);
}

function arrayOrEmpty(value: unknown): readonly unknown[] {
  return Array.isArray(value) ? value : [];
}

function availabilityFromFree(value: unknown): RavelryAvailability {
  if (value === true) {
    return "free";
  }
  if (value === false) {
    return "paid";
  }
  return "unknown";
}

function canonicalPatternUrl(permalink: string): string {
  return `https://www.ravelry.com/patterns/library/${encodeURIComponent(permalink)}`;
}

function designerNameFrom(value: Record<string, unknown>): string {
  const designer = objectOrNull(value.designer);
  return sanitizedTextOrUndefined(designer?.name, MAX_DESIGNER_NAME_LENGTH) ?? "";
}

function searchThumbnailFrom(value: Record<string, unknown>): string | undefined {
  const photo = objectOrNull(value.first_photo);
  return thumbnailUrlOrUndefined(photo?.medium_url) ?? thumbnailUrlOrUndefined(photo?.small2_url);
}

function detailThumbnailFrom(value: Record<string, unknown>): string | undefined {
  const firstPhoto = objectOrNull(arrayOrEmpty(value.photos)[0]);
  return thumbnailUrlOrUndefined(firstPhoto?.medium_url) ?? thumbnailUrlOrUndefined(firstPhoto?.small2_url);
}

function sanitizePatternValue(value: unknown, thumbnailUrl?: string): SanitizedPattern | null {
  const pattern = objectOrNull(value);
  if (!pattern) {
    return null;
  }

  const ravelryPatternId = positiveIntegerOrUndefined(pattern.id);
  const permalink = sanitizedTextOrUndefined(pattern.permalink, MAX_PATTERN_PERMALINK_LENGTH);
  if (ravelryPatternId == null || !permalink) {
    return null;
  }

  return toSanitizedPattern({
    ravelryPatternId,
    title: sanitizedTextOrUndefined(pattern.name, MAX_PATTERN_TITLE_LENGTH) ?? "",
    designerName: designerNameFrom(pattern),
    ...(thumbnailUrl ? { thumbnailUrl } : {}),
    canonicalUrl: canonicalPatternUrl(permalink),
    availability: availabilityFromFree(pattern.free),
  });
}

function paginationFrom(value: unknown): SanitizedPagination {
  const paginator = objectOrNull(value);
  return {
    page: integerInRangeOrUndefined(paginator?.page, 1, MAX_PAGINATION_VALUE) ?? 1,
    pageCount: integerInRangeOrUndefined(paginator?.page_count, 1, MAX_PAGINATION_VALUE) ?? 1,
    resultCount: integerInRangeOrUndefined(paginator?.results, 0, MAX_PAGINATION_VALUE) ?? 0,
  };
}

function appendOptionalString(url: URL, name: string, value: string | undefined): void {
  const trimmedValue = value?.trim();
  if (trimmedValue) {
    url.searchParams.set(name, trimmedValue);
  }
}

function appendOptionalNumber(url: URL, name: string, value: number | undefined): void {
  if (value != null && Number.isFinite(value)) {
    url.searchParams.set(name, String(value));
  }
}

async function ravelryJson(
  fetchImpl: typeof fetch,
  accessToken: string,
  url: URL,
): Promise<unknown> {
  const response = await fetchRavelry(fetchImpl, url.toString(), {
    headers: new Headers({
      Authorization: `Bearer ${accessToken}`,
    }),
  });

  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new RavelryClientHttpError(response.status);
  }

  return responseJson(response);
}

async function responseJson(response: Response): Promise<unknown> {
  try {
    return await readJsonResponse(response, RAVELRY_API_RESPONSE_MAX_BYTES);
  } catch {
    throw new RavelryClientHttpError(503);
  }
}

async function fetchRavelry(
  fetchImpl: typeof fetch,
  input: string,
  init: RequestInit,
): Promise<Response> {
  try {
    return await fetchImpl(input, {
      ...init,
      redirect: "error",
      signal: AbortSignal.timeout(RAVELRY_API_REQUEST_TIMEOUT_MILLIS),
    });
  } catch {
    throw new RavelryClientHttpError(503);
  }
}

export function createRavelryClient(fetchImpl: typeof fetch = fetch): RavelryClient {
  return {
    async getCurrentUser(accessToken) {
      const response = await fetchRavelry(fetchImpl, "https://api.ravelry.com/current_user.json", {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });

      if (!response.ok) {
        throw new RavelryClientHttpError(response.status);
      }

      return currentUserFromResponse(await responseJson(response));
    },
    async searchPatterns(accessToken, query) {
      const url = new URL(`${RAVELRY_API_BASE_URL}/patterns/search.json`);
      appendOptionalString(url, "query", query.query);
      appendOptionalString(url, "craft", query.craft);
      appendOptionalString(url, "availability", query.availability);
      appendOptionalString(url, "pc", query.pc);
      appendOptionalString(url, "weight", query.weight);
      appendOptionalNumber(url, "diff_from", query.difficultyFrom);
      appendOptionalNumber(url, "diff_to", query.difficultyTo);
      appendOptionalNumber(url, "page", query.page);
      appendOptionalNumber(url, "page_size", query.pageSize);
      url.searchParams.set("sort", "best");

      const body = objectOrNull(await ravelryJson(fetchImpl, accessToken, url));
      const patterns = arrayOrEmpty(body?.patterns)
        .map((value) => sanitizePatternValue(value, searchThumbnailFrom(objectOrNull(value) ?? {})))
        .filter((value): value is SanitizedPattern => value !== null);
      return {
        patterns,
        pagination: paginationFrom(body?.paginator),
      };
    },
    async getPatternById(accessToken, ravelryPatternId) {
      const url = new URL(`${RAVELRY_API_BASE_URL}/patterns/${ravelryPatternId}.json`);
      const body = await ravelryJson(fetchImpl, accessToken, url);
      if (body === null) {
        return null;
      }
      const pattern = objectOrNull(body)?.pattern;
      return sanitizePatternValue(pattern, detailThumbnailFrom(objectOrNull(pattern) ?? {}));
    },
  };
}
