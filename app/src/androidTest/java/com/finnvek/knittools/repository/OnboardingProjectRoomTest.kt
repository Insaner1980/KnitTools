package com.finnvek.knittools.repository

import android.content.Context
import android.graphics.pdf.PdfDocument
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.OnboardingProjectEntity
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.OnboardingDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OnboardingProjectRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun concurrentCreationAndPostCommitRetryReturnOneProjectWithNoUndo() =
        runBlocking {
            val database = auditDatabase(context)
            try {
                val fixture = RoomCounterTestFixture(database, context)
                val draft = OnboardingDraft("same-draft", name = "My socks", initialCount = "47", targetRows = "120")
                val repository = fixture.counterRepository()
                val results =
                    coroutineScope {
                        List(
                            4,
                        ) { async(Dispatchers.IO) { repository.createOnboardingProject(draft, false) } }.awaitAll()
                    }
                val projectId = (results.first() as ProjectCreationResult.Created).projectId
                assertTrue(results.all { it == ProjectCreationResult.Created(projectId) })
                assertEquals(1, repository.getProjectCount())
                assertEquals(projectId, fixture.counterRepository().findOnboardingProject(draft.id))
                assertFalse(repository.applyMainCounterChange(projectId, MainCounterChange.Undo))
                assertEquals(47, repository.getProject(projectId)?.count)
                assertEquals(120, repository.getProject(projectId)?.targetRows)
            } finally {
                database.close()
            }
        }

    @Test fun failureAfterCommitIsRecoveredByReceiptOnANewRepository() =
        runBlocking {
            val database = auditDatabase(context)
            try {
                val fixture = RoomCounterTestFixture(database, context)
                val afterCommit =
                    object : DatabaseTransactionRunner {
                        override suspend fun <T> run(block: suspend () -> T): T {
                            fixture.transactionRunner.run(block)
                            error("Process stopped after commit")
                        }
                    }
                val draft = OnboardingDraft("committed", name = "My scarf")
                val failed =
                    runCatching {
                        fixture
                            .counterRepository(
                                transactions = afterCommit,
                            ).createOnboardingProject(draft, false)
                    }
                assertTrue(failed.isFailure)
                val recovered = fixture.counterRepository()
                val id = recovered.findOnboardingProject(draft.id)
                assertNotNull(id)
                assertEquals(ProjectCreationResult.Created(id!!), recovered.createOnboardingProject(draft, false))
                assertEquals(1, recovered.getProjectCount())
            } finally {
                database.close()
            }
        }

    @Test fun failedReceiptRollsBackProjectAndPdfRelationsWithoutDeletingDraftFile() =
        runBlocking {
            val database = auditDatabase(context)
            try {
                val fixture = RoomCounterTestFixture(database, context)
                val directory = File(context.filesDir, "pattern_pdfs/0").apply { mkdirs() }
                val file = File.createTempFile("onboarding-test-", ".pdf", directory)
                try {
                    val pdf = PdfDocument()
                    try {
                        val page = pdf.startPage(PdfDocument.PageInfo.Builder(100, 100, 1).create())
                        pdf.finishPage(page)
                        file.outputStream().use(pdf::writeTo)
                    } finally {
                        pdf.close()
                    }
                    val failingDao =
                        object : CounterProjectDao by database.counterProjectDao() {
                            override suspend fun insertOnboardingProject(receipt: OnboardingProjectEntity) {
                                error("Receipt write failed")
                            }
                        }
                    val draft =
                        OnboardingDraft(
                            "rollback",
                            name = "My socks",
                            pdfUri =
                                android.net.Uri
                                    .fromFile(file)
                                    .toString(),
                            pdfName = "Pattern",
                        )
                    val before = database.auditSnapshot()
                    assertTrue(
                        runCatching {
                            fixture.counterRepository(projectDao = failingDao).createOnboardingProject(draft, false)
                        }.isFailure,
                    )
                    assertEquals(before, database.auditSnapshot())
                    assertTrue(file.exists())
                    val result = fixture.counterRepository().createOnboardingProject(draft, false)
                    assertTrue(result is ProjectCreationResult.Created)
                    assertEquals(
                        1,
                        database
                            .projectDocumentDao()
                            .getForProject(
                                (result as ProjectCreationResult.Created).projectId,
                            ).size,
                    )
                } finally {
                    file.delete()
                }
            } finally {
                database.close()
            }
        }
}
