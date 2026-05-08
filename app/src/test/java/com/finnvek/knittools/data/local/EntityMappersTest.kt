package com.finnvek.knittools.data.local

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {
    @Test
    fun `counter project mapper preserves all fields`() {
        assertCounterProjectMapping(
            CounterProjectEntity(
                id = 11L,
                name = "Cardigan",
                count = 24,
                secondaryCount = 3,
                stepSize = 2,
                notes = "Sleeves in progress",
                createdAt = 1_700_000_001L,
                updatedAt = 1_700_000_002L,
                sectionName = "Left sleeve",
                stitchCount = 96,
                isCompleted = true,
                totalRows = 120,
                completedAt = 1_700_000_003L,
                yarnCardIds = "4,8",
                linkedPatternId = 42L,
                patternUri = "content://pattern",
                patternName = "Rib cardigan",
                currentPatternPage = 5,
                patternRowMapping = "1=10",
                stitchTrackingEnabled = true,
                currentStitch = 38,
            ),
        )
        assertCounterProjectMapping(
            CounterProjectEntity(
                id = 12L,
                name = "Hat",
                createdAt = 1_700_000_011L,
                updatedAt = 1_700_000_012L,
            ),
        )
    }

    @Test
    fun `project counter mapper preserves all fields`() {
        assertProjectCounterMapping(
            ProjectCounterEntity(
                id = 21L,
                projectId = 11L,
                name = "Decrease rounds",
                count = 8,
                stepSize = 2,
                repeatAt = 12,
                sortOrder = 3,
                createdAt = 1_700_000_101L,
                counterType = "SHAPING",
                startingStitches = 88,
                stitchChange = -8,
                shapeEveryN = 4,
                repeatStartRow = 2,
                repeatEndRow = 10,
                totalRepeats = 6,
                currentRepeat = 4,
            ),
        )
        assertProjectCounterMapping(
            ProjectCounterEntity(
                id = 22L,
                projectId = 12L,
                name = "Rows",
                createdAt = 1_700_000_102L,
            ),
        )
    }

    @Test
    fun `row reminder mapper preserves all fields`() {
        assertRowReminderMapping(
            RowReminderEntity(
                id = 31L,
                projectId = 11L,
                targetRow = 16,
                repeatInterval = 8,
                message = "Cable row",
                isCompleted = true,
                createdAt = 1_700_000_201L,
            ),
        )
        assertRowReminderMapping(
            RowReminderEntity(
                id = 32L,
                projectId = 12L,
                targetRow = 6,
                message = "Switch color",
                createdAt = 1_700_000_202L,
            ),
        )
    }

    @Test
    fun `progress photo mapper preserves all fields`() {
        assertProgressPhotoMapping(
            ProgressPhotoEntity(
                id = 41L,
                projectId = 11L,
                photoUri = "content://photo",
                rowNumber = 36,
                note = "After sleeve split",
                createdAt = 1_700_000_301L,
            ),
        )
        assertProgressPhotoMapping(
            ProgressPhotoEntity(
                id = 42L,
                projectId = 12L,
                photoUri = "content://photo-without-note",
                rowNumber = 4,
                createdAt = 1_700_000_302L,
            ),
        )
    }

    @Test
    fun `saved pattern mapper preserves all fields`() {
        assertSavedPatternMapping(
            SavedPatternEntity(
                id = 51L,
                ravelryId = 12345,
                name = "Rib Cardigan",
                designerName = "Designer",
                thumbnailUrl = "https://example.com/thumb.jpg",
                difficulty = 3.5f,
                gaugeStitches = 22.0f,
                gaugeRows = 30.5f,
                needleSize = "4 mm",
                yarnWeight = "DK",
                yardage = 850,
                isFree = false,
                patternUrl = "https://example.com/pattern",
                savedAt = 1_700_000_401L,
            ),
        )
        assertSavedPatternMapping(
            SavedPatternEntity(
                id = 52L,
                ravelryId = 67890,
                name = "Simple Hat",
                designerName = "Maker",
                savedAt = 1_700_000_402L,
            ),
        )
    }

    @Test
    fun `yarn card mapper preserves all fields`() {
        assertYarnCardMapping(
            YarnCardEntity(
                id = 61L,
                brand = "Finn Wool",
                yarnName = "Soft DK",
                fiberContent = "100% wool",
                weightGrams = "50",
                lengthMeters = "120",
                needleSize = "4 mm",
                gaugeInfo = "22 sts",
                colorName = "Moss",
                colorNumber = "18",
                dyeLot = "A7",
                weightCategory = "DK",
                careSymbols = 15L,
                photoUri = "content://yarn",
                createdAt = 1_700_000_501L,
                quantityInStash = 7,
                status = "IN_PROJECT",
                linkedProjectId = 11L,
            ),
        )
        assertYarnCardMapping(
            YarnCardEntity(
                id = 62L,
                createdAt = 1_700_000_502L,
            ),
        )
    }

    @Test
    fun `knit session mapper preserves all fields`() {
        assertKnitSessionMapping(
            SessionEntity(
                id = 71L,
                projectId = 11L,
                startedAt = 1_700_000_601L,
                endedAt = 1_700_000_961L,
                startRow = 12,
                endRow = 20,
                durationMinutes = 36,
            ),
        )
    }

    @Test
    fun `pattern annotation mapper preserves all fields`() {
        assertPatternAnnotationMapping(
            PatternAnnotationEntity(
                id = 81L,
                projectId = 11L,
                page = 3,
                pathData = "M 0 0 L 10 10",
                color = "#FFAA00",
                strokeWidth = 4.5f,
                createdAt = 1_700_000_701L,
            ),
        )
    }

    private fun assertCounterProjectMapping(entity: CounterProjectEntity) {
        val domain =
            CounterProject(
                id = entity.id,
                name = entity.name,
                count = entity.count,
                secondaryCount = entity.secondaryCount,
                stepSize = entity.stepSize,
                notes = entity.notes,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                sectionName = entity.sectionName,
                stitchCount = entity.stitchCount,
                isCompleted = entity.isCompleted,
                totalRows = entity.totalRows,
                completedAt = entity.completedAt,
                yarnCardIds = entity.yarnCardIds,
                linkedPatternId = entity.linkedPatternId,
                patternUri = entity.patternUri,
                patternName = entity.patternName,
                currentPatternPage = entity.currentPatternPage,
                patternRowMapping = entity.patternRowMapping,
                stitchTrackingEnabled = entity.stitchTrackingEnabled,
                currentStitch = entity.currentStitch,
            )

        assertMapsBothWays(
            entity = entity,
            domain = domain,
            toDomain = CounterProjectEntity::toDomain,
            toEntity = CounterProject::toEntity,
        )
    }

    private fun assertProjectCounterMapping(entity: ProjectCounterEntity) {
        val domain =
            ProjectCounter(
                id = entity.id,
                projectId = entity.projectId,
                name = entity.name,
                count = entity.count,
                stepSize = entity.stepSize,
                repeatAt = entity.repeatAt,
                sortOrder = entity.sortOrder,
                createdAt = entity.createdAt,
                counterType = entity.counterType,
                startingStitches = entity.startingStitches,
                stitchChange = entity.stitchChange,
                shapeEveryN = entity.shapeEveryN,
                repeatStartRow = entity.repeatStartRow,
                repeatEndRow = entity.repeatEndRow,
                totalRepeats = entity.totalRepeats,
                currentRepeat = entity.currentRepeat,
            )

        assertMapsBothWays(
            entity = entity,
            domain = domain,
            toDomain = ProjectCounterEntity::toDomain,
            toEntity = ProjectCounter::toEntity,
        )
    }

    private fun assertRowReminderMapping(entity: RowReminderEntity) {
        val domain =
            RowReminder(
                id = entity.id,
                projectId = entity.projectId,
                targetRow = entity.targetRow,
                repeatInterval = entity.repeatInterval,
                message = entity.message,
                isCompleted = entity.isCompleted,
                createdAt = entity.createdAt,
            )

        assertMapsBothWays(
            entity = entity,
            domain = domain,
            toDomain = RowReminderEntity::toDomain,
            toEntity = RowReminder::toEntity,
        )
    }

    private fun assertProgressPhotoMapping(entity: ProgressPhotoEntity) {
        val domain =
            ProgressPhoto(
                id = entity.id,
                projectId = entity.projectId,
                photoUri = entity.photoUri,
                rowNumber = entity.rowNumber,
                note = entity.note,
                createdAt = entity.createdAt,
            )

        assertMapsBothWays(
            entity = entity,
            domain = domain,
            toDomain = ProgressPhotoEntity::toDomain,
            toEntity = ProgressPhoto::toEntity,
        )
    }

    private fun assertSavedPatternMapping(entity: SavedPatternEntity) {
        val domain =
            SavedPattern(
                id = entity.id,
                ravelryId = entity.ravelryId,
                name = entity.name,
                designerName = entity.designerName,
                thumbnailUrl = entity.thumbnailUrl,
                difficulty = entity.difficulty,
                gaugeStitches = entity.gaugeStitches,
                gaugeRows = entity.gaugeRows,
                needleSize = entity.needleSize,
                yarnWeight = entity.yarnWeight,
                yardage = entity.yardage,
                isFree = entity.isFree,
                patternUrl = entity.patternUrl,
                savedAt = entity.savedAt,
            )

        assertMapsBothWays(
            entity = entity,
            domain = domain,
            toDomain = SavedPatternEntity::toDomain,
            toEntity = SavedPattern::toEntity,
        )
    }

    private fun assertYarnCardMapping(entity: YarnCardEntity) {
        val domain =
            YarnCard(
                id = entity.id,
                brand = entity.brand,
                yarnName = entity.yarnName,
                fiberContent = entity.fiberContent,
                weightGrams = entity.weightGrams,
                lengthMeters = entity.lengthMeters,
                needleSize = entity.needleSize,
                gaugeInfo = entity.gaugeInfo,
                colorName = entity.colorName,
                colorNumber = entity.colorNumber,
                dyeLot = entity.dyeLot,
                weightCategory = entity.weightCategory,
                careSymbols = entity.careSymbols,
                photoUri = entity.photoUri,
                createdAt = entity.createdAt,
                quantityInStash = entity.quantityInStash,
                status = entity.status,
                linkedProjectId = entity.linkedProjectId,
            )

        assertMapsBothWays(
            entity = entity,
            domain = domain,
            toDomain = YarnCardEntity::toDomain,
            toEntity = YarnCard::toEntity,
        )
    }

    private fun assertKnitSessionMapping(entity: SessionEntity) {
        val domain =
            KnitSession(
                id = entity.id,
                projectId = entity.projectId,
                startedAt = entity.startedAt,
                endedAt = entity.endedAt,
                startRow = entity.startRow,
                endRow = entity.endRow,
                durationMinutes = entity.durationMinutes,
            )

        assertMapsBothWays(
            entity = entity,
            domain = domain,
            toDomain = SessionEntity::toDomain,
            toEntity = KnitSession::toEntity,
        )
    }

    private fun assertPatternAnnotationMapping(entity: PatternAnnotationEntity) {
        val domain =
            PatternAnnotation(
                id = entity.id,
                projectId = entity.projectId,
                page = entity.page,
                pathData = entity.pathData,
                color = entity.color,
                strokeWidth = entity.strokeWidth,
                createdAt = entity.createdAt,
            )

        assertMapsBothWays(
            entity = entity,
            domain = domain,
            toDomain = PatternAnnotationEntity::toDomain,
            toEntity = PatternAnnotation::toEntity,
        )
    }

    private fun <Entity, Domain> assertMapsBothWays(
        entity: Entity,
        domain: Domain,
        toDomain: Entity.() -> Domain,
        toEntity: Domain.() -> Entity,
    ) {
        assertEquals(domain, entity.toDomain())
        assertEquals(entity, domain.toEntity())
    }
}
