package com.finnvek.knittools.data.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import java.time.ZoneId
import javax.inject.Inject

interface SessionTimeSource {
    fun snapshot(): SessionTimeSnapshot
}

object UnavailableBootSessionTimeSource : SessionTimeSource {
    override fun snapshot(): SessionTimeSnapshot =
        SessionTimeSnapshot(
            wallClockMillis = System.currentTimeMillis(),
            elapsedRealtimeMillis = System.nanoTime() / 1_000_000L,
            bootCount = null,
            zoneId = ZoneId.systemDefault().id,
        )
}

class AndroidSessionTimeSource
    @Inject
    constructor(
        private val context: Context,
    ) : SessionTimeSource {
        override fun snapshot(): SessionTimeSnapshot =
            SessionTimeSnapshot(
                wallClockMillis = System.currentTimeMillis(),
                elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
                bootCount = readBootCount(),
                zoneId = ZoneId.systemDefault().id,
            )

        private fun readBootCount(): Long? =
            runCatching {
                Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT).toLong()
            }.getOrNull()
    }
