export type RavelryAvailability = "free" | "paid" | "unknown";

export interface SanitizedPattern {
  readonly ravelryPatternId: number;
  readonly title: string;
  readonly designerName: string;
  readonly thumbnailUrl?: string;
  readonly canonicalUrl: string;
  readonly originalUrl?: string;
  readonly availability: RavelryAvailability;
}

export interface SanitizedPagination {
  readonly page: number;
  readonly pageCount: number;
  readonly resultCount: number;
}

export interface SanitizedPatternSearchResponse {
  readonly patterns: readonly SanitizedPattern[];
  readonly pagination: SanitizedPagination;
}

export function toSanitizedPattern(pattern: SanitizedPattern): SanitizedPattern {
  const sanitized: SanitizedPattern = {
    ravelryPatternId: pattern.ravelryPatternId,
    title: pattern.title,
    designerName: pattern.designerName,
    canonicalUrl: pattern.canonicalUrl,
    availability: pattern.availability,
  };
  return {
    ...sanitized,
    ...(pattern.thumbnailUrl ? { thumbnailUrl: pattern.thumbnailUrl } : {}),
    ...(pattern.originalUrl ? { originalUrl: pattern.originalUrl } : {}),
  };
}
