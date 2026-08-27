package com.finnvek.knittools.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import coil3.EventListener
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RemotePatternImageTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val imageLoaders = mutableListOf<ImageLoader>()
    private val httpClients = mutableListOf<HttpClient>()

    @After
    fun tearDown() {
        imageLoaders.forEach { imageLoader -> imageLoader.shutdown() }
        httpClients.forEach { client -> client.close() }
    }

    @Test
    fun nullAndBlankUrlsCreateNoSlotOrNetworkRequest() {
        val engine = MockEngine { error("Invalid image URL must not start a request") }
        val imageLoader = imageLoader(engine)

        composeRule.setContent {
            MaterialTheme {
                Column {
                    RemotePatternImage(
                        imageUrl = null,
                        modifier = Modifier.size(IMAGE_SIZE).testTag(NULL_IMAGE_TAG),
                        imageLoaderProvider = { imageLoader },
                    )
                    RemotePatternImage(
                        imageUrl = "   ",
                        modifier = Modifier.size(IMAGE_SIZE).testTag(BLANK_IMAGE_TAG),
                        imageLoaderProvider = { imageLoader },
                    )
                    RemotePatternImage(
                        imageUrl = "not a valid URI",
                        modifier = Modifier.size(IMAGE_SIZE).testTag(MALFORMED_IMAGE_TAG),
                        imageLoaderProvider = { imageLoader },
                    )
                    RemotePatternImage(
                        imageUrl = "http://images.example.test/pattern.png",
                        modifier = Modifier.size(IMAGE_SIZE).testTag(HTTP_IMAGE_TAG),
                        imageLoaderProvider = { imageLoader },
                    )
                }
            }
        }

        composeRule.onNodeWithTag(NULL_IMAGE_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(BLANK_IMAGE_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(MALFORMED_IMAGE_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(HTTP_IMAGE_TAG).assertDoesNotExist()
        composeRule.runOnIdle {
            assertTrue(engine.requestHistory.isEmpty())
        }
    }

    @Test
    fun successfulHttpsRequestUsesKtorAndDoesNotWriteUserContentStorage() {
        val requestStarted = AtomicBoolean(false)
        val succeeded = AtomicBoolean(false)
        val responseGate = CompletableDeferred<Unit>()
        val beforeStorage = userContentStorageSnapshot()
        val engine =
            MockEngine { request ->
                assertEquals(URLProtocol.HTTPS, request.url.protocol)
                assertEquals("images.example.test", request.url.host)
                requestStarted.set(true)
                responseGate.await()
                respond(
                    content = onePixelPng(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Image.PNG.toString()),
                )
            }
        val imageLoader = imageLoader(engine, onSuccess = { succeeded.set(true) })

        composeRule.setContent {
            MaterialTheme {
                Column {
                    RemotePatternImage(
                        imageUrl = "  $SUCCESS_URL  ",
                        modifier = Modifier.size(IMAGE_SIZE).testTag(IMAGE_TAG),
                        imageLoaderProvider = { imageLoader },
                    )
                    Text(PATTERN_TITLE)
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = REQUEST_TIMEOUT_MILLIS) { requestStarted.get() }
        composeRule.onNodeWithTag(IMAGE_TAG).assertExists()
        composeRule
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
                useUnmergedTree = true,
            ).assertCountEquals(0)
        composeRule.runOnIdle { responseGate.complete(Unit) }
        composeRule.waitUntil(timeoutMillis = REQUEST_TIMEOUT_MILLIS) { succeeded.get() }
        composeRule.onNodeWithTag(IMAGE_TAG).assertExists()
        composeRule
            .onNodeWithTag(IMAGE_TAG)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        composeRule.onAllNodesWithText(PATTERN_TITLE).assertCountEquals(1)
        composeRule.runOnIdle {
            assertEquals(1, engine.requestHistory.size)
            assertEquals(beforeStorage, userContentStorageSnapshot())
        }
    }

    @Test
    fun failureRemovesImageSlotButKeepsTextAndActionUsable() {
        val failed = AtomicBoolean(false)
        val clicked = AtomicBoolean(false)
        val beforeStorage = userContentStorageSnapshot()
        val engine =
            MockEngine {
                respond(
                    content = "not found",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                )
            }
        val imageLoader = imageLoader(engine, onError = { failed.set(true) })

        composeRule.setContent {
            MaterialTheme {
                Column {
                    RemotePatternImage(
                        imageUrl = FAILURE_URL,
                        modifier = Modifier.size(IMAGE_SIZE).testTag(IMAGE_TAG),
                        imageLoaderProvider = { imageLoader },
                    )
                    Text(PATTERN_TITLE)
                    Text(PATTERN_DESIGNER)
                    Text(PATTERN_AVAILABILITY)
                    Button(onClick = { clicked.set(true) }) {
                        Text("Open pattern")
                    }
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = REQUEST_TIMEOUT_MILLIS) { failed.get() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(IMAGE_TAG).assertDoesNotExist()
        composeRule.onNodeWithText(PATTERN_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(PATTERN_DESIGNER).assertIsDisplayed()
        composeRule.onNodeWithText(PATTERN_AVAILABILITY).assertIsDisplayed()
        composeRule.onNodeWithText("Open pattern").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertTrue(clicked.get())
            assertEquals(1, engine.requestHistory.size)
            assertEquals(beforeStorage, userContentStorageSnapshot())
        }
    }

    @Test
    fun changingUrlResetsFailureForAReusedCompositionSlot() {
        val failures = AtomicInteger(0)
        val successes = AtomicInteger(0)
        val currentUrl = mutableStateOf(FAILURE_URL)
        val recompositionMarker = mutableStateOf(0)
        val engine =
            MockEngine { request ->
                if (request.url.encodedPath.endsWith("missing.png")) {
                    respond("missing", HttpStatusCode.NotFound)
                } else {
                    respond(
                        content = onePixelPng(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Image.PNG.toString()),
                    )
                }
            }
        val imageLoader =
            imageLoader(
                engine = engine,
                onSuccess = { successes.incrementAndGet() },
                onError = { failures.incrementAndGet() },
            )

        composeRule.setContent {
            MaterialTheme {
                Column {
                    Text("Recomposition ${recompositionMarker.value}")
                    RemotePatternImage(
                        imageUrl = currentUrl.value,
                        modifier = Modifier.size(IMAGE_SIZE).testTag(IMAGE_TAG),
                        imageLoaderProvider = { imageLoader },
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = REQUEST_TIMEOUT_MILLIS) { failures.get() == 1 }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(IMAGE_TAG).assertDoesNotExist()
        composeRule.runOnIdle { recompositionMarker.value += 1 }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Recomposition 1").assertIsDisplayed()
        composeRule.onNodeWithTag(IMAGE_TAG).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, engine.requestHistory.size) }
        composeRule.runOnIdle { currentUrl.value = SUCCESS_URL }
        composeRule.waitUntil(timeoutMillis = REQUEST_TIMEOUT_MILLIS) { successes.get() == 1 }
        composeRule.onNodeWithTag(IMAGE_TAG).assertExists()
        composeRule.runOnIdle { recompositionMarker.value += 1 }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Recomposition 2").assertIsDisplayed()
        composeRule.onNodeWithTag(IMAGE_TAG).assertExists()
        composeRule.runOnIdle {
            assertEquals(
                listOf("/missing.png", "/pattern.png"),
                engine.requestHistory.map { request -> request.url.encodedPath },
            )
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun imageLoader(
        engine: MockEngine,
        onSuccess: () -> Unit = {},
        onError: () -> Unit = {},
    ): ImageLoader {
        val client = HttpClient(engine).also(httpClients::add)
        return ImageLoader
            .Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = client))
            }.memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .eventListener(
                object : EventListener() {
                    override fun onSuccess(
                        request: ImageRequest,
                        result: SuccessResult,
                    ) {
                        onSuccess()
                    }

                    override fun onError(
                        request: ImageRequest,
                        result: ErrorResult,
                    ) {
                        onError()
                    }
                },
            ).build()
            .also(imageLoaders::add)
    }

    private fun onePixelPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun userContentStorageSnapshot(): Map<String, List<String>> =
        USER_CONTENT_DIRECTORIES.associateWith { directoryName ->
            val directory = File(context.filesDir, directoryName)
            if (!directory.exists()) {
                emptyList()
            } else {
                directory
                    .walkTopDown()
                    .filter(File::isFile)
                    .map { file -> file.relativeTo(context.filesDir).invariantSeparatorsPath }
                    .sorted()
                    .toList()
            }
        }

    private companion object {
        private const val IMAGE_TAG = "remote-pattern-image"
        private const val NULL_IMAGE_TAG = "null-remote-pattern-image"
        private const val BLANK_IMAGE_TAG = "blank-remote-pattern-image"
        private const val MALFORMED_IMAGE_TAG = "malformed-remote-pattern-image"
        private const val HTTP_IMAGE_TAG = "http-remote-pattern-image"
        private const val PATTERN_TITLE = "Pattern title"
        private const val PATTERN_DESIGNER = "Test Designer"
        private const val PATTERN_AVAILABILITY = "Availability unknown"
        private const val SUCCESS_URL = "https://images.example.test/pattern.png"
        private const val FAILURE_URL = "https://images.example.test/missing.png"
        private const val REQUEST_TIMEOUT_MILLIS = 5_000L
        private val IMAGE_SIZE = 80.dp
        private val USER_CONTENT_DIRECTORIES =
            listOf("progress_photos", "yarn_photos", "pattern_captures", "pattern_pdfs")
    }
}
