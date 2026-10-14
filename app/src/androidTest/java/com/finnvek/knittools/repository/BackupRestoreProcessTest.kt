package com.finnvek.knittools.repository

import org.junit.Test

class BackupRestoreProcessTest {
    @Test fun interruptedRestoreReopensAndRecoversWithoutPartialDataOrMissingFiles() {
        checkNotNull(BackupRestoreProcessProbe.phase())
        val fixture = BackupRepositoryTest()
        fixture.setUp()
        try {
            fixture.verifyRecoveredProcessFixture()
        } finally {
            fixture.tearDown()
        }
    }
}
