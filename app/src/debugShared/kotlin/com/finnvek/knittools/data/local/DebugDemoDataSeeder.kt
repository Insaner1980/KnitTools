package com.finnvek.knittools.data.local

import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object DebugDemoDataSeeder {
    private const val MARKER_PROJECT_NAME = "Forest Cardigan"
    private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
    private const val MINUTE_MILLIS = 60_000L
    private const val DEFAULT_NEEDLE_SIZE = "4.0 mm"

    fun seedIfNeeded(
        applicationScope: CoroutineScope,
        ioDispatcher: CoroutineDispatcher,
        database: Lazy<KnitToolsDatabase>,
        transactionRunner: Lazy<DatabaseTransactionRunner>,
        addCounter: suspend (ProjectCounter) -> Long,
        saveYarnCard: suspend (YarnCard) -> Long,
    ) {
        applicationScope.launch(ioDispatcher) {
            transactionRunner.get().run {
                seedDatabaseIfNeeded(
                    database = database.get(),
                    addCounter = addCounter,
                    saveYarnCard = saveYarnCard,
                )
            }
        }
    }

    private suspend fun seedDatabaseIfNeeded(
        database: KnitToolsDatabase,
        addCounter: suspend (ProjectCounter) -> Long,
        saveYarnCard: suspend (YarnCard) -> Long,
    ) {
        val projectDao = database.counterProjectDao()
        if (projectDao.getAllProjectsOnce().any { it.name == MARKER_PROJECT_NAME }) {
            return
        }

        val now = System.currentTimeMillis()
        val woodlandPatternId =
            database.savedPatternDao().insert(
                SavedPatternEntity(
                    source = SavedPatternSource.WebLink.persistedValue,
                    name = "Woodland Cardigan",
                    designerName = "Maya Rowan",
                    difficulty = 3.5f,
                    gaugeStitches = 20f,
                    gaugeRows = 28f,
                    needleSize = DEFAULT_NEEDLE_SIZE,
                    yarnWeight = "DK",
                    yardage = 1_150,
                    originalUrl = "https://example.invalid/patterns/woodland-cardigan",
                    canonicalUrl = "https://example.invalid/patterns/woodland-cardigan",
                    savedAt = now - 35L * DAY_MILLIS,
                    updatedAt = now - 3L * DAY_MILLIS,
                ),
            )
        database.savedPatternDao().insert(
            SavedPatternEntity(
                source = SavedPatternSource.WebLink.persistedValue,
                name = "Harbor Socks",
                designerName = "Elli North",
                difficulty = 2.5f,
                gaugeStitches = 30f,
                gaugeRows = 42f,
                needleSize = "2.5 mm",
                yarnWeight = "Fingering",
                yardage = 420,
                originalUrl = "https://example.invalid/patterns/harbor-socks",
                canonicalUrl = "https://example.invalid/patterns/harbor-socks",
                savedAt = now - 18L * DAY_MILLIS,
                updatedAt = now - 2L * DAY_MILLIS,
            ),
        )

        val cardiganId =
            projectDao.insert(
                CounterProjectEntity(
                    name = MARKER_PROJECT_NAME,
                    count = 86,
                    notes = "Check sleeve length after the next cable repeat. Block pieces before seaming.",
                    createdAt = now - 42L * DAY_MILLIS,
                    updatedAt = now - 25L * MINUTE_MILLIS,
                    sectionName = "Right sleeve",
                    stitchCount = 112,
                    linkedPatternId = woodlandPatternId,
                    patternName = "Woodland Cardigan",
                    stitchTrackingEnabled = true,
                    currentStitch = 47,
                    targetRows = 164,
                ),
            )
        val socksId =
            projectDao.insert(
                CounterProjectEntity(
                    name = "Sunday Socks",
                    count = 34,
                    notes = "Second sock. Match the first heel-flap length: 32 rows.",
                    createdAt = now - 20L * DAY_MILLIS,
                    updatedAt = now - 6L * 60L * MINUTE_MILLIS,
                    sectionName = "Heel flap",
                    stitchCount = 64,
                    stitchTrackingEnabled = true,
                    currentStitch = 16,
                    targetRows = 72,
                ),
            )
        val bagId =
            projectDao.insert(
                CounterProjectEntity(
                    name = "Linen Market Bag",
                    count = 18,
                    craftType = "CROCHET",
                    mainCounterLabelType = "ROUNDS",
                    notes = "Switch to the open mesh section after round 20.",
                    createdAt = now - 11L * DAY_MILLIS,
                    updatedAt = now - DAY_MILLIS,
                    sectionName = "Mesh body",
                    stitchCount = 96,
                    targetRows = 42,
                ),
            )
        val blanketId =
            projectDao.insert(
                CounterProjectEntity(
                    name = "Sunrise Baby Blanket",
                    count = 128,
                    notes = "Alternate cream and apricot every eight rows.",
                    createdAt = now - 63L * DAY_MILLIS,
                    updatedAt = now - 2L * DAY_MILLIS,
                    sectionName = "Stripe 16",
                    stitchCount = 148,
                    targetRows = 220,
                ),
            )
        val beanieId =
            projectDao.insert(
                CounterProjectEntity(
                    name = "Cable Beanie",
                    count = 96,
                    notes = "Finished with a folded brim and a removable pom-pom.",
                    createdAt = now - 90L * DAY_MILLIS,
                    updatedAt = now - 14L * DAY_MILLIS,
                    isCompleted = true,
                    totalRows = 96,
                    completedAt = now - 14L * DAY_MILLIS,
                    stitchCount = 88,
                    targetRows = 96,
                ),
            )

        seedCounters(addCounter, cardiganId, socksId, bagId, blanketId, now)
        seedReminders(database, cardiganId, socksId, bagId, blanketId, now)
        seedYarn(database, saveYarnCard, cardiganId, socksId, bagId, blanketId, now)
        seedSessions(database, cardiganId, socksId, bagId, blanketId, beanieId, now)
        seedHistory(database, cardiganId, socksId, bagId, now)
    }

    private suspend fun seedCounters(
        addCounter: suspend (ProjectCounter) -> Long,
        cardiganId: Long,
        socksId: Long,
        bagId: Long,
        blanketId: Long,
        now: Long,
    ) {
        addCounter(
            ProjectCounter(
                projectId = cardiganId,
                name = "Cable repeat",
                count = 5,
                repeatAt = 8,
                sortOrder = 0,
                createdAt = now - 35L * DAY_MILLIS,
                counterType = ProjectCounterType.REPEATING,
            ),
        )
        addCounter(
            ProjectCounter(
                projectId = cardiganId,
                name = "Sleeve increases",
                count = 7,
                sortOrder = 1,
                createdAt = now - 21L * DAY_MILLIS,
                counterType = ProjectCounterType.SHAPING,
                startingStitches = 76,
                stitchChange = 2,
                shapeEveryN = 6,
            ),
        )
        addCounter(
            ProjectCounter(
                projectId = socksId,
                name = "Heel turns",
                count = 9,
                sortOrder = 0,
                createdAt = now - 8L * DAY_MILLIS,
                counterType = ProjectCounterType.COUNT_UP,
            ),
        )
        addCounter(
            ProjectCounter(
                projectId = bagId,
                name = "Mesh repeat",
                count = 2,
                sortOrder = 0,
                createdAt = now - 6L * DAY_MILLIS,
                counterType = ProjectCounterType.REPEAT_SECTION,
                repeatStartRow = 13,
                repeatEndRow = 20,
                totalRepeats = 4,
                currentRepeat = 2,
            ),
        )
        addCounter(
            ProjectCounter(
                projectId = blanketId,
                name = "Color stripe",
                count = 16,
                sortOrder = 0,
                createdAt = now - 50L * DAY_MILLIS,
                linkedToMainCounter = true,
            ),
        )
    }

    private suspend fun seedReminders(
        database: KnitToolsDatabase,
        cardiganId: Long,
        socksId: Long,
        bagId: Long,
        blanketId: Long,
        now: Long,
    ) {
        val dao = database.rowReminderDao()
        dao.insert(
            RowReminderEntity(
                projectId = cardiganId,
                targetRow = 90,
                message = "Measure sleeve length",
                createdAt = now,
            ),
        )
        dao.insert(
            RowReminderEntity(
                projectId = cardiganId,
                targetRow = 88,
                repeatInterval = 8,
                message = "Work cable chart",
                createdAt = now,
            ),
        )
        dao.insert(RowReminderEntity(projectId = socksId, targetRow = 40, message = "Begin heel turn", createdAt = now))
        dao.insert(RowReminderEntity(projectId = bagId, targetRow = 20, message = "Start open mesh", createdAt = now))
        dao.insert(
            RowReminderEntity(
                projectId = blanketId,
                targetRow = 136,
                repeatInterval = 8,
                message = "Change stripe color",
                createdAt = now,
            ),
        )
    }

    private suspend fun seedYarn(
        database: KnitToolsDatabase,
        saveYarnCard: suspend (YarnCard) -> Long,
        cardiganId: Long,
        socksId: Long,
        bagId: Long,
        blanketId: Long,
        now: Long,
    ) {
        saveYarnCard(
            YarnCard(
                brand = "North Mill",
                yarnName = "Willow DK",
                fiberContent = "100% merino wool",
                weightGrams = "100 g",
                lengthMeters = "220 m",
                needleSize = DEFAULT_NEEDLE_SIZE,
                gaugeInfo = "20 sts / 28 rows",
                colorName = "Forest Moss",
                colorNumber = "318",
                dyeLot = "D24-07",
                weightCategory = "DK",
                createdAt = now - 40L * DAY_MILLIS,
                quantityInStash = 6,
                status = YarnCardStatus.IN_USE,
                linkedProjectId = cardiganId,
            ),
        )
        val creamId =
            saveYarnCard(
                YarnCard(
                    brand = "North Mill",
                    yarnName = "Willow DK",
                    fiberContent = "100% merino wool",
                    weightGrams = "100 g",
                    lengthMeters = "220 m",
                    needleSize = DEFAULT_NEEDLE_SIZE,
                    gaugeInfo = "20 sts / 28 rows",
                    colorName = "Natural Cream",
                    colorNumber = "101",
                    dyeLot = "D24-11",
                    weightCategory = "DK",
                    createdAt = now - 39L * DAY_MILLIS,
                    quantityInStash = 2,
                    status = YarnCardStatus.IN_USE,
                    linkedProjectId = cardiganId,
                ),
            )
        saveYarnCard(
            YarnCard(
                brand = "Cloudbird",
                yarnName = "Everyday Sock",
                fiberContent = "75% wool, 25% nylon",
                weightGrams = "100 g",
                lengthMeters = "420 m",
                needleSize = "2.5 mm",
                gaugeInfo = "30 sts / 42 rows",
                colorName = "Stormy Sea",
                colorNumber = "07",
                dyeLot = "S18",
                weightCategory = "Fingering",
                createdAt = now - 18L * DAY_MILLIS,
                quantityInStash = 1,
                status = YarnCardStatus.IN_USE,
                linkedProjectId = socksId,
            ),
        )
        saveYarnCard(
            YarnCard(
                brand = "Summer Thread Co.",
                yarnName = "Pure Linen",
                fiberContent = "100% linen",
                weightGrams = "50 g",
                lengthMeters = "130 m",
                needleSize = "3.5–4.0 mm",
                colorName = "Terracotta",
                colorNumber = "42",
                dyeLot = "L09",
                weightCategory = "Sport",
                createdAt = now - 10L * DAY_MILLIS,
                quantityInStash = 4,
                status = YarnCardStatus.IN_USE,
                linkedProjectId = bagId,
            ),
        )
        saveYarnCard(
            YarnCard(
                brand = "Soft Nest",
                yarnName = "Baby Cotton",
                fiberContent = "100% organic cotton",
                weightGrams = "50 g",
                lengthMeters = "125 m",
                needleSize = "3.5 mm",
                colorName = "Apricot",
                colorNumber = "214",
                dyeLot = "B31",
                weightCategory = "Sport",
                createdAt = now - 60L * DAY_MILLIS,
                quantityInStash = 3,
                status = YarnCardStatus.IN_USE,
                linkedProjectId = blanketId,
            ),
        )
        database.projectYarnNoteDao().upsert(
            ProjectYarnNoteEntity(
                projectId = cardiganId,
                name = "Contrast yarn",
                description = "Natural cream for cuffs and button band",
                quantity = 2,
                notes = "Keep one skein unopened in case the button band needs extra length.",
                savedYarnCardId = creamId,
                createdAt = now - 30L * DAY_MILLIS,
                updatedAt = now - 4L * DAY_MILLIS,
            ),
        )
    }

    private suspend fun seedSessions(
        database: KnitToolsDatabase,
        cardiganId: Long,
        socksId: Long,
        bagId: Long,
        blanketId: Long,
        beanieId: Long,
        now: Long,
    ) {
        val sessions =
            listOf(
                DemoSession(cardiganId, 1, 79, 86, 42),
                DemoSession(cardiganId, 3, 72, 79, 51),
                DemoSession(cardiganId, 6, 64, 72, 58),
                DemoSession(cardiganId, 9, 58, 64, 39),
                DemoSession(socksId, 0, 30, 34, 28),
                DemoSession(socksId, 4, 24, 30, 35),
                DemoSession(socksId, 8, 18, 24, 31),
                DemoSession(bagId, 2, 14, 18, 36),
                DemoSession(bagId, 7, 9, 14, 44),
                DemoSession(blanketId, 1, 120, 128, 47),
                DemoSession(blanketId, 5, 112, 120, 52),
                DemoSession(blanketId, 10, 104, 112, 49),
                DemoSession(beanieId, 14, 88, 96, 45),
            )
        sessions.forEach { session ->
            val endedAt = now - session.daysAgo * DAY_MILLIS - 2L * 60L * 60L * 1_000L
            database.sessionDao().insert(
                SessionEntity(
                    projectId = session.projectId,
                    startedAt = endedAt - session.minutes * MINUTE_MILLIS,
                    endedAt = endedAt,
                    startRow = session.startRow,
                    endRow = session.endRow,
                    durationMinutes = session.minutes,
                    durationSeconds = session.minutes * 60L,
                    rowsWorked = session.endRow - session.startRow,
                    zoneId = "Europe/Helsinki",
                ),
            )
        }
    }

    private suspend fun seedHistory(
        database: KnitToolsDatabase,
        cardiganId: Long,
        socksId: Long,
        bagId: Long,
        now: Long,
    ) {
        val entries =
            listOf(
                CounterHistoryEntity(
                    projectId = cardiganId,
                    action = "INCREMENT",
                    previousValue = 83,
                    newValue = 84,
                    timestamp =
                        now - 45L * MINUTE_MILLIS,
                ),
                CounterHistoryEntity(
                    projectId = cardiganId,
                    action = "INCREMENT",
                    previousValue = 84,
                    newValue = 85,
                    timestamp =
                        now - 35L * MINUTE_MILLIS,
                ),
                CounterHistoryEntity(
                    projectId = cardiganId,
                    action = "INCREMENT",
                    previousValue = 85,
                    newValue = 86,
                    timestamp =
                        now - 25L * MINUTE_MILLIS,
                ),
                CounterHistoryEntity(
                    projectId = socksId,
                    action = "INCREMENT",
                    previousValue = 33,
                    newValue = 34,
                    timestamp =
                        now - 6L * 60L * MINUTE_MILLIS,
                ),
                CounterHistoryEntity(
                    projectId = bagId,
                    action = "INCREMENT",
                    previousValue = 17,
                    newValue = 18,
                    timestamp =
                        now - DAY_MILLIS,
                ),
            )
        entries.forEach { database.counterProjectDao().insertHistory(it) }
    }

    private data class DemoSession(
        val projectId: Long,
        val daysAgo: Long,
        val startRow: Int,
        val endRow: Int,
        val minutes: Int,
    )
}
