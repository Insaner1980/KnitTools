package com.finnvek.knittools.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

internal interface BackupProviderIo {
    suspend fun read(
        source: Uri,
        target: File,
        maxBytes: Long,
        check: () -> Unit = {},
    )

    suspend fun write(
        source: File,
        destination: Uri,
        maxBytes: Long,
        check: () -> Unit = {},
    )
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupProviderIoModule {
    @Binds
    abstract fun bindBackupProviderIo(implementation: ContentResolverBackupProviderIo): BackupProviderIo
}

@Singleton
internal class ContentResolverBackupProviderIo private constructor(
    private val endpoint: BackupProviderEndpoint,
    private val noProgressTimeoutMillis: Long,
    private val workerExecutor: ExecutorService,
    private val watchdogExecutor: ScheduledExecutorService,
    private val descriptorCloserExecutor: ExecutorService,
) : BackupProviderIo {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        AndroidBackupProviderEndpoint(context.contentResolver),
        NO_PROGRESS_TIMEOUT_MILLIS,
        worker,
        watchdog,
        descriptorCloser,
    )

    internal constructor(
        endpoint: BackupProviderEndpoint,
        noProgressTimeoutMillis: Long,
    ) : this(endpoint, noProgressTimeoutMillis, worker, watchdog, descriptorCloser)

    override suspend fun read(
        source: Uri,
        target: File,
        maxBytes: Long,
        check: () -> Unit,
    ) {
        transfer(endpoint.request(source, "r")) { handle, deadline ->
            val input = handle.inputStream()
            FileOutputStream(target).use { output ->
                copyFromProvider(input, output, maxBytes, deadline, check)
            }
            deadline.call { handle.checkError() }
        }
    }

    override suspend fun write(
        source: File,
        destination: Uri,
        maxBytes: Long,
        check: () -> Unit,
    ) {
        transfer(endpoint.request(destination, "wt")) { handle, deadline ->
            val output = handle.outputStream()
            FileInputStream(source).use { input ->
                copyToProvider(input, output, maxBytes, deadline, check)
            }
            deadline.call { output.flush() }
            deadline.call { handle.checkError() }
        }
    }

    private suspend fun transfer(
        request: BackupProviderRequest,
        block: (BackupProviderHandle, ProviderDeadline) -> Unit,
    ) {
        val completion = CompletableDeferred<Result<Unit>>()
        val deadline =
            ProviderDeadline(
                request,
                noProgressTimeoutMillis,
                watchdogExecutor,
                descriptorCloserExecutor,
            ) { failure -> completion.complete(Result.failure(failure)) }
        workerExecutor.execute {
            var handle: BackupProviderHandle? = null
            var failure =
                runCatching {
                    handle = deadline.call { request.open() } ?: throw IOException("Provider returned no descriptor")
                    deadline.markOpened()
                    block(checkNotNull(handle), deadline)
                }.exceptionOrNull()?.let(deadline::classify)
            val closeFailure =
                runCatching {
                    handle?.let { deadline.call { it.close() } }
                }.exceptionOrNull()?.let(deadline::classify)
            if (closeFailure != null) {
                failure?.addSuppressed(closeFailure) ?: run { failure = closeFailure }
            }
            deadline.finish()
            completion.complete(failure?.let { Result.failure(it) } ?: Result.success(Unit))
        }
        try {
            completion.await().getOrThrow()
        } catch (cancelled: CancellationException) {
            deadline.cancel(cancelled)
            withContext(NonCancellable) { completion.await() }
            throw cancelled
        }
    }

    private fun copyFromProvider(
        input: InputStream,
        output: OutputStream,
        maxBytes: Long,
        deadline: ProviderDeadline,
        check: () -> Unit,
    ) {
        var total = 0L
        var resetDeadline = true
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        while (true) {
            check()
            val count = deadline.call(resetDeadline) { input.read(buffer) }
            if (count < 0) return
            if (count == 0) {
                resetDeadline = false
                continue
            }
            resetDeadline = true
            total += count
            BackupFormat.requireValid(total <= maxBytes)
            output.write(buffer, 0, count)
        }
    }

    private fun copyToProvider(
        input: InputStream,
        output: OutputStream,
        maxBytes: Long,
        deadline: ProviderDeadline,
        check: () -> Unit,
    ) {
        var total = 0L
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        while (true) {
            check()
            val count = input.read(buffer)
            if (count < 0) return
            total += count
            BackupFormat.requireValid(total <= maxBytes)
            deadline.call { output.write(buffer, 0, count) }
        }
    }

    companion object {
        private const val NO_PROGRESS_TIMEOUT_MILLIS = 30_000L
        private const val COPY_BUFFER_SIZE = 64 * 1024
        private val threadNumber = AtomicInteger()
        private val threadFactory =
            java.util.concurrent.ThreadFactory { task ->
                Thread(task, "backup-provider-${threadNumber.incrementAndGet()}").apply { isDaemon = true }
            }
        private val worker = Executors.newCachedThreadPool(threadFactory)
        private val descriptorCloser = Executors.newCachedThreadPool(threadFactory)
        private val watchdog =
            java.util.concurrent.ScheduledThreadPoolExecutor(1, threadFactory).apply {
                removeOnCancelPolicy = true
            }
    }
}

internal interface BackupProviderEndpoint {
    fun request(
        uri: Uri,
        mode: String,
    ): BackupProviderRequest
}

internal interface BackupProviderRequest {
    fun open(): BackupProviderHandle?

    fun cancelOpen()

    fun closeDescriptor()
}

internal interface BackupProviderHandle : Closeable {
    fun inputStream(): InputStream

    fun outputStream(): OutputStream

    fun checkError() = Unit
}

private class AndroidBackupProviderEndpoint(
    private val resolver: ContentResolver,
) : BackupProviderEndpoint {
    override fun request(
        uri: Uri,
        mode: String,
    ): BackupProviderRequest = AndroidBackupProviderRequest(resolver, uri, mode)
}

private class AndroidBackupProviderRequest(
    private val resolver: ContentResolver,
    private val uri: Uri,
    private val mode: String,
) : BackupProviderRequest {
    private val signal = CancellationSignal()
    private val cancelled = AtomicBoolean()
    private val descriptor = AtomicReference<ParcelFileDescriptor?>()

    override fun open(): BackupProviderHandle? {
        val opened = resolver.openFileDescriptor(uri, mode, signal) ?: return null
        descriptor.set(opened)
        if (cancelled.get()) {
            opened.close()
            throw CancellationException("Provider operation was cancelled")
        }
        return AndroidBackupProviderHandle(opened)
    }

    override fun cancelOpen() {
        cancelled.set(true)
        signal.cancel()
    }

    override fun closeDescriptor() {
        cancelled.set(true)
        try {
            descriptor.get()?.close()
        } catch (_: IOException) {
            // Sulkemisen tarkoitus on vapauttaa blokkaava provider-kutsu; alkuperäinen virhe säilyy.
        }
    }
}

private class AndroidBackupProviderHandle(
    private val descriptor: ParcelFileDescriptor,
) : BackupProviderHandle {
    override fun inputStream(): InputStream = ParcelFileDescriptor.AutoCloseInputStream(descriptor)

    override fun outputStream(): OutputStream = ParcelFileDescriptor.AutoCloseOutputStream(descriptor)

    override fun checkError() = descriptor.checkError()

    override fun close() = descriptor.close()
}

private class ProviderNoProgressException(
    cause: Throwable? = null,
) : IOException("Provider made no progress", cause)

internal class ProviderDeadline(
    private val request: BackupProviderRequest,
    timeoutMillis: Long,
    private val scheduler: ScheduledExecutorService,
    private val descriptorCloser: ExecutorService,
    private val onOpenTimeout: (Throwable) -> Unit,
) {
    private val timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
    private var deadlineNanos = 0L
    private var scheduled: ScheduledFuture<*>? = null
    private var opening = true
    private var timedOut = false
    private var cancelled: CancellationException? = null
    private var finished = false

    fun <T> call(
        resetDeadline: Boolean = true,
        block: () -> T,
    ): T {
        arm(resetDeadline)
        val result = runCatching(block)
        pause()
        result.exceptionOrNull()?.let { throw classify(it) }
        throwIfStopped()
        return result.getOrThrow()
    }

    @Synchronized
    fun markOpened() {
        opening = false
        throwIfStopped()
    }

    @Synchronized
    fun classify(failure: Throwable): Throwable =
        when {
            cancelled != null -> checkNotNull(cancelled)
            timedOut -> ProviderNoProgressException(failure)
            else -> failure
        }

    fun cancel(cause: CancellationException) {
        val completeOpening: Boolean
        synchronized(this) {
            if (finished || cancelled != null) return
            cancelled = cause
            scheduled?.cancel(false)
            scheduled = null
            completeOpening = opening
        }
        abortProvider()
        if (completeOpening) onOpenTimeout(cause)
    }

    @Synchronized
    fun finish() {
        finished = true
        scheduled?.cancel(false)
        scheduled = null
    }

    @Synchronized
    private fun arm(resetDeadline: Boolean) {
        throwIfStopped()
        if (resetDeadline || deadlineNanos == 0L) deadlineNanos = System.nanoTime() + timeoutNanos
        scheduled?.cancel(false)
        val delay = (deadlineNanos - System.nanoTime()).coerceAtLeast(0L)
        scheduled = scheduler.schedule(::expire, delay, TimeUnit.NANOSECONDS)
    }

    @Synchronized
    private fun pause() {
        scheduled?.cancel(false)
        scheduled = null
    }

    private fun expire() {
        val completeOpening: Boolean
        synchronized(this) {
            if (finished || timedOut || cancelled != null) return
            timedOut = true
            scheduled = null
            completeOpening = opening
        }
        abortProvider()
        if (completeOpening) onOpenTimeout(ProviderNoProgressException())
    }

    private fun abortProvider() {
        descriptorCloser.execute { request.closeDescriptor() }
        descriptorCloser.execute { request.cancelOpen() }
    }

    @Synchronized
    private fun throwIfStopped() {
        cancelled?.let { throw it }
        if (timedOut) throw ProviderNoProgressException()
    }
}
