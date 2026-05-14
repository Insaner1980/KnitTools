import type { CandidateMatch } from "deepsec/config";

export function isTestFile(filePath: string): boolean {
  return /(?:^|[\\/])(?:test|androidTest|__tests__)(?:[\\/])|[._-](?:test|spec)\./i.test(filePath);
}

export function lineNumberAt(content: string, index: number): number {
  return content.slice(0, index).split(/\r\n|\r|\n/).length;
}

export function snippetAroundLine(content: string, lineNumber: number, context = 3): string {
  const lines = content.split(/\r\n|\r|\n/);
  const start = Math.max(0, lineNumber - context - 1);
  const end = Math.min(lines.length, lineNumber + context);
  return lines.slice(start, end).join("\n");
}

export function candidate(
  slug: string,
  content: string,
  index: number,
  matchedPattern: string,
): CandidateMatch {
  const lineNumber = lineNumberAt(content, index);
  return {
    vulnSlug: slug,
    lineNumbers: [lineNumber],
    snippet: snippetAroundLine(content, lineNumber),
    matchedPattern,
  };
}

export function regexCandidates(
  slug: string,
  content: string,
  patterns: Array<{ regex: RegExp; label: string }>,
): CandidateMatch[] {
  const matches: CandidateMatch[] = [];

  for (const { regex, label } of patterns) {
    const flags = regex.flags.includes("g") ? regex.flags : `${regex.flags}g`;
    const globalRegex = new RegExp(regex.source, flags);
    for (const match of content.matchAll(globalRegex)) {
      matches.push(candidate(slug, content, match.index ?? 0, label));
    }
  }

  return matches;
}
