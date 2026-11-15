package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProjectYarnNoteDao
import com.finnvek.knittools.data.local.ProjectYarnNoteEntity
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.YarnCardStatus
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectYarnNoteRepositoryTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
    }

    @Test
    fun `project yarn note repository saves and observes notes per project`() =
        runTest {
            val dao =
                FakeProjectYarnNoteDao(
                    notes =
                        listOf(
                            ProjectYarnNoteEntity(id = 1L, projectId = 10L, name = "Sleeve yarn"),
                            ProjectYarnNoteEntity(id = 2L, projectId = 11L, name = "Other yarn"),
                        ),
                )
            val repository = buildRepository(dao = dao)

            val savedId =
                repository.save(
                    ProjectYarnNote(
                        projectId = 10L,
                        name = "Contrast cuff",
                        description = "Silk blend",
                        quantity = 2,
                        notes = "Use only for cuffs",
                        createdAt = 100L,
                        updatedAt = 200L,
                    ),
                )
            val notes = repository.observeForProject(10L).first()

            assertEquals(77L, savedId)
            assertEquals("Contrast cuff", dao.lastUpserted?.name)
            assertEquals("Silk blend", dao.lastUpserted?.description)
            assertEquals(2, dao.lastUpserted?.quantity)
            assertEquals("Use only for cuffs", dao.lastUpserted?.notes)
            assertEquals(listOf("Sleeve yarn", "Contrast cuff"), notes.map { it.name })
            assertEquals("Contrast cuff", dao.getById(savedId)?.name)
        }

    @Test
    fun `save to my yarn creates linked yarn card and preserves project note reference`() =
        runTest {
            val noteDao =
                FakeProjectYarnNoteDao(
                    notes =
                        listOf(
                            ProjectYarnNoteEntity(
                                id = 7L,
                                projectId = 10L,
                                name = "Project mohair",
                                description = "Kid silk",
                                quantity = 3,
                                notes = "Hold double",
                                createdAt = 123L,
                            ),
                        ),
                )
            val yarnDao = FakeYarnCardDao()
            val projectDao = LinkTrackingProjectDao(listOf(CounterProjectEntity(id = 10L, yarnCardIds = "")))
            val transactionRunner = CountingDatabaseTransactionRunner()
            val repository =
                buildRepository(
                    dao = noteDao,
                    yarnCardRepository = buildYarnCardRepository(yarnDao, projectDao, transactionRunner),
                    transactionRunner = transactionRunner,
                )

            val beforeSave = System.currentTimeMillis()
            val yarnCardId = repository.saveToMyYarn(7L)
            val savedCard = requireNotNull(yarnDao.lastUpserted)

            assertEquals(88L, yarnCardId)
            assertTrue(savedCard.createdAt in beforeSave..System.currentTimeMillis())
            assertEquals(123L, noteDao.getById(7L)?.createdAt)
            assertEquals("Project mohair", yarnDao.lastUpserted?.yarnName)
            assertEquals("Kid silk", yarnDao.lastUpserted?.fiberContent)
            assertEquals(3, yarnDao.lastUpserted?.quantityInStash)
            assertEquals(YarnCardStatus.IN_USE, yarnDao.lastUpserted?.status)
            assertEquals(10L, yarnDao.lastUpserted?.linkedProjectId)
            assertEquals(7L to 88L, noteDao.savedYarnCardLink)
            assertEquals(mapOf(10L to "88"), projectDao.updatedYarnCardIds)
            assertEquals(1, transactionRunner.runCount)
        }

    @Test
    fun `save to my yarn returns null when project note is missing`() =
        runTest {
            val noteDao = FakeProjectYarnNoteDao()
            val repository = buildRepository(dao = noteDao)

            assertNull(repository.saveToMyYarn(404L))
        }

    @Test
    fun `edit reuses creation normalization and preserves the current library binding`() =
        runTest {
            val original = ProjectYarnNote(id = 7, projectId = 10, name = "Wool", createdAt = 100)
            val current = original.copy(savedYarnCardId = 9, updatedAt = 200).toEntity()
            val dao = FakeProjectYarnNoteDao(listOf(current))
            val repository = buildRepository(dao)
            assertEquals(
                ProjectYarnNoteEditResult.InvalidName,
                repository.edit(original, original.copy(name = "  ")),
            )
            assertNull(dao.lastUpserted)
            val draft = original.copy(name = "  Silk  ", description = "  Blend  ", quantity = 0, notes = "  Cuffs  ")

            assertEquals(ProjectYarnNoteEditResult.Saved, repository.edit(original, draft))
            val edited = requireNotNull(dao.lastUpserted)
            assertEquals(
                current.copy(
                    name = "Silk",
                    description = "Blend",
                    quantity = 1,
                    notes = "Cuffs",
                    updatedAt = edited.updatedAt,
                ),
                edited,
            )
            repository.save(draft.copy(id = 0))
            val created = requireNotNull(dao.lastUpserted)
            assertEquals(
                listOf("Silk", "Blend", 1, "Cuffs"),
                listOf(created.name, created.description, created.quantity, created.notes),
            )
        }

    @Test
    fun `edit rejects a concurrent change to any editable field without writing`() =
        runTest {
            val original = ProjectYarnNote(id = 7, projectId = 10, name = "Wool")
            listOf(
                original.copy(name = "Other"),
                original.copy(description = "Other"),
                original.copy(quantity = 2),
                original.copy(notes = "Other"),
            ).forEach { current ->
                val dao = FakeProjectYarnNoteDao(listOf(current.toEntity()))
                assertEquals(
                    ProjectYarnNoteEditResult.Conflict,
                    buildRepository(dao).edit(original, original.copy(name = "Silk", quantity = 3)),
                )
                assertNull(dao.lastUpserted)
            }
        }

    @Test
    fun `retry of an already committed normalized draft does not write again`() =
        runTest {
            val original = ProjectYarnNote(id = 7, projectId = 10, name = "Wool", createdAt = 100)
            val current = original.copy(name = "Silk", quantity = 1, savedYarnCardId = 9, updatedAt = 200)
            val dao = FakeProjectYarnNoteDao(listOf(current.toEntity()))

            assertEquals(
                ProjectYarnNoteEditResult.Saved,
                buildRepository(dao).edit(original, original.copy(name = " Silk ", quantity = 0)),
            )
            assertNull(dao.lastUpserted)
            assertEquals(current.toEntity(), dao.getById(7))
        }

    private fun buildRepository(
        dao: ProjectYarnNoteDao,
        yarnCardRepository: YarnCardRepository =
            buildYarnCardRepository(
                yarnDao = FakeYarnCardDao(),
                projectDao = LinkTrackingProjectDao(listOf(CounterProjectEntity(id = 10L))),
            ),
        transactionRunner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner,
    ): ProjectYarnNoteRepository =
        ProjectYarnNoteRepository(
            dao = dao,
            yarnCardRepository = yarnCardRepository,
            transactionRunner = transactionRunner,
            usageDao = mockk(relaxed = true),
        )

    private fun buildYarnCardRepository(
        yarnDao: FakeYarnCardDao,
        projectDao: LinkTrackingProjectDao,
        transactionRunner: DatabaseTransactionRunner = ImmediateDatabaseTransactionRunner,
    ): YarnCardRepository =
        YarnCardRepository(
            yarnDao,
            projectDao,
            context,
            transactionRunner,
            UnconfinedTestDispatcher(),
        )

    private class FakeProjectYarnNoteDao(
        notes: List<ProjectYarnNoteEntity> = emptyList(),
    ) : ProjectYarnNoteDao {
        private val stored = notes.associateBy { it.id }.toMutableMap()
        var lastUpserted: ProjectYarnNoteEntity? = null
        var savedYarnCardLink: Pair<Long, Long>? = null
        var deletedId: Long? = null

        override fun observeForProject(projectId: Long): Flow<List<ProjectYarnNoteEntity>> =
            flowOf(
                stored.values
                    .filter { it.projectId == projectId }
                    .sortedWith(compareByDescending<ProjectYarnNoteEntity> { it.createdAt }.thenByDescending { it.id }),
            )

        override suspend fun getById(id: Long): ProjectYarnNoteEntity? = stored[id]

        override suspend fun upsert(note: ProjectYarnNoteEntity): Long {
            lastUpserted = note
            val id = note.id.takeIf { it > 0L } ?: maxOf(77L, (stored.keys.maxOrNull() ?: 0L) + 1L)
            stored[id] = note.copy(id = id)
            return id
        }

        override suspend fun update(note: ProjectYarnNoteEntity): Int {
            if (note.id !in stored) return 0
            lastUpserted = note
            stored[note.id] = note
            return 1
        }

        override suspend fun updateSavedYarnCardId(
            id: Long,
            savedYarnCardId: Long,
            updatedAt: Long,
        ): Int {
            val note = stored[id] ?: return 0
            stored[id] = note.copy(savedYarnCardId = savedYarnCardId, updatedAt = updatedAt)
            savedYarnCardLink = id to savedYarnCardId
            return 1
        }

        override suspend fun delete(id: Long) {
            deletedId = id
            stored.remove(id)
        }
    }

    private class FakeYarnCardDao : YarnCardDao {
        var lastUpserted: YarnCardEntity? = null

        override fun getAllCards(): Flow<List<YarnCardEntity>> = flowOf(emptyList())

        override suspend fun getCard(id: Long): YarnCardEntity? = null

        override fun observeCard(id: Long): Flow<YarnCardEntity?> = flowOf(null)

        override suspend fun getCards(ids: List<Long>): List<YarnCardEntity> = emptyList()

        override suspend fun upsert(card: YarnCardEntity): Long {
            lastUpserted = card
            return 88L
        }

        override fun getCardCount(): Flow<Int> = flowOf(0)

        override suspend fun updateQuantity(
            id: Long,
            quantity: Int,
        ): Int = 0

        override suspend fun updateStatus(
            id: Long,
            status: String,
        ): Int = 0

        override suspend fun updatePhotoUri(
            id: Long,
            photoUri: String,
        ): Int = 0

        override suspend fun updateLinkedProjectId(
            id: Long,
            projectId: Long?,
        ): Int = 0

        override suspend fun clearLinkedProject(projectId: Long) = Unit

        override suspend fun delete(id: Long) = Unit

        override suspend fun deleteByIds(ids: List<Long>) = Unit
    }

    private class LinkTrackingProjectDao(
        projects: List<CounterProjectEntity>,
    ) : StubCounterProjectDao(projects) {
        val updatedYarnCardIds = linkedMapOf<Long, String>()

        override suspend fun updateYarnCardIds(
            id: Long,
            yarnCardIds: String,
            updatedAt: Long,
        ) {
            updatedYarnCardIds[id] = yarnCardIds
        }
    }

    private class CountingDatabaseTransactionRunner : DatabaseTransactionRunner {
        var runCount: Int = 0
            private set

        override suspend fun <T> run(block: suspend () -> T): T {
            runCount += 1
            return block()
        }
    }
}
