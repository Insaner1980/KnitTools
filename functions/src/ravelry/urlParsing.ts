export interface ParsedRavelryPatternUrl {
  readonly originalUrl: string;
  readonly canonicalUrl: string;
  readonly ravelryPatternId?: number;
  readonly patternSlug?: string;
}

function canonicalLibraryUrl(patternSlug: string): string {
  return `https://www.ravelry.com/patterns/library/${encodeURIComponent(patternSlug)}`;
}

export function parseRavelryPatternUrl(url: string): ParsedRavelryPatternUrl | null {
  const originalUrl = url.trim();
  let parsedUrl: URL;
  try {
    parsedUrl = new URL(originalUrl);
  } catch {
    return null;
  }

  const hostname = parsedUrl.hostname.toLowerCase().replace(/^www\./, "");
  if ((parsedUrl.protocol !== "https:" && parsedUrl.protocol !== "http:") || hostname !== "ravelry.com") {
    return null;
  }

  const segments = parsedUrl.pathname.split("/").filter((segment) => segment.length > 0);
  if (segments[0] !== "patterns" || segments[1] !== "library" || !segments[2]) {
    return null;
  }

  const patternSlug = decodeURIComponent(segments[2]).trim();
  if (!patternSlug) {
    return null;
  }

  const ravelryPatternId = /^\d+$/.test(patternSlug) ? Number(patternSlug) : undefined;
  return {
    originalUrl,
    canonicalUrl: canonicalLibraryUrl(patternSlug),
    ...(ravelryPatternId != null ? { ravelryPatternId } : {}),
    patternSlug,
  };
}
