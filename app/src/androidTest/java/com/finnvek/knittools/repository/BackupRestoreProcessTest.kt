package com.finnvek.knittools.repository

import org.junit.Test

class BackupRestoreProcessTest {
    @Test fun interruptedRestoreReopensAndRecoversWithoutPartialDataOrMissingFiles() {
        org.junit.Assume.assumeNotNull(BackupRestoreProcessProbe.phase())
        val fixture = BackupRepositoryTest()
        fixture.setUp()
        try {
            fixture.verifyRecoveredProcessFixture()
        } finally {
            fixture.tearDown()
        }
    }
}
