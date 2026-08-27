import type {
  RavelryAvailability,
  SanitizedPagination,
  SanitizedPattern,
  SanitizedPatternSearchResponse,
} from "./sanitizedTypes";
import { toSanitizedPattern } from "./sanitizedTypes";

const RAVELRY_API_BASE_URL = "https://api.ravelry.com";

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

function stringOrUndefined(value: unknown): string | undefined {
  if (typeof value === "number") {
    return String(value);
  }
  return typeof value === "string" && value.trim().length > 0 ? value : undefined;
}

function thumbnailUrlOrUndefined(value: unknown): string | undefined {
  if (typeof value !== "string") {
    return undefined;
  }
  const trimmedValue = value.trim();
  if (trimmedValue.length === 0) {
    return undefined;
  }
  try {
    return new URL(trimmedValue).protocol === "https:" ? trimmedValue : undefined;
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
    ravelryUserId: stringOrUndefined(user.id),
    ravelryUsername:
      stringOrUndefined(user.username) ?? stringOrUndefined(user.login) ?? stringOrUndefined(user.name),
  };
}

function objectOrNull(value: unknown): Record<string, unknown> | null {
  return typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;
}

function numberOrUndefined(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function positiveIntegerOrUndefined(value: unknown): number | undefined {
  return typeof value === "number" && Number.isInteger(value) && value > 0 ? value : undefined;
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
  return stringOrUndefined(designer?.name) ?? "";
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
  const permalink = stringOrUndefined(pattern.permalink);
  if (ravelryPatternId == null || !permalink) {
    return null;
  }

  return toSanitizedPattern({
    ravelryPatternId,
    title: stringOrUndefined(pattern.name) ?? "",
    designerName: designerNameFrom(pattern),
    ...(thumbnailUrl ? { thumbnailUrl } : {}),
    canonicalUrl: canonicalPatternUrl(permalink),
    availability: availabilityFromFree(pattern.free),
  });
}

function paginationFrom(value: unknown): SanitizedPagination {
  const paginator = objectOrNull(value);
  return {
    page: numberOrUndefined(paginator?.page) ?? 1,
    pageCount: numberOrUndefined(paginator?.page_count) ?? 1,
    resultCount: numberOrUndefined(paginator?.results) ?? 0,
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
  const response = await fetchImpl(url.toString(), {
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

  return response.json();
}

export function createRavelryClient(fetchImpl: typeof fetch = fetch): RavelryClient {
  return {
    async getCurrentUser(accessToken) {
      const response = await fetchImpl("https://api.ravelry.com/current_user.json", {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });

      if (!response.ok) {
        throw new RavelryClientHttpError(response.status);
      }

      return currentUserFromResponse(await response.json());
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
