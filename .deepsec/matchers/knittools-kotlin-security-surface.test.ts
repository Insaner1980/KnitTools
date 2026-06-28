import assert from "node:assert/strict";
import { test } from "node:test";
import { androidIntentInputSurface } from "./android-intent-input-surface.js";
import { androidKotlinEntrypointSurface } from "./android-kotlin-entrypoint-surface.js";
import { knitToolsFileUriSurface } from "./knittools-file-uri-surface.js";
import { ravelryFirebaseCallableSurface } from "./ravelry-firebase-callable-surface.js";
import { widgetMutationSurface } from "./widget-mutation-surface.js";

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
