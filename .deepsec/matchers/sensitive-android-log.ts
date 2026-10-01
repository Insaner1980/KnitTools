import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { candidate, isTestFile } from "./utils.js";

const sensitiveWords =
  "(?:ravelry|oauth|token|credential|secret|password|billing|purchase|project|projectId|counter|count|pattern|instruction|voice)";

export const sensitiveAndroidLog: MatcherPlugin = {
  slug: "sensitive-android-log",
  description:
    "Android log statements that may disclose user project data, billing state, voice transcripts, or credentials",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    const logCallRegex = /\b(?:Log|android\.util\.Log)\.(?:v|d|i|w|e)\s*\(/g;
    const sensitiveRegex = new RegExp(sensitiveWords, "i");

    return [...content.matchAll(logCallRegex)].flatMap((logMatch) => {
      const index = logMatch.index ?? 0;
      const openIndex = index + logMatch[0].length - 1;
      const call = content.slice(index, logCallEnd(content, openIndex));
      if (!sensitiveRegex.test(call)) return [];

      return [candidate("sensitive-android-log", content, index, "Sensitive term in Android log call")];
    });
  },
};

function logCallEnd(content: string, openIndex: number): number {
  let depth = 0;
  let quote = "";
  for (let index = openIndex; index < content.length; index += 1) {
    if (quote === '"""') {
      if (content.startsWith(quote, index)) {
        quote = "";
        index += 2;
      }
    } else if (quote) {
      if (content[index] === "\\") index += 1;
      else if (content[index] === quote) quote = "";
    } else if (content.startsWith('"""', index)) {
      quote = '"""';
      index += 2;
    } else if (content[index] === '"' || content[index] === "'") {
      quote = content[index];
    } else if (content[index] === "(") {
      depth += 1;
    } else if (content[index] === ")") {
      depth -= 1;
      if (depth === 0) return index + 1;
    }
  }
  return content.length;
}
