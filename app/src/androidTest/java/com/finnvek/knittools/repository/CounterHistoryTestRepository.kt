package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import kotlinx.coroutines.Dispatchers

internal fun counterHistoryTestRepository(
    database: KnitToolsDatabase,
    context: android.content.Context,
    projectDao: com.finnvek.knittools.data.local.CounterProjectDao = database.counterProjectDao(),
): CounterRepository = RoomCounterTestFixture(database, context).counterRepository(projectDao)

internal class RoomCounterTestFixture(
    private val database: KnitToolsDatabase,
    private val context: android.content.Context,
) {
    val transactionRunner = RoomDatabaseTransactionRunner(database)
    val savedPatternRepository =
        SavedPatternRepository(
            dao = database.savedPatternDao(),
            context = context,
            counterProjectDao = database.counterProjectDao(),
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.IO,
            projectDocumentDao = database.projectDocumentDao(),
        )
    val projectDocumentRepository =
        ProjectDocumentRepository(
            documentDao = database.projectDocumentDao(),
            projectDao = database.counterProjectDao(),
            savedPatternRepository = savedPatternRepository,
            layerRepository =
                PatternAnnotationLayerRepository(
                    database.patternAnnotationLayerDao(),
                    transactionRunner,
                ),
            transactionRunner = transactionRunner,
            fileAvailability = ProjectDocumentFileAvailability(context, Dispatchers.IO),
        )
    val yarnCardRepository =
        YarnCardRepository(
            dao = database.yarnCardDao(),
            counterProjectDao = database.counterProjectDao(),
            context = context,
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.IO,
        )

    fun counterRepository(
        projectDao: com.finnvek.knittools.data.local.CounterProjectDao = database.counterProjectDao(),
        savedPatterns: SavedPatternRepository = savedPatternRepository,
        documents: ProjectDocumentRepository = projectDocumentRepository,
    ): CounterRepository =
        CounterRepository(
            dao = projectDao,
            projectCounterDao = database.projectCounterDao(),
            sessionDao = database.sessionDao(),
            photoStorage = ProgressPhotoStorage(),
            patternDocumentStorage = PatternDocumentStorage(),
            context = context,
            yarnCardRepository = yarnCardRepository,
            savedPatternRepository = savedPatterns,
            projectDocumentRepository = documents,
            projectFolderDao = database.projectFolderDao(),
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.IO,
        )
}
