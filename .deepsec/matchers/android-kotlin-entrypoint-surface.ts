import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, kotlinClassHeaderCode, regexCandidates } from "./utils.js";

export const androidKotlinEntrypointSurface: MatcherPlugin = {
  slug: "android-kotlin-entrypoint-surface",
  description:
    "Android Kotlin activities, receivers, services, workers, and widget entry points that deserve trust-boundary review",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt", "app/src/release/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    const entrypoints = regexCandidates("android-kotlin-entrypoint-surface", content, [
      {
        regex: /\bclass\s+\w+(?:(?!\bclass\b)[^{};]){0,260}?:\s*(?:[\w.]+\.)?(?:AppCompatActivity|ComponentActivity|Activity)\s*\(/,
        label: "Android activity entry point",
      },
      {
        regex: /\bclass\s+\w+(?:(?!\bclass\b)[^{};]){0,260}?:\s*(?:[\w.]+\.)?GlanceAppWidgetReceiver\s*\(/,
        label: "Glance app widget receiver entry point",
      },
      {
        regex: /\bclass\s+\w+(?:(?!\bclass\b)[^{};]){0,260}?:\s*(?:[\w.]+\.)?BroadcastReceiver\s*\(/,
        label: "Android broadcast receiver entry point",
      },
      {
        regex: /\bclass\s+\w+(?:(?!\bclass\b)[^{};]){0,260}?:\s*(?:[\w.]+\.)?(?:Service|LifecycleService)\s*\(/,
        label: "Android service entry point",
      },
      {
        regex: /\bclass\s+\w+(?:(?!\bclass\b)[^{};]){0,320}?:\s*(?:[\w.]+\.)?(?:Worker|CoroutineWorker|ListenableWorker)\s*\(/,
        label: "WorkManager background execution entry point",
      },
    ], kotlinClassHeaderCode(content));
    return entrypoints.concat(regexCandidates("android-kotlin-entrypoint-surface", content, [
      {
        regex: /\bWorkManager\.getInstance\s*\(|\benqueueUnique(?:Periodic)?Work\s*\(/,
        label: "WorkManager scheduling surface",
      },
    ]));
  },
};
