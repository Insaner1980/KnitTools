import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { test } from "node:test";
import { androidExportedComponent } from "./android-exported-component.js";
import { androidIntentInputSurface } from "./android-intent-input-surface.js";
import { androidKotlinEntrypointSurface } from "./android-kotlin-entrypoint-surface.js";
import { androidUriShareWithoutClipData } from "./android-uri-share-without-clipdata.js";
import { fileproviderBroadPath } from "./fileprovider-broad-path.js";
import { knitToolsFileUriSurface } from "./knittools-file-uri-surface.js";
import { ravelryFirebaseCallableSurface } from "./ravelry-firebase-callable-surface.js";
import { sensitiveAndroidLog } from "./sensitive-android-log.js";
import { widgetMutationSurface } from "./widget-mutation-surface.js";

const productionPath = "app/src/main/java/com/finnvek/knittools/Surface.kt";

for (const [base, label] of [
  ["AppCompatActivity", "Android activity entry point"],
  ["GlanceAppWidgetReceiver", "Glance app widget receiver entry point"],
  ["BroadcastReceiver", "Android broadcast receiver entry point"],
  ["LifecycleService", "Android service entry point"],
  ["CoroutineWorker", "WorkManager background execution entry point"],
]) {
  test(`class boundaries preserve names and lines for ${base}`, () => {
    const content = [
      "class Helper {}",
      `class First : ${base}() {}`,
      "class BodylessHelper",
      "class Second(",
      "  value: String,",
      `) : android.example.${base}() {}`,
    ].join("\n");
    const matches = androidKotlinEntrypointSurface.match(content, productionPath);
    assert.deepEqual(matches.map((match) => match.matchedPattern), [label, label]);
    assert.deepEqual(matches.map((match) => match.lineNumbers), [[2], [4]]);
    assert.deepEqual(matches.map((match) =>
      content.split("\n")[match.lineNumbers[0] - 1].match(/class (\w+)/)?.[1]),
    ["First", "Second"]);
    if (base === "BroadcastReceiver") {
      assert.deepEqual(widgetMutationSurface.match(content, productionPath)
        .map((match) => match.lineNumbers), [[2], [4]]);
    }
  });
  test(`constructor lambda defaults preserve the ${base} entry point`, () => {
    const content = `class Helper {}\nclass Entry(val hook: () -> Unit = { run { } }) : ${base}() {}`;
    assert.deepEqual(androidKotlinEntrypointSurface.match(content, productionPath)
      .map((match) => [match.matchedPattern, match.lineNumbers]), [[label, [2]]]);
    if (base === "BroadcastReceiver") {
      assert.deepEqual(widgetMutationSurface.match(content, productionPath)
        .map((match) => match.lineNumbers), [[2]]);
    }
  });
  test(`function type bounds preserve the ${base} entry point`, () => {
    const content = `class Helper {}\nabstract class Entry<T : () -> Unit> : ${base}()`;
    assert.deepEqual(androidKotlinEntrypointSurface.match(content, productionPath)
      .map((match) => [match.matchedPattern, match.lineNumbers]), [[label, [2]]]);
    if (base === "BroadcastReceiver") {
      assert.deepEqual(widgetMutationSurface.match(content, productionPath)
        .map((match) => match.lineNumbers), [[2]]);
    }
  });
}

test("widget receiver matching cannot start in a previous class", () => {
  const content = "class Helper {}\r\nclass Receiver\r\n : BroadcastReceiver() {}";
  const matches = widgetMutationSurface.match(content, productionPath);
  assert.deepEqual(matches.map((match) => match.lineNumbers), [[2]]);
  assert.ok(matches[0].snippet.includes("class Receiver"));
});

test("constructor defaults still expose WorkManager scheduling", () => {
  const content = "class Scheduler(val manager: WorkManager = WorkManager.getInstance(context)) {}";
  assert.deepEqual(androidKotlinEntrypointSurface.match(content, productionPath)
    .map((match) => match.matchedPattern), ["WorkManager scheduling surface"]);
});

for (const action of ["ACTION_SEND", "ACTION_SEND_MULTIPLE"]) {
  test(`${action} keeps nested local functions inside the share boundary`, () => {
    for (const grants of ["", "addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)",
      'addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); clipData = ClipData.newRawUri("PDF", uri)']) {
      const content = `fun share(uri: Uri) = Intent(Intent.${action}).apply {
  fun title(): String {
    fun label() = "Pattern"
    return label()
  }
  putExtra(Intent.EXTRA_TITLE, title())
  putExtra(Intent.EXTRA_STREAM, uri)
  ${grants}
}
private fun later() {
  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  clipData = ClipData.newRawUri("synthetic", uri)
}`;
      const matches = androidUriShareWithoutClipData.match(content, productionPath);
      assert.deepEqual(matches.map((match) => match.lineNumbers),
        grants.includes("clipData") ? [] : [[1]]);
      if (matches.length > 0) {
        assert.equal(matches[0].matchedPattern, grants.includes("FLAG_GRANT")
          ? "EXTRA_STREAM content URI share without ClipData"
          : "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION");
      }
    }
  });
  test(`${action} ignores fun inside strings and comments at the function boundary`, () => {
    const content = `fun share(uri: Uri) = Intent(Intent.${action}).apply {
  putExtra(Intent.EXTRA_TITLE, "A fun pattern")
  // fun is a keyword, not a declaration here
  putExtra(Intent.EXTRA_STREAM, uri)
}
private fun later() {
  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  clipData = ClipData.newRawUri("synthetic", uri)
}`;
    assert.deepEqual(androidUriShareWithoutClipData.match(content, productionPath)
      .map((match) => match.lineNumbers), [[1]]);
  });
  for (const grants of ["", "addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)",
    'clipData = ClipData.newRawUri("synthetic", uri)']) {
    test(`${action} cannot borrow later function permissions: ${grants || "neither"}`, () => {
      const content = `fun share(uri: Uri) = Intent(Intent.${action}).apply {
  putExtra(Intent.EXTRA_STREAM, uri)
  ${grants}
}
private suspend fun later(uri: Uri) {
  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  clipData = ClipData.newRawUri("synthetic", uri)
}`;
      const matches = androidUriShareWithoutClipData.match(content, productionPath);
      assert.deepEqual(matches.map((match) => match.lineNumbers), [[1]]);
      assert.equal(matches[0].matchedPattern, grants.includes("FLAG_GRANT")
        ? "EXTRA_STREAM content URI share without ClipData"
        : "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION");
    });
  }
  test(`${action} with both permissions is safe`, () => {
    const content = `fun share(uri: Uri) = Intent(Intent.${action}).apply {
  putExtra(Intent.EXTRA_STREAM, uri)
  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  clipData = ClipData.newRawUri("synthetic", uri)
}`;
    assert.deepEqual(androidUriShareWithoutClipData.match(content, productionPath), []);
  });
}

test("ACTION_SENDTO and longer identifiers are not stream share actions", () => {
  for (const action of ["ACTION_SENDTO", "ACTION_SENDER", "ACTION_SEND_MULTIPLE_EXTRA"]) {
    assert.deepEqual(androidUriShareWithoutClipData.match(
      `Intent(Intent.${action}).putExtra(Intent.EXTRA_STREAM, uri)`, productionPath), []);
  }
});

for (const expression of [
  'format(normalize(value)) + syntheticToken',
  '")" + syntheticToken',
  String.raw`"escaped quote: \" )" + syntheticToken`,
  String.raw`"escaped slash: \\" + syntheticToken`,
  `'(' + format(value) + syntheticToken`,
  '"""raw ) ( " text""" + syntheticToken',
]) {
  test(`sensitive argument survives log call boundaries: ${expression}`, () => {
    const content = `android.util.Log.d(TAG, ${expression})`;
    assert.deepEqual(sensitiveAndroidLog.match(content, productionPath)
      .map((match) => match.lineNumbers), [[1]]);
  });
}

test("safe logs end before a later sensitive identifier despite strings and escapes", () => {
  const content = String.raw`Log.d(TAG, format(normalize(value)) + "( \" )" + '(' + "\\")
val syntheticToken = "synthetic-only"
Log.i(TAG, """raw ( ) " text""")`;
  assert.deepEqual(sensitiveAndroidLog.match(content, productionPath), []);
});

test("flags Android Kotlin entry points without scanning test files", () => {
  const content = `
class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) = Unit
}

class CounterWidgetActions : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) = Unit
}`;

  const matches = androidKotlinEntrypointSurface.match(
    content,
    "app/src/main/java/com/finnvek/knittools/MainActivity.kt",
  );
  const testMatches = androidKotlinEntrypointSurface.match(
    content,
    "app/src/test/java/com/finnvek/knittools/MainActivityTest.kt",
  );

  assert.deepEqual(
    matches.map((match) => match.matchedPattern).sort(),
    ["Android activity entry point", "Android broadcast receiver entry point"],
  );
  assert.deepEqual(testMatches, []);
});

test("flags intent extras and callback data reads", () => {
  const content = `
private fun handle(intent: Intent?) {
  val uri = intent?.data
  val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
  val projectId = intent?.getLongExtra(EXTRA_PROJECT_ID, 0L)
}`;

  const matches = androidIntentInputSurface.match(
    content,
    "app/src/main/java/com/finnvek/knittools/MainActivity.kt",
  );

  assert.deepEqual(
    matches.map((match) => match.matchedPattern).sort(),
    ["Android intent data URI read", "Android intent extra read", "Android text share extra read"],
  );
});

test("flags FileProvider, SAF, and content resolver file boundaries", () => {
  const content = `
fun copy(context: Context, uri: Uri) {
  context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
  context.contentResolver.openInputStream(uri)?.use { input -> input.copyTo(output) }
  AppFileStorage.openReadDescriptor(context, uri)
  FileProvider.getUriForFile(context, authority, file)
}`;

  const matches = knitToolsFileUriSurface.match(
    content,
    "app/src/main/java/com/finnvek/knittools/data/storage/AppFileStorage.kt",
  );

  assert.deepEqual(
    matches.map((match) => match.matchedPattern).sort(),
    [
      "Android FileProvider URI creation",
      "Android content resolver file read",
      "Android persistable URI permission boundary",
      "File copy boundary",
      "ParcelFileDescriptor URI read boundary",
    ],
  );
});

test("flags Firebase callable and auth boundaries for the Ravelry backend", () => {
  const content = `
class RavelryBackendClient(
  private val functions: FirebaseFunctions,
) {
  suspend fun import(url: String) {
    Firebase.auth.signInAnonymously()
    functions.getHttpsCallable("ravelryImportPatternByUrl").call(mapOf("url" to url))
  }
}`;

  const matches = ravelryFirebaseCallableSurface.match(
    content,
    "app/src/main/java/com/finnvek/knittools/data/remote/RavelryBackendClient.kt",
  );

  assert.deepEqual(
    matches.map((match) => match.matchedPattern).sort(),
    ["Firebase Auth boundary", "Firebase callable function boundary", "Ravelry backend callable name"],
  );
});

test("flags widget launch-token and persisted state write boundaries", () => {
  const tokenContent = `
object CounterLaunchTokenStore {
  internal fun consumeLaunchId(context: Context, launchId: String?): Boolean = false
}`;
  const tokenMatches = widgetMutationSurface.match(
    tokenContent,
    "app/src/main/java/com/finnvek/knittools/data/storage/CounterLaunchTokenStore.kt",
  );

  const stateContent = `
suspend fun persist(context: Context, data: WidgetData, glanceId: GlanceId) {
  CounterWidgetState.save(context, data)
  context.widgetDataStore.updatePreferencesSafely("Widget state") {}
  updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {}
}`;
  const stateMatches = widgetMutationSurface.match(
    stateContent,
    "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetState.kt",
  );

  assert.ok(
    widgetMutationSurface.filePatterns.includes(
      "app/src/main/java/com/finnvek/knittools/data/storage/CounterLaunchTokenStore.kt",
    ),
  );
  assert.deepEqual(
    tokenMatches.map((match) => match.matchedPattern),
    ["Widget launch-token trust boundary"],
  );
  assert.deepEqual(
    stateMatches.map((match) => match.matchedPattern).sort(),
    [
      "Glance widget persisted state write",
      "Widget persisted DataStore write",
      "Widget persisted state boundary",
    ],
  );
});

test("flags widget broadcasts and repository mutation surfaces", () => {
  const content = `
class CounterWidgetActions : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) = Unit
  private suspend fun apply(repository: CounterRepository, projectId: Long) {
    repository.applyWidgetCountChange(projectId, true)
  }
}
fun button(context: Context) = actionSendBroadcast(CounterWidgetActions.incrementIntent(context))`;

  const matches = widgetMutationSurface.match(
    content,
    "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt",
  );

  assert.deepEqual(
    matches.map((match) => match.matchedPattern).sort(),
    [
      "Android broadcast receiver entry point",
      "Glance widget broadcast action",
      "Widget counter repository mutation",
    ],
  );
});

test("checks every Android URI share independently", () => {
  const content = `
fun safeShare(uri: Uri) = Intent(Intent.ACTION_SEND).apply {
  putExtra(Intent.EXTRA_STREAM, uri)
  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  clipData = ClipData.newRawUri("PDF", uri)
}

fun unsafeShare(uri: Uri) = Intent(Intent.ACTION_SEND).apply {
  putExtra(Intent.EXTRA_STREAM, uri)
  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}`;

  const matches = androidUriShareWithoutClipData.match(
    content,
    "app/src/main/java/com/finnvek/knittools/ui/PatternShare.kt",
  );

  assert.deepEqual(
    matches.map((match) => match.matchedPattern),
    ["EXTRA_STREAM content URI share without ClipData"],
  );
});

test("flags broad external media FileProvider paths", () => {
  const content = `<paths><external-media-path name="media" path="." /></paths>`;

  const matches = fileproviderBroadPath.match(content, "app/src/main/res/xml/file_paths.xml");

  assert.deepEqual(
    matches.map((match) => match.matchedPattern),
    ['FileProvider path="." or equivalent broad directory'],
  );
});

test("flags each exported Android component without crossing manifest tags", () => {
  const content = `
<manifest>
  <application>
    <activity android:name=".PrivateActivity" android:exported="false" />
    <provider android:name=".ExportedProvider" android:exported="true" />
    <activity-alias android:name=".ExportedAlias" android:exported="true" />
  </application>
</manifest>`;

  const matches = androidExportedComponent.match(content, "app/src/main/AndroidManifest.xml");

  assert.equal(matches.length, 2);
  assert.deepEqual(
    matches.flatMap((match) => match.lineNumbers),
    [5, 6],
  );
});

test("flags sensitive multiline Android log calls", () => {
  const content = `
Log.w(
  TAG,
  "Ravelry token refresh failed",
  error,
)`;

  const matches = sensitiveAndroidLog.match(
    content,
    "app/src/main/java/com/finnvek/knittools/data/remote/RavelryBackendClient.kt",
  );

  assert.deepEqual(
    matches.map((match) => match.matchedPattern),
    ["Sensitive term in Android log call"],
  );
});
