package com.finnvek.knittools.repository
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProjectYarnNoteDao
import com.finnvek.knittools.data.local.ProjectYarnNoteEntity
import com.finnvek.knittools.data.local.ProjectYarnUsageDao
import com.finnvek.knittools.data.local.ProjectYarnUsageEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.TrialState
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Each parameter exercises the concrete competitor and owning write of its numbered prompt. */
@RunWith(Parameterized::class)
class AuditPart07YarnBoundaryTest(
    private val prompt: Int,
) {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: KnitToolsDatabase
    private lateinit var gate: YarnBoundaryTransactions
    private lateinit var yarn: YarnCardRepository
    private lateinit var notes: ProjectYarnNoteRepository
    private lateinit var usage: ProjectYarnUsageRepository
    private lateinit var pro: ProManager
    private lateinit var counter: CounterRepository
    private lateinit var photo: File
    private lateinit var draft: YarnCard
    private lateinit var originalNote: ProjectYarnNote
    private var usageId = 0L
    private var revision = 0L
    private var initialSnapshot = emptyMap<String, List<List<String?>>>()
    private val amounts = YarnUsageAmounts(1200.0, 600.0, 350.0, 200.0, 100.0)
    private val operation = prompt - (prompt - 1059) % 4
    private val boundary = (prompt - 1059) % 4

    @Before
    fun setUp() =
        runBlocking {
            check(context.packageName == "com.finnvek.knittools.activepolicytest")
            database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
            gate = YarnBoundaryTransactions(RoomDatabaseTransactionRunner(database))
            pro = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).proManager()
            pro.initialize()
            setTrial(true)
            val cardDao = database.yarnCardDao()
            val cards =
                object : YarnCardDao by cardDao {
                    override suspend fun upsert(card: YarnCardEntity): Long = gate.write { cardDao.upsert(card) }

                    override suspend fun updateQuantity(
                        id: Long,
                        quantity: Int,
                    ): Int = gate.write { cardDao.updateQuantity(id, quantity) }

                    override suspend fun updateStatus(
                        id: Long,
                        status: String,
                    ): Int = gate.single { cardDao.updateStatus(id, status) }

                    override suspend fun updateLinkedProjectId(
                        id: Long,
                        projectId: Long?,
                    ): Int = gate.write { cardDao.updateLinkedProjectId(id, projectId) }

                    override suspend fun deleteByIds(ids: List<Long>) = gate.write { cardDao.deleteByIds(ids) }
                }
            val noteDao = database.projectYarnNoteDao()
            val noteWrites =
                object : ProjectYarnNoteDao by noteDao {
                    override suspend fun upsert(note: ProjectYarnNoteEntity): Long = gate.write { noteDao.upsert(note) }

                    override suspend fun update(note: ProjectYarnNoteEntity): Int = gate.write { noteDao.update(note) }

                    override suspend fun delete(id: Long) = gate.write { noteDao.delete(id) }

                    override suspend fun updateSavedYarnCardId(
                        id: Long,
                        savedYarnCardId: Long,
                        updatedAt: Long,
                    ): Int = gate.write { noteDao.updateSavedYarnCardId(id, savedYarnCardId, updatedAt) }
                }
            val usageDao = database.projectYarnUsageDao()
            val usageWrites =
                object : ProjectYarnUsageDao by usageDao {
                    override suspend fun insert(usage: ProjectYarnUsageEntity): Long =
                        gate.write { usageDao.insert(usage) }

                    override suspend fun update(usage: ProjectYarnUsageEntity): Int =
                        gate.write { usageDao.update(usage) }
                }
            yarn =
                YarnCardRepository(cards, database.counterProjectDao(), context, gate, Dispatchers.IO, proManager = pro)
            notes = ProjectYarnNoteRepository(noteWrites, yarn, gate, usageWrites)
            usage = ProjectYarnUsageRepository(usageWrites, noteWrites, cards, gate, Dispatchers.IO)
            counter = RoomCounterTestFixture(database, context).counterRepository()
            database.counterProjectDao().insert(CounterProjectEntity(id = 1, name = "Owner", count = 7, updatedAt = 10))
            database.counterProjectDao().insert(
                CounterProjectEntity(id = 2, name = "Destination", count = 11, updatedAt = 20),
            )
            database.counterProjectDao().insert(
                CounterProjectEntity(id = 3, name = "Untouched", count = 13, updatedAt = 30),
            )
            photo = File(context.filesDir, "yarn_photos/3/audit-part07-$prompt.jpg")
            requireNotNull(photo.parentFile).mkdirs()
            photo.writeBytes(byteArrayOf(1, 3, 5, 7))
            if (operation != 1059) {
                database.yarnCardDao().upsert(
                    YarnCardEntity(
                        id = 3,
                        yarnName = "Original",
                        quantityInStash = 9,
                        photoUri = Uri.fromFile(photo).toString(),
                    ),
                )
                assertTrue(yarn.updateLinkedProjectId(3, 1))
                draft = requireNotNull(yarn.getCard(3)).copy(yarnName = "Edited descriptive name")
            }
            if (operation in listOf(1083, 1091, 1095, 1099, 1103, 1107)) {
                database.projectYarnNoteDao().upsert(
                    ProjectYarnNoteEntity(
                        id = 4,
                        projectId = 1,
                        name = "Note source",
                        savedYarnCardId = if (operation in listOf(1083, 1103, 1107)) 3 else null,
                    ),
                )
            }
            if (operation == 1091) originalNote = requireNotNull(noteDao.getById(4)).toDomain()
            if (operation in listOf(1067, 1083, 1095, 1099, 1107)) {
                val source =
                    if (operation ==
                        1067
                    ) {
                        YarnUsageSource(yarnCardId = 3)
                    } else {
                        YarnUsageSource(projectYarnNoteId = 4)
                    }
                val created = usage.create(1, source, amounts, "Fallback") as YarnUsageResult.Created
                usageId = created.usage.id
                revision = created.usage.updatedAt
            }
            initialSnapshot = database.auditSnapshot()
        }

    @After
    fun tearDown() =
        runBlocking {
            if (::database.isInitialized) database.close()
            if (::photo.isInitialized) photo.delete()
            if (::pro.isInitialized) setTrial(true)
        }

    @Test
    fun numberedBoundary() =
        runBlocking {
            withTimeout(30_000) {
                when (boundary) {
                    0 -> admission()
                    1 -> failure()
                    2 -> overlap()
                    3 -> cancellation()
                }
            }
        }

    private suspend fun setTrial(active: Boolean) {
        ProPolicyTestControl.setPurchased(false)
        ProPolicyTestControl.setTrialState(
            TrialState(isActive = active, daysRemaining = if (active) 1 else 0, hasStarted = true),
        )
        withTimeout(10_000) { pro.proState.first { it.hasFeature(ProFeature.UNLIMITED_YARN) == active } }
    }

    private suspend fun mutate(): Any? =
        when (operation) {
            1059 -> yarn.saveCard(YarnCard(yarnName = "Created", quantityInStash = 2, linkedProjectId = 1))
            1063 -> yarn.saveCard(draft)
            1067 -> yarn.changeQuantity(3, 1)
            1071 -> yarn.updateStatus(3, YarnCardStatus.FINISHED)
            1075 -> yarn.updateLinkedProjectId(3, 2)
            1083 -> yarn.deleteCard(3)
            1087 -> notes.save(ProjectYarnNote(projectId = 1, name = "New project note", quantity = 2))
            1091 -> notes.edit(originalNote, originalNote.copy(name = "Edited project note"))
            1095 -> notes.saveToMyYarn(4)
            1099 -> notes.delete(4)
            1103 -> usage.create(1, YarnUsageSource(yarnCardId = 3), amounts, "Fallback")
            1107 -> usage.update(1, usageId, revision, amounts.copy(usedMeters = 900.0))
            else -> error("Unknown yarn operation")
        }

    private suspend fun competitor(): Any? =
        when (operation) {
            1059 -> {
                setTrial(false)
            }

            1063 -> {
                assertTrue(yarn.changeQuantity(3, 2))
                assertTrue(yarn.updateLinkedProjectId(3, 2))
                setTrial(false)
            }

            1067 -> {
                usage.update(1, usageId, revision, amounts.copy(usedMeters = 700.0))
            }

            1071 -> {
                yarn.updateLinkedProjectId(3, 2)
            }

            1075 -> {
                counter.deleteProject(2)
            }

            1083 -> {
                notes.saveToMyYarn(4)
            }

            1087 -> {
                counter.deleteProject(1)
            }

            1091 -> {
                notes.saveToMyYarn(4)
            }

            1095 -> {
                notes.saveToMyYarn(4)
            }

            1099 -> {
                notes.saveToMyYarn(4)
            }

            1103 -> {
                usage.create(1, YarnUsageSource(projectYarnNoteId = 4), amounts.copy(usedMeters = 700.0), "Alias")
            }

            1107 -> {
                usage.update(1, usageId, revision, amounts.copy(usedMeters = 700.0))
            }

            else -> {
                error("No reachable owner")
            }
        }

    private suspend fun admission() =
        coroutineScope {
            gate.arm(Phase.BEFORE_READ)
            val action = async(Dispatchers.IO) { runCatching { mutate() } }
            gate.reached.await()
            competitor()
            val competing = database.auditSnapshot()
            gate.release.complete(Unit)
            val result = action.await()
            when (operation) {
                1059 -> {
                    assertNull(result.getOrThrow())
                    assertEquals(competing, database.auditSnapshot())
                    assertEquals(0, yarn.getCardCount().first())
                }

                1063 -> {
                    assertEquals(3L, result.getOrThrow())
                    assertEquals("Edited descriptive name", requireNotNull(yarn.getCard(3)).yarnName)
                    assertEquals(11, requireNotNull(yarn.getCard(3)).quantityInStash)
                    assertEquals(2L, requireNotNull(yarn.getCard(3)).linkedProjectId)
                    assertFalse(pro.hasFeature(ProFeature.UNLIMITED_YARN))
                }

                1067 -> {
                    assertEquals(true, result.getOrThrow())
                    assertEquals(10, requireNotNull(yarn.getCard(3)).quantityInStash)
                    assertEquals(700.0, requireNotNull(usageRow()).usedMeters)
                }

                1071 -> {
                    assertEquals(true, result.getOrThrow())
                    assertEquals(YarnCardStatus.FINISHED, requireNotNull(yarn.observeCard(3).first()).status)
                    assertEquals(2L, requireNotNull(yarn.getCard(3)).linkedProjectId)
                }

                1075 -> {
                    assertEquals(false, result.getOrThrow())
                    assertEquals(competing, database.auditSnapshot())
                }

                1083 -> {
                    assertEquals(true, result.getOrThrow())
                    assertDeletedCard()
                }

                1087 -> {
                    assertTrue(result.isFailure)
                    assertEquals(competing, database.auditSnapshot())
                    assertNull(database.counterProjectDao().getProject(1))
                }

                1095 -> {
                    assertEquals(
                        requireNotNull(database.projectYarnNoteDao().getById(4)).savedYarnCardId,
                        result.getOrThrow(),
                    )
                    assertEquals(competing, database.auditSnapshot())
                    assertConverted()
                }

                1091 -> {
                    assertEquals(ProjectYarnNoteEditResult.Saved, result.getOrThrow())
                    assertEditedNote()
                    val cardId = requireNotNull(database.projectYarnNoteDao().getById(4)?.savedYarnCardId)
                    assertEquals("Note source", requireNotNull(yarn.getCard(cardId)).yarnName)
                }

                1099 -> {
                    result.getOrThrow()
                    assertDeletedNote()
                    assertNotNull(database.yarnCardDao().getCard(requireNotNull(requireNotNull(usageRow()).yarnCardId)))
                }

                1103 -> {
                    assertTrue(result.getOrThrow() is YarnUsageResult.AlreadyExists)
                    assertEquals(competing, database.auditSnapshot())
                    assertEquals(700.0, usageRows().single().usedMeters)
                }

                1107 -> {
                    assertEquals(YarnUsageResult.StaleAction, result.getOrThrow())
                    assertEquals(competing, database.auditSnapshot())
                    assertEquals(700.0, requireNotNull(usageRow()).usedMeters)
                }
            }
            assertReciprocal()
        }

    private suspend fun failure() {
        val before = database.auditSnapshot()
        val bytes = photo.readBytes().toList()
        val (event, table) =
            when (operation) {
                1059 -> "UPDATE" to "counter_projects"
                1063, 1067, 1071, 1075 -> "UPDATE" to "yarn_cards"
                1083 -> "DELETE" to "yarn_cards"
                1087 -> "INSERT" to "project_yarn_notes"
                1091 -> "UPDATE" to "project_yarn_notes"
                1095 -> "UPDATE" to "project_yarn_usage"
                1099 -> "DELETE" to "project_yarn_notes"
                1103 -> "INSERT" to "project_yarn_usage"
                else -> error("No failure scenario")
            }
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER audit_yarn_fail AFTER $event ON $table " +
                "BEGIN SELECT RAISE(ABORT, 'audit-owning-write'); END",
        )
        val result = runCatching { mutate() }
        assertTrue(
            "Owning SQL trigger must reject delivery",
            result.isFailure || result.getOrNull() == YarnUsageResult.PersistenceFailure,
        )
        assertEquals("Every persisted table rolls back", before, database.auditSnapshot())
        assertEquals("Failure preserves original private photo", bytes, photo.readBytes().toList())
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER audit_yarn_fail")
        assertSuccess(mutate())
        assertReciprocal()
    }

    private suspend fun overlap() =
        coroutineScope {
            // Trial expiry does not use SQLite: stop before its authoritative admission read.
            if (operation == 1059) {
                gate.arm(Phase.BEFORE_READ)
                val action = async(Dispatchers.IO) { mutate() }
                gate.reached.await()
                competitor()
                val before = database.auditSnapshot()
                gate.release.complete(Unit)
                assertNull(action.await())
                assertEquals(before, database.auditSnapshot())
                return@coroutineScope
            }
            gate.arm(Phase.BEFORE_WRITE)
            val action = async(Dispatchers.IO) { mutate() }
            gate.reached.await()
            val attempted = CompletableDeferred<Unit>()
            val competing =
                async(Dispatchers.IO) {
                    attempted.complete(Unit)
                    competitor()
                }
            attempted.await()
            if (operation == 1071) {
                // Status is a single column SQL write; the relink can finish before it.
                assertEquals(true, competing.await())
            } else {
                assertFalse("Concurrent writer waits for the open Room transaction", competing.isCompleted)
            }
            gate.release.complete(Unit)
            assertSuccess(action.await(), verifyState = false)
            val other = competing.await()
            when (operation) {
                1063 -> {
                    assertEquals(11, requireNotNull(yarn.getCard(3)).quantityInStash)
                    assertEquals("Edited descriptive name", requireNotNull(yarn.getCard(3)).yarnName)
                    assertEquals(2L, requireNotNull(yarn.getCard(3)).linkedProjectId)
                }

                1067 -> {
                    assertTrue(other is YarnUsageResult.Updated)
                    assertEquals(700.0, requireNotNull(usageRow()).usedMeters)
                    assertEquals(10, requireNotNull(yarn.getCard(3)).quantityInStash)
                }

                1071 -> {
                    assertEquals(YarnCardStatus.FINISHED, requireNotNull(yarn.getCard(3)).status)
                    assertEquals(2L, requireNotNull(yarn.getCard(3)).linkedProjectId)
                    assertEquals(9, requireNotNull(yarn.getCard(3)).quantityInStash)
                }

                1075 -> {
                    assertNull(database.counterProjectDao().getProject(2))
                    assertNull(requireNotNull(yarn.getCard(3)).linkedProjectId)
                }

                1083 -> {
                    assertNotNull(other)
                    assertNull(yarn.getCard(3))
                    assertEquals(other, requireNotNull(database.projectYarnNoteDao().getById(4)).savedYarnCardId)
                    assertEquals(other, requireNotNull(usageRow()).yarnCardId)
                    assertEquals(amounts, requireNotNull(usageRow()).toDomain().amounts)
                }

                1087 -> {
                    assertNull(database.counterProjectDao().getProject(1))
                    assertTrue(
                        database
                            .projectYarnNoteDao()
                            .observeForProject(1)
                            .first()
                            .isEmpty(),
                    )
                }

                1095 -> {
                    assertEquals(requireNotNull(database.projectYarnNoteDao().getById(4)).savedYarnCardId, other)
                    assertConverted()
                }

                1091 -> {
                    assertEditedNote()
                    assertEquals(other, database.projectYarnNoteDao().getById(4)?.savedYarnCardId)
                    assertEquals(
                        "Edited project note",
                        requireNotNull(yarn.getCard(requireNotNull(other as? Long))).yarnName,
                    )
                }

                1099 -> {
                    assertNull(other)
                    assertDeletedNote()
                }

                1103 -> {
                    assertTrue(other is YarnUsageResult.AlreadyExists)
                    assertEquals(amounts, usageRows().single().toDomain().amounts)
                }
            }
            assertReciprocal()
        }

    private suspend fun cancellation() =
        coroutineScope {
            val before = database.auditSnapshot()
            val bytes = photo.readBytes().toList()
            gate.arm(Phase.BEFORE_COMMIT)
            val cancelled = async(Dispatchers.IO) { mutate() }
            gate.reached.await()
            cancelled.cancelAndJoin()
            assertTrue(cancelled.isCancelled)
            assertEquals("Cancellation before owning commit preserves all rows", before, database.auditSnapshot())
            assertEquals(bytes, photo.readBytes().toList())
            gate.arm(Phase.AFTER_COMMIT)
            val late = async(Dispatchers.IO) { mutate() }
            gate.reached.await()
            val durable = database.auditSnapshot()
            assertFalse("The authoritative commit completed", before == durable)
            late.cancelAndJoin()
            assertTrue(late.isCancelled)
            assertEquals("Late completion cannot undo the commit", durable, database.auditSnapshot())
            if (operation == 1083) {
                // Cancellation at the return boundary can leave a committed deletion's orphan.
                // The app's restart cleanup is the recovery owner, and must retain no referenced data.
                yarn.pruneUnreferencedPhotoFiles()
                assertDeletedCard()
            } else {
                assertEquals(bytes, photo.readBytes().toList())
            }
            assertSuccessState()
            assertReciprocal()
        }

    private suspend fun assertSuccess(
        result: Any?,
        verifyState: Boolean = true,
    ) {
        when (operation) {
            1059, 1087, 1095 -> assertTrue(result is Long && result > 0)
            1063 -> assertEquals(3L, result)
            1067, 1071, 1075, 1083 -> assertEquals(true, result)
            1099 -> assertEquals(Unit, result)
            1091 -> assertEquals(ProjectYarnNoteEditResult.Saved, result)
            1103 -> assertTrue(result is YarnUsageResult.Created)
        }
        if (verifyState) assertSuccessState()
    }

    private suspend fun assertSuccessState() {
        when (operation) {
            1059 -> {
                val card = yarn.getAllCards().first().single()
                assertEquals("Created", card.yarnName)
                assertEquals(2, card.quantityInStash)
                assertEquals(1L, card.linkedProjectId)
            }

            1063 -> {
                assertEquals("Edited descriptive name", requireNotNull(yarn.getCard(3)).yarnName)
                assertEquals(Uri.fromFile(photo).toString(), requireNotNull(yarn.getCard(3)).photoUri)
            }

            1067 -> {
                assertEquals(10, requireNotNull(yarn.getCard(3)).quantityInStash)
            }

            1071 -> {
                assertEquals(YarnCardStatus.FINISHED, requireNotNull(yarn.observeCard(3).first()).status)
                assertEquals(9, requireNotNull(yarn.getCard(3)).quantityInStash)
            }

            1075 -> {
                assertEquals(2L, requireNotNull(yarn.getCard(3)).linkedProjectId)
            }

            1083 -> {
                assertNull(yarn.getCard(3))
            }

            1087 -> {
                assertEquals(
                    "New project note",
                    database
                        .projectYarnNoteDao()
                        .observeForProject(1)
                        .first()
                        .single()
                        .name,
                )
                assertEquals(1, yarn.getCardCount().first())
                assertTrue(usageRows().isEmpty())
            }

            1095 -> {
                assertConverted()
            }

            1091 -> {
                assertEditedNote()
            }

            1099 -> {
                assertDeletedNote()
            }

            1103 -> {
                assertEquals(1, usageRows().size)
                assertEquals(amounts, usageRows().single().toDomain().amounts)
                assertEquals(9, requireNotNull(yarn.getCard(3)).quantityInStash)
            }
        }
    }

    private suspend fun assertConverted() {
        val note = requireNotNull(database.projectYarnNoteDao().getById(4))
        val card = requireNotNull(yarn.getCard(requireNotNull(note.savedYarnCardId)))
        assertEquals("Note source", note.name)
        assertEquals(note.name, card.yarnName)
        assertEquals(1L, card.linkedProjectId)
        assertEquals(YarnCardStatus.IN_USE, card.status)
        assertEquals(2, yarn.getCardCount().first())
        assertEquals(1, usageRows().size)
        assertEquals(card.id, requireNotNull(usageRow()).yarnCardId)
        assertEquals(4L, requireNotNull(usageRow()).projectYarnNoteId)
        assertEquals(amounts, requireNotNull(usageRow()).toDomain().amounts)
    }

    private suspend fun assertEditedNote() {
        val edited = requireNotNull(database.projectYarnNoteDao().getById(4)).toDomain()
        assertEquals("Edited project note", edited.name)
        assertEquals(
            originalNote.copy(
                name = edited.name,
                updatedAt = edited.updatedAt,
                savedYarnCardId = edited.savedYarnCardId,
            ),
            edited,
        )
        assertTrue(usageRows().isEmpty())
    }

    private suspend fun assertDeletedCard() {
        assertNull(yarn.getCard(3))
        assertNull(requireNotNull(usageRow()).yarnCardId)
        assertEquals(4L, requireNotNull(usageRow()).projectYarnNoteId)
        assertEquals(amounts, requireNotNull(usageRow()).toDomain().amounts)
        assertEquals("Note source", requireNotNull(usageRow()).sourceNameSnapshot)
        assertFalse(photo.exists())
    }

    private suspend fun assertDeletedNote() {
        assertNull(database.projectYarnNoteDao().getById(4))
        assertNull(requireNotNull(usageRow()).projectYarnNoteId)
        assertEquals(amounts, requireNotNull(usageRow()).toDomain().amounts)
        assertEquals("Note source", requireNotNull(usageRow()).sourceNameSnapshot)
    }

    private suspend fun usageRow() = database.projectYarnUsageDao().getById(usageId)

    private suspend fun usageRows() =
        database
            .projectYarnUsageDao()
            .getProject(1)
            ?.usages
            .orEmpty()
            .map { it.usage }

    private suspend fun assertReciprocal() {
        val cards = yarn.getAllCards().first()
        val projects = database.counterProjectDao().getAllProjectsOnce()
        cards.forEach { card ->
            val owners = projects.filter { card.id.toString() in it.yarnCardIds.split(',') }.map { it.id }
            assertEquals(card.linkedProjectId?.let { listOf(it) }.orEmpty(), owners)
        }
        projects.forEach { project ->
            project.yarnCardIds.split(',').filter { it.isNotEmpty() }.forEach { id ->
                assertEquals(project.id, cards.single { it.id == id.toLong() }.linkedProjectId)
            }
        }
        assertEquals(13, requireNotNull(database.counterProjectDao().getProject(3)).count)
        assertEquals(30L, requireNotNull(database.counterProjectDao().getProject(3)).updatedAt)
        val current = database.auditSnapshot()
        val mutableTables = setOf("counter_projects", "yarn_cards", "project_yarn_notes", "project_yarn_usage")
        initialSnapshot.filterKeys { it !in mutableTables }.forEach { (table, rows) ->
            assertEquals("Unrelated table $table", rows, current[table])
        }
        if (operation in listOf(1067, 1103, 1107)) {
            assertEquals(
                "Usage and stash amounts do not mutate project counters/timestamps",
                initialSnapshot["counter_projects"],
                current["counter_projects"],
            )
        }
    }

    private enum class Phase { BEFORE_READ, BEFORE_WRITE, BEFORE_COMMIT, AFTER_COMMIT }

    private class YarnBoundaryTransactions(
        private val room: DatabaseTransactionRunner,
    ) : BoundaryPause<Phase>(),
        DatabaseTransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T {
            val selected = phase.get().takeUnless { it == Phase.BEFORE_WRITE }
            if (selected != null) phase.compareAndSet(selected, null)
            if (selected == Phase.BEFORE_READ) pause()
            val result =
                room.run {
                    val value = block()
                    if (selected == Phase.BEFORE_COMMIT) pause()
                    value
                }
            if (selected == Phase.AFTER_COMMIT) pause()
            return result
        }

        suspend fun <T> write(block: suspend () -> T): T {
            if (phase.compareAndSet(Phase.BEFORE_WRITE, null)) pause()
            return block()
        }

        suspend fun <T> single(block: suspend () -> T): T {
            val selected = phase.getAndSet(null)
            if (selected in listOf(Phase.BEFORE_READ, Phase.BEFORE_WRITE, Phase.BEFORE_COMMIT)) pause()
            val result = block()
            if (selected == Phase.AFTER_COMMIT) pause()
            return result
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "prompt_{0}")
        fun prompts(): List<Array<Int>> = ((1059..1078) + (1083..1107)).map { arrayOf(it) }
    }
}
