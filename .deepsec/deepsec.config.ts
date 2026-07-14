import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { type DeepsecPlugin, defineConfig } from "deepsec/config";
import { androidExportedComponent } from "./matchers/android-exported-component.js";
import { androidIntentInputSurface } from "./matchers/android-intent-input-surface.js";
import { androidKotlinEntrypointSurface } from "./matchers/android-kotlin-entrypoint-surface.js";
import { androidUriShareWithoutClipData } from "./matchers/android-uri-share-without-clipdata.js";
import { fileproviderBroadPath } from "./matchers/fileprovider-broad-path.js";
import { knitToolsFileUriSurface } from "./matchers/knittools-file-uri-surface.js";
import { ravelryFirebaseCallableSurface } from "./matchers/ravelry-firebase-callable-surface.js";
import { ravelryCredentialSurface } from "./matchers/ravelry-credential-surface.js";
import { sensitiveAndroidLog } from "./matchers/sensitive-android-log.js";
import { widgetMutationSurface } from "./matchers/widget-mutation-surface.js";

const here = path.dirname(fileURLToPath(import.meta.url));

function knitToolsPlugin(): DeepsecPlugin {
  return {
    name: "knittools-android",
    matchers: [
      androidExportedComponent,
      androidKotlinEntrypointSurface,
      androidIntentInputSurface,
      fileproviderBroadPath,
      knitToolsFileUriSurface,
      androidUriShareWithoutClipData,
      ravelryFirebaseCallableSurface,
      ravelryCredentialSurface,
      sensitiveAndroidLog,
      widgetMutationSurface,
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
        "Prioritize exported Android components, OAuth callback handling, FileProvider URI grants, widget mutations, Ravelry credential handling, and sensitive logging.",
      priorityPaths: [
        "app/src/main/AndroidManifest.xml",
        "app/src/main/java/com/finnvek/knittools/auth/",
        "app/src/main/java/com/finnvek/knittools/widget/",
        "app/src/main/java/com/finnvek/knittools/data/storage/",
      ],
    },
  ],
  plugins: [knitToolsPlugin()],
});
