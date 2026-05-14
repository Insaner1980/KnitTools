import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const ravelryCredentialSurface: MatcherPlugin = {
  slug: "ravelry-credential-surface",
  description:
    "Ravelry credential and OAuth surfaces where embedded secrets, token handling, and logging need review",
  noiseTier: "normal",
  filePatterns: ["app/build.gradle.kts", "app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return regexCandidates("ravelry-credential-surface", content, [
      {
        regex: /BuildConfig\.RAVELRY_(?:BASIC_AUTH|OAUTH2)_[A-Z_]+/,
        label: "Ravelry credential read from BuildConfig",
      },
      {
        regex: /KNITTOOLS_(?:ALLOW_EMBEDDED_RAVELRY_SECRETS|RAVELRY_[A-Z0-9_]+)/,
        label: "Ravelry release credential environment gate",
      },
      {
        regex: /debug\.credentials\.properties/,
        label: "Debug credential file path",
      },
      {
        regex: /Authorization["']?\s*,\s*["'](?:Basic|Bearer)\b/,
        label: "Ravelry Authorization header construction",
      },
    ]);
  },
};
