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
    const actionRegex = /Intent\.ACTION_SEND(?:_MULTIPLE)?/g;
    const actionMatches = [...content.matchAll(actionRegex)];

    return actionMatches.flatMap((actionMatch, matchIndex) => {
      const index = actionMatch.index ?? 0;
      const nextIndex = actionMatches[matchIndex + 1]?.index ?? content.length;
      const shareBlock = content.slice(index, nextIndex);
      if (!shareBlock.includes("Intent.EXTRA_STREAM")) return [];

      const hasReadGrant = shareBlock.includes("FLAG_GRANT_READ_URI_PERMISSION");
      const hasClipData = /\bclipData\b|ClipData\./.test(shareBlock);
      if (hasReadGrant && hasClipData) return [];

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
    });
  },
};
