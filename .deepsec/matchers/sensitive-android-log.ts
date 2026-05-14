import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

const sensitiveWords =
  "(?:ravelry|oauth|token|credential|secret|password|billing|purchase|project|projectId|counter|count|pattern|instruction|ocr|prompt|voice|ai)";

export const sensitiveAndroidLog: MatcherPlugin = {
  slug: "sensitive-android-log",
  description:
    "Android log statements that may disclose user project data, billing state, AI/OCR content, or credentials",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return regexCandidates("sensitive-android-log", content, [
      {
        regex: new RegExp(
          String.raw`\b(?:Log|android\.util\.Log)\.(?:v|d|i|w|e)\s*\([^;\n]*${sensitiveWords}[^;\n]*\)`,
          "i",
        ),
        label: "Sensitive term in Android log call",
      },
    ]);
  },
};
