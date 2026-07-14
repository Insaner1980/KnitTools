import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const ravelryFirebaseCallableSurface: MatcherPlugin = {
  slug: "ravelry-firebase-callable-surface",
  description:
    "Ravelry Firebase Auth and Functions callable boundaries where backend-owned OAuth and metadata imports cross Android",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt", "app/src/release/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!/\b(?:Firebase|Ravelry|ravelry)\b/.test(content)) return [];

    return regexCandidates("ravelry-firebase-callable-surface", content, [
      {
        regex: /\b(?:Firebase\.auth|FirebaseAuth\b|FirebaseAuth\.getInstance\s*\()/,
        label: "Firebase Auth boundary",
      },
      {
        regex: /\bgetHttpsCallable\s*\(/,
        label: "Firebase callable function boundary",
      },
      {
        regex:
          /["']ravelry(?:SearchPatterns|ImportPatternById|ImportPatternByUrl|OAuthStart|OAuthCallback|AuthStatus|Disconnect|CurrentUser)["']/,
        label: "Ravelry backend callable name",
      },
      {
        regex: /\bhandleCallback\s*\(|\bstartAuth\s*\(|\bdisconnect\s*\(/,
        label: "Ravelry backend auth state boundary",
      },
    ]);
  },
};
