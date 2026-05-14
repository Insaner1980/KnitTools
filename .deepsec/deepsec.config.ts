import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { type DeepsecPlugin, defineConfig } from "deepsec/config";
import { androidExportedComponent } from "./matchers/android-exported-component.js";
import { androidUriShareWithoutClipData } from "./matchers/android-uri-share-without-clipdata.js";
import { fileproviderBroadPath } from "./matchers/fileprovider-broad-path.js";
import { firebaseAiWithoutAppCheck } from "./matchers/firebase-ai-without-appcheck.js";
import { ravelryCredentialSurface } from "./matchers/ravelry-credential-surface.js";
import { sensitiveAndroidLog } from "./matchers/sensitive-android-log.js";

const here = path.dirname(fileURLToPath(import.meta.url));

function knitToolsPlugin(): DeepsecPlugin {
  return {
    name: "knittools-android",
    matchers: [
      androidExportedComponent,
      fileproviderBroadPath,
      androidUriShareWithoutClipData,
      ravelryCredentialSurface,
      firebaseAiWithoutAppCheck,
      sensitiveAndroidLog,
    ],
  };
}

export default defineConfig({
  projects: [
    {
      id: "knittools",
      root: "..",
      infoMarkdown: fs.readFileSync(path.join(here, "data", "knittools", "INFO.md"), "utf-8"),
      promptAppend:
        "Prioritize exported Android components, OAuth callback handling, FileProvider URI grants, widget mutations, Ravelry credential handling, Firebase AI App Check use, and sensitive logging.",
      priorityPaths: [
        "app/src/main/AndroidManifest.xml",
        "app/src/main/java/com/finnvek/knittools/auth/",
        "app/src/main/java/com/finnvek/knittools/ai/",
        "app/src/main/java/com/finnvek/knittools/widget/",
        "app/src/main/java/com/finnvek/knittools/data/storage/",
      ],
    },
  ],
  plugins: [knitToolsPlugin()],
});
