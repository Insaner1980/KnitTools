package com.finnvek.knittools.repository

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.pro.InAppReviewManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.tips.TipSession
import kotlinx.coroutines.CoroutineScope
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
    private val onboardingStateRepository = OnboardingStateRepository(MemoryOnboardingPreferences())
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
        transactions: DatabaseTransactionRunner = transactionRunner,
        sessionTimeSource: com.finnvek.knittools.data.time.SessionTimeSource =
            com.finnvek.knittools.data.time.UnavailableBootSessionTimeSource,
    ): CounterRepository =
        CounterRepository(
            dao = projectDao,
            projectCounterDao = database.projectCounterDao(),
            sessionDao = database.sessionDao(),
            photoStorage = ProgressPhotoStorage(),
            patternDocumentStorage = PatternDocumentStorage(Dispatchers.IO),
            context = context,
            yarnCardRepository = yarnCardRepository,
            savedPatternRepository = savedPatterns,
            projectDocumentRepository = documents,
            projectFolderDao = database.projectFolderDao(),
            transactionRunner = transactions,
            ioDispatcher = Dispatchers.IO,
            sessionTimeSource = sessionTimeSource,
        )

    fun counterViewModel(
        pro: ProManager,
        applicationScope: CoroutineScope,
        counters: ProjectCounterRepository =
            ProjectCounterRepository(
                database.projectCounterDao(),
                database.counterProjectDao(),
                pro,
                transactionRunner,
            ),
        transactions: DatabaseTransactionRunner = transactionRunner,
    ): CounterViewModel =
        CounterViewModel(
            repository = counterRepository(transactions = transactions),
            reminderRepository =
                ReminderRepository(
                    database.rowReminderDao(),
                    database.counterProjectDao(),
                    pro,
                    transactionRunner,
                ),
            projectCounterRepository = counters,
            photoRepository =
                ProgressPhotoRepository(
                    database.progressPhotoDao(),
                    ProgressPhotoStorage(),
                    context,
                    Dispatchers.IO,
                    transactionRunner = RoomDatabaseTransactionRunner(database),
                ),
            projectYarnNoteRepository =
                ProjectYarnNoteRepository(
                    database.projectYarnNoteDao(),
                    yarnCardRepository,
                    transactionRunner,
                    database.projectYarnUsageDao(),
                ),
            preferencesManager = PreferencesManager(context),
            proManager = pro,
            yarnCardRepository = yarnCardRepository,
            savedPatternRepository = savedPatternRepository,
            projectDocumentRepository = projectDocumentRepository,
            patternDocumentStorage = PatternDocumentStorage(Dispatchers.IO),
            inAppReviewManager = InAppReviewManager(context),
            tipSession =
                TipSession(
                    onboardingStateRepository,
                    UsageAnalytics.NONE,
                    applicationScope,
                ),
            onboardingStateRepository = onboardingStateRepository,
            savedStateHandle = SavedStateHandle(),
            context = context,
            ioDispatcher = Dispatchers.IO,
            applicationScope = applicationScope,
        )
}
