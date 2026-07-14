import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const androidKotlinEntrypointSurface: MatcherPlugin = {
  slug: "android-kotlin-entrypoint-surface",
  description:
    "Android Kotlin activities, receivers, services, workers, and widget entry points that deserve trust-boundary review",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt", "app/src/release/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return regexCandidates("android-kotlin-entrypoint-surface", content, [
      {
        regex: /\bclass\s+\w+[\s\S]{0,260}:\s*(?:[\w.]+\.)?(?:AppCompatActivity|ComponentActivity|Activity)\s*\(/,
        label: "Android activity entry point",
      },
      {
        regex: /\bclass\s+\w+[\s\S]{0,260}:\s*(?:[\w.]+\.)?GlanceAppWidgetReceiver\s*\(/,
        label: "Glance app widget receiver entry point",
      },
      {
        regex: /\bclass\s+\w+[\s\S]{0,260}:\s*(?:[\w.]+\.)?BroadcastReceiver\s*\(/,
        label: "Android broadcast receiver entry point",
      },
      {
        regex: /\bclass\s+\w+[\s\S]{0,260}:\s*(?:[\w.]+\.)?(?:Service|LifecycleService)\s*\(/,
        label: "Android service entry point",
      },
      {
        regex: /\bclass\s+\w+[\s\S]{0,320}:\s*(?:[\w.]+\.)?(?:Worker|CoroutineWorker|ListenableWorker)\s*\(/,
        label: "WorkManager background execution entry point",
      },
      {
        regex: /\bWorkManager\.getInstance\s*\(|\benqueueUnique(?:Periodic)?Work\s*\(/,
        label: "WorkManager scheduling surface",
      },
    ]);
  },
};
