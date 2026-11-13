import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { candidate, isTestFile, kotlinStructuralCode } from "./utils.js";

export const androidUriShareWithoutClipData: MatcherPlugin = {
  slug: "android-uri-share-without-clipdata",
  description:
    "ACTION_SEND content URI shares that should pair EXTRA_STREAM with read grants and ClipData",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    const actionRegex = /\bIntent\.ACTION_SEND(?:_MULTIPLE)?\b/g;
    const code = kotlinStructuralCode(content);
    const actionMatches = [...code.matchAll(actionRegex)];

    return actionMatches.flatMap((actionMatch, matchIndex) => {
      const index = actionMatch.index ?? 0;
      const nextIndex = actionMatches[matchIndex + 1]?.index ?? content.length;
      let depth = 0;
      let endIndex = nextIndex;
      for (const token of code.slice(index, nextIndex).matchAll(/[{}]|\bfun\s+/g)) {
        if (token[0] === "{") depth++;
        else if (token[0] === "}") depth--;
        else if (depth <= 0) {
          endIndex = index + token.index;
          break;
        }
      }
      const shareBlock = code.slice(index, endIndex);
      if (!shareBlock.includes("Intent.EXTRA_STREAM")) return [];

      const hasReadGrant = shareBlock.includes("FLAG_GRANT_READ_URI_PERMISSION");
      const streamUri = /\bputExtra\s*\(\s*Intent\.EXTRA_STREAM\s*,\s*([\w.]+)\s*\)/.exec(shareBlock)?.[1];
      const assignments = [...shareBlock.matchAll(/\bclipData\s*=(?!=)/g)];
      const lastAssignment = assignments.at(-1);
      const clipUri = lastAssignment && /^\bclipData\s*=\s*ClipData\.(?:newRawUri\s*\([^,]*,|newUri\s*\([^,]*,[^,]*,)\s*([\w.]+)\s*\)/
        .exec(shareBlock.slice(lastAssignment.index))?.[1];
      // Only a direct same-URI assignment is enough to dismiss this review candidate.
      const hasClipData = streamUri != null && clipUri === streamUri;
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
