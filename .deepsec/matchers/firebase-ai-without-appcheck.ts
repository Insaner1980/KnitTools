import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const firebaseAiWithoutAppCheck: MatcherPlugin = {
  slug: "firebase-ai-without-appcheck",
  description:
    "Firebase AI usage should keep App Check limited-use tokens enabled for hosted model calls",
  noiseTier: "precise",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!/Firebase\.ai|FirebaseAI/.test(content)) return [];
    if (/useLimitedUseAppCheckTokens\s*=\s*true/.test(content)) return [];

    return regexCandidates("firebase-ai-without-appcheck", content, [
      { regex: /Firebase\.ai|FirebaseAI/, label: "Firebase AI call without limited-use App Check token option" },
    ]);
  },
};
