import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const widgetMutationSurface: MatcherPlugin = {
  slug: "widget-mutation-surface",
  description:
    "KnitTools widget broadcast and repository mutation surfaces that can change counter state from home-screen widgets",
  noiseTier: "normal",
  filePatterns: [
    "app/src/main/java/com/finnvek/knittools/widget/**/*.kt",
    "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt",
  ],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!/\b(?:Widget|widget|BroadcastReceiver|actionSendBroadcast|applyWidgetCountChange)\b/.test(content)) return [];

    return regexCandidates("widget-mutation-surface", content, [
      {
        regex: /\bclass\s+\w+[\s\S]{0,260}:\s*(?:[\w.]+\.)?BroadcastReceiver\s*\(/,
        label: "Android broadcast receiver entry point",
      },
      {
        regex: /\bactionSendBroadcast\s*\(/,
        label: "Glance widget broadcast action",
      },
      {
        regex: /\bapplyWidgetCountChange\s*\(/,
        label: "Widget counter repository mutation",
      },
      {
        regex: /\bCounterWidgetState\.(?:load|syncAll|updateProjectId)\s*\(/,
        label: "Widget persisted state boundary",
      },
    ]);
  },
};
