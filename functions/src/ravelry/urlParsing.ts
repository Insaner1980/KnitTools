export interface ParsedRavelryPatternUrl {
  readonly originalUrl: string;
  readonly canonicalUrl: string;
  readonly ravelryPatternId?: number;
  readonly patternSlug?: string;
}

const MAX_RAVELRY_PATTERN_URL_LENGTH = 2_048;
const MAX_RAVELRY_PATTERN_SLUG_LENGTH = 512;
const MAX_RAVELRY_PATTERN_ID = 2_147_483_647;
const CONTROL_CHARACTERS = /[\u0000-\u001F\u007F-\u009F\u2028\u2029]/;

function canonicalLibraryUrl(patternSlug: string): string {
  return `https://www.ravelry.com/patterns/library/${encodeURIComponent(patternSlug)}`;
}

export function parseRavelryPatternUrl(url: string): ParsedRavelryPatternUrl | null {
  const originalUrl = url.trim();
  if (
    originalUrl.length === 0 ||
    originalUrl.length > MAX_RAVELRY_PATTERN_URL_LENGTH ||
    CONTROL_CHARACTERS.test(originalUrl)
  ) {
    return null;
  }
  let parsedUrl: URL;
  try {
    parsedUrl = new URL(originalUrl);
  } catch {
    return null;
  }

  const hostname = parsedUrl.hostname.toLowerCase().replace(/^www\./, "");
  if (
    parsedUrl.protocol !== "https:" ||
    hostname !== "ravelry.com" ||
    parsedUrl.username.length > 0 ||
    parsedUrl.password.length > 0 ||
    (parsedUrl.port.length > 0 && parsedUrl.port !== "443")
  ) {
    return null;
  }

  const segments = parsedUrl.pathname.split("/").filter((segment) => segment.length > 0);
  if (segments.length !== 3 || segments[0] !== "patterns" || segments[1] !== "library" || !segments[2]) {
    return null;
  }

  let patternSlug: string;
  try {
    patternSlug = decodeURIComponent(segments[2]).trim();
  } catch {
    return null;
  }
  if (
    !patternSlug ||
    patternSlug.length > MAX_RAVELRY_PATTERN_SLUG_LENGTH ||
    patternSlug.includes("/") ||
    patternSlug.includes("\\") ||
    patternSlug.includes("?") ||
    patternSlug.includes("#") ||
    CONTROL_CHARACTERS.test(patternSlug)
  ) {
    return null;
  }

  const numericPatternId = /^\d+$/.test(patternSlug) ? Number(patternSlug) : undefined;
  if (
    numericPatternId != null &&
    (!Number.isSafeInteger(numericPatternId) || numericPatternId <= 0 || numericPatternId > MAX_RAVELRY_PATTERN_ID)
  ) {
    return null;
  }
  const ravelryPatternId = numericPatternId;
  return {
    originalUrl,
    canonicalUrl: canonicalLibraryUrl(patternSlug),
    ...(ravelryPatternId != null ? { ravelryPatternId } : {}),
    patternSlug,
  };
}
