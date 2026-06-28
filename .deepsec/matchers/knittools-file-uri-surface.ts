import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const knitToolsFileUriSurface: MatcherPlugin = {
  slug: "knittools-file-uri-surface",
  description:
    "KnitTools SAF, FileProvider, content URI, and local file copy boundaries for pattern, yarn, and progress files",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt", "app/src/release/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!/\b(?:Uri|FileProvider|contentResolver|OpenDocument|CreateDocument|copyTo|copyRecursively)\b/.test(content)) {
      return [];
    }

    return regexCandidates("knittools-file-uri-surface", content, [
      {
        regex: /\bFileProvider\.getUriForFile\s*\(/,
        label: "Android FileProvider URI creation",
      },
      {
        regex: /\bcontentResolver\.openInputStream\s*\(/,
        label: "Android content resolver file read",
      },
      {
        regex: /\bcontentResolver\.openOutputStream\s*\(/,
        label: "Android content resolver file write",
      },
      {
        regex: /\btakePersistableUriPermission\s*\(/,
        label: "Android persistable URI permission boundary",
      },
      {
        regex: /\bActivityResultContracts\.(?:OpenDocument|CreateDocument|GetContent)\b/,
        label: "Android Storage Access Framework picker boundary",
      },
      {
        regex: /\b(?:copyTo|copyRecursively)\s*\(/,
        label: "File copy boundary",
      },
      {
        regex: /\b(?:delete|deleteRecursively)\s*\(/,
        label: "File deletion boundary",
      },
    ]);
  },
};
