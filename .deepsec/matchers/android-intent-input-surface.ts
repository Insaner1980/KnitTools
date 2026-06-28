import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const androidIntentInputSurface: MatcherPlugin = {
  slug: "android-intent-input-surface",
  description:
    "Android intent data and extras that cross exported activity, receiver, callback, or share-import boundaries",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt", "app/src/release/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!/\b(?:Intent|intent)\b/.test(content)) return [];

    return regexCandidates("android-intent-input-surface", content, [
      {
        regex: /\b\w*intent\??\.data\b/i,
        label: "Android intent data URI read",
      },
      {
        regex: /\bget(?:String|CharSequence)Extra\s*\(\s*Intent\.EXTRA_TEXT\b/,
        label: "Android text share extra read",
      },
      {
        regex:
          /\bget(?:Boolean|Byte|Char|Short|Int|Long|Float|Double|String|CharSequence|Parcelable|Serializable|Bundle|StringArrayList|IntegerArrayList|ParcelableArrayList)?Extra\s*\((?!\s*Intent\.EXTRA_TEXT\b)/,
        label: "Android intent extra read",
      },
      {
        regex: /\b(?:intent\??\.)?extras\b/i,
        label: "Android intent extras bundle read",
      },
    ]);
  },
};
