import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { regexCandidates } from "./utils.js";

export const androidExportedComponent: MatcherPlugin = {
  slug: "android-exported-component",
  description:
    "Exported Android activities, services, and receivers that need explicit trust-boundary review",
  noiseTier: "normal",
  filePatterns: ["app/src/main/AndroidManifest.xml"],
  match(content): CandidateMatch[] {
    return regexCandidates("android-exported-component", content, [
      {
        regex:
          /<(activity|service|receiver)\b[\s\S]*?android:exported\s*=\s*"true"[\s\S]*?(?:<\/\1>|\/>)/,
        label: 'Android component with android:exported="true"',
      },
    ]);
  },
};
