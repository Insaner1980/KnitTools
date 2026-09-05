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
      const nextCallIndex = content.indexOf(")", index) + 1;
      const call = content.slice(index, nextCallIndex > 0 ? nextCallIndex : content.length);
      if (!sensitiveRegex.test(call)) return [];

      return [candidate("sensitive-android-log", content, index, "Sensitive term in Android log call")];
    });
  },
};
