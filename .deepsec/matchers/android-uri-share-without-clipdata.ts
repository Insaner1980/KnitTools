import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { candidate, isTestFile } from "./utils.js";

export const androidUriShareWithoutClipData: MatcherPlugin = {
  slug: "android-uri-share-without-clipdata",
  description:
    "ACTION_SEND content URI shares that should pair EXTRA_STREAM with read grants and ClipData",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!content.includes("Intent.ACTION_SEND") && !content.includes("Intent.ACTION_SEND_MULTIPLE")) return [];
    if (!content.includes("Intent.EXTRA_STREAM")) return [];

    const hasReadGrant = content.includes("FLAG_GRANT_READ_URI_PERMISSION");
    const hasClipData = /\bclipData\b|ClipData\./.test(content);
    if (hasReadGrant && hasClipData) return [];

    const index =
      content.indexOf("Intent.ACTION_SEND") >= 0
        ? content.indexOf("Intent.ACTION_SEND")
        : content.indexOf("Intent.ACTION_SEND_MULTIPLE");

    return [
      candidate(
        "android-uri-share-without-clipdata",
        content,
        index,
        hasReadGrant
          ? "EXTRA_STREAM content URI share without ClipData"
          : "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION",
      ),
    ];
  },
};
