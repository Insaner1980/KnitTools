package com.finnvek.knittools.data.local

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec
import com.finnvek.knittools.domain.model.PatternAvailability
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.sanitizeMainCounterCustomLabel
import com.finnvek.knittools.domain.model.sanitizeReadingGuideFraction
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction

fun CounterProjectEntity.toDomain(): CounterProject =
    CounterProject(
        id = id,
        name = name,
        count = count,
        craftType = CraftType.fromPersistedValue(craftType),
        mainCounterLabelType = MainCounterLabelType.fromPersistedValue(mainCounterLabelType),
    ).withEntityProjectDetails(this)

fun CounterProject.toEntity(): CounterProjectEntity =
    CounterProjectEntity(
        id = id,
        name = name,
        count = count,
        craftType = craftType.persistedValue,
        mainCounterLabelType = mainCounterLabelType.persistedValue,
    ).withDomainProjectDetails(this)

private fun CounterProject.withEntityProjectDetails(entity: CounterProjectEntity): CounterProject {
    val validStitchCount = entity.stitchCount?.takeIf { it > 0 }
    val trackingEnabled = entity.stitchTrackingEnabled && validStitchCount != null
    val currentStitch =
        validStitchCount
            ?.takeIf { trackingEnabled }
            ?.let { entity.currentStitch.coerceIn(0, it) }
            ?: 0
    return copy(
        count = entity.count.coerceAtLeast(0),
        mainCounterCustomLabel = sanitizeMainCounterCustomLabel(entity.mainCounterCustomLabel),
        readingLineEnabled = entity.readingLineEnabled,
        readingLineYFraction = entity.readingLineYFraction.coerceIn(0f, 1f),
        readingLineFollowCurrentRow = entity.readingLineFollowCurrentRow,
        verticalReadingGuideEnabled = entity.verticalReadingGuideEnabled,
        verticalReadingGuideXFraction = sanitizeReadingGuideFraction(entity.verticalReadingGuideXFraction),
        secondaryCount = entity.secondaryCount,
        secondaryCounterUsed = entity.secondaryCounterUsed,
        stepSize = entity.stepSize.coerceAtLeast(1),
        notes = entity.notes,
        notesCreated = entity.notesCreated,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        sectionName = entity.sectionName,
        stitchCount = validStitchCount,
        isCompleted = entity.isCompleted,
        totalRows = entity.totalRows,
        completedAt = entity.completedAt,
        yarnCardIds = entity.yarnCardIds,
        linkedPatternId = entity.linkedPatternId,
        patternUri = entity.patternUri,
        patternName = entity.patternName,
        currentPatternPage = entity.currentPatternPage,
        patternRowMapping = entity.patternRowMapping,
        stitchTrackingEnabled = trackingEnabled,
        currentStitch = currentStitch,
        targetRows = entity.targetRows,
    )
}

private fun CounterProjectEntity.withDomainProjectDetails(project: CounterProject): CounterProjectEntity {
    val validStitchCount = project.stitchCount?.takeIf { it > 0 }
    val trackingEnabled = project.stitchTrackingEnabled && validStitchCount != null
    val currentStitch =
        validStitchCount
            ?.takeIf { trackingEnabled }
            ?.let { project.currentStitch.coerceIn(0, it) }
            ?: 0
    return copy(
        count = project.count.coerceAtLeast(0),
        mainCounterCustomLabel = sanitizeMainCounterCustomLabel(project.mainCounterCustomLabel),
        readingLineEnabled = project.readingLineEnabled,
        readingLineYFraction = sanitizeReadingLineYFraction(project.readingLineYFraction),
        readingLineFollowCurrentRow = project.readingLineFollowCurrentRow,
        verticalReadingGuideEnabled = project.verticalReadingGuideEnabled,
        verticalReadingGuideXFraction = sanitizeReadingGuideFraction(project.verticalReadingGuideXFraction),
        secondaryCount = project.secondaryCount,
        secondaryCounterUsed = project.secondaryCounterUsed,
        stepSize = project.stepSize.coerceAtLeast(1),
        notes = project.notes,
        notesCreated = project.notesCreated,
        createdAt = project.createdAt,
        updatedAt = project.updatedAt,
        sectionName = project.sectionName,
        stitchCount = validStitchCount,
        isCompleted = project.isCompleted,
        totalRows = project.totalRows,
        completedAt = project.completedAt,
        yarnCardIds = project.yarnCardIds,
        linkedPatternId = project.linkedPatternId,
        patternUri = project.patternUri,
        patternName = project.patternName,
        currentPatternPage = project.currentPatternPage,
        patternRowMapping = project.patternRowMapping,
        stitchTrackingEnabled = trackingEnabled,
        currentStitch = currentStitch,
        targetRows = project.targetRows,
    )
}

fun ProjectCounterEntity.toDomain(): ProjectCounter {
    val type = ProjectCounterType.fromPersistedValue(counterType)
    return ProjectCounter(
        id = id,
        projectId = projectId,
        name = name,
        count = count.coerceAtLeast(0),
        stepSize = stepSize.coerceAtLeast(1),
        repeatAt = repeatAt,
        sortOrder = sortOrder,
        createdAt = createdAt,
        counterType = type,
        startingStitches = startingStitches,
        stitchChange = stitchChange,
        shapeEveryN = shapeEveryN,
        repeatStartRow = repeatStartRow,
        repeatEndRow = repeatEndRow,
        totalRepeats = totalRepeats,
        currentRepeat = currentRepeat,
        linkedToMainCounter = linkedToMainCounter && type != ProjectCounterType.REPEAT_SECTION,
    )
}

fun ProjectCounter.toEntity(): ProjectCounterEntity =
    ProjectCounterEntity(
        id = id,
        projectId = projectId,
        name = name,
        count = count.coerceAtLeast(0),
        stepSize = stepSize.coerceAtLeast(1),
        repeatAt = repeatAt,
        sortOrder = sortOrder,
        createdAt = createdAt,
        counterType = counterType.persistedValue,
        startingStitches = startingStitches,
        stitchChange = stitchChange,
        shapeEveryN = shapeEveryN,
        repeatStartRow = repeatStartRow,
        repeatEndRow = repeatEndRow,
        totalRepeats = totalRepeats,
        currentRepeat = currentRepeat,
        linkedToMainCounter = linkedToMainCounter && counterType != ProjectCounterType.REPEAT_SECTION,
    )

fun ProjectYarnNoteEntity.toDomain(): ProjectYarnNote =
    ProjectYarnNote(
        id = id,
        projectId = projectId,
        name = name,
        description = description,
        quantity = quantity,
        notes = notes,
        savedYarnCardId = savedYarnCardId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ProjectYarnNote.toEntity(): ProjectYarnNoteEntity =
    ProjectYarnNoteEntity(
        id = id,
        projectId = projectId,
        name = name,
        description = description,
        quantity = quantity,
        notes = notes,
        savedYarnCardId = savedYarnCardId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun RowReminderEntity.toDomain(): RowReminder =
    RowReminder(
        id = id,
        projectId = projectId,
        targetRow = targetRow,
        repeatInterval = repeatInterval,
        message = message,
        isCompleted = isCompleted,
        createdAt = createdAt,
    )

fun RowReminder.toEntity(): RowReminderEntity =
    RowReminderEntity(
        id = id,
        projectId = projectId,
        targetRow = targetRow,
        repeatInterval = repeatInterval,
        message = message,
        isCompleted = isCompleted,
        createdAt = createdAt,
    )

fun ProgressPhotoEntity.toDomain(): ProgressPhoto =
    ProgressPhoto(
        id = id,
        projectId = projectId,
        photoUri = photoUri,
        rowNumber = rowNumber,
        note = note,
        createdAt = createdAt,
    )

fun ProgressPhoto.toEntity(): ProgressPhotoEntity =
    ProgressPhotoEntity(
        id = id,
        projectId = projectId,
        photoUri = photoUri,
        rowNumber = rowNumber,
        note = note,
        createdAt = createdAt,
    )

fun SavedPatternEntity.toDomain(): SavedPattern =
    SavedPattern(
        id = id,
        // CPD-OFF: Eksplisiittinen kenttarakenne sailyttaa kerros- ja tietokantarajat.
        source = SavedPatternSource.fromPersistedValue(source),
        ravelryPatternId = ravelryPatternId,
        name = name,
        designerName = designerName,
        thumbnailUrl = thumbnailUrl,
        difficulty = difficulty,
        gaugeStitches = gaugeStitches,
        gaugeRows = gaugeRows,
        needleSize = needleSize,
        yarnWeight = yarnWeight,
        yardage = yardage,
        availability = PatternAvailability.fromPersistedValue(availability),
        originalUrl = originalUrl,
        canonicalUrl = canonicalUrl,
        localPdfUri = localPdfUri,
        isAvailableOffline = isAvailableOffline,
        savedAt = savedAt,
        updatedAt = updatedAt,
        lastSyncedAt = lastSyncedAt,
    )

fun SavedPattern.toEntity(): SavedPatternEntity =
// CPD-ON
    SavedPatternEntity(
        id = id,
        source = source.persistedValue,
        ravelryPatternId = ravelryPatternId,
        name = name,
        designerName = designerName,
        thumbnailUrl = thumbnailUrl,
        difficulty = difficulty,
        gaugeStitches = gaugeStitches,
        gaugeRows = gaugeRows,
        needleSize = needleSize,
        yarnWeight = yarnWeight,
        yardage = yardage,
        availability = availability.persistedValue,
        originalUrl = originalUrl,
        canonicalUrl = canonicalUrl,
        localPdfUri = localPdfUri,
        isAvailableOffline = isAvailableOffline,
        savedAt = savedAt,
        updatedAt = updatedAt,
        lastSyncedAt = lastSyncedAt,
    )

fun YarnCardEntity.toDomain(): YarnCard =
    // CPD-OFF: Eksplisiittinen kenttarakenne sailyttaa kerros- ja tietokantarajat.
    YarnCard(
        id = id,
        brand = brand,
        yarnName = yarnName,
        fiberContent = fiberContent,
        weightGrams = weightGrams,
        lengthMeters = lengthMeters,
        needleSize = needleSize,
        gaugeInfo = gaugeInfo,
        colorName = colorName,
        colorNumber = colorNumber,
        dyeLot = dyeLot,
        weightCategory = weightCategory,
        careSymbols = careSymbols,
        photoUri = photoUri,
        createdAt = createdAt,
        quantityInStash = quantityInStash,
        status = status,
        linkedProjectId = linkedProjectId,
    )

fun YarnCard.toEntity(): YarnCardEntity =
// CPD-ON
    YarnCardEntity(
        id = id,
        brand = brand,
        yarnName = yarnName,
        fiberContent = fiberContent,
        weightGrams = weightGrams,
        lengthMeters = lengthMeters,
        needleSize = needleSize,
        gaugeInfo = gaugeInfo,
        colorName = colorName,
        colorNumber = colorNumber,
        dyeLot = dyeLot,
        weightCategory = weightCategory,
        careSymbols = careSymbols,
        photoUri = photoUri,
        createdAt = createdAt,
        quantityInStash = quantityInStash,
        status = status,
        linkedProjectId = linkedProjectId,
    )

fun SessionEntity.toDomain(): KnitSession =
    KnitSession(
        id = id,
        projectId = projectId,
        startedAt = startedAt,
        endedAt = endedAt,
        startRow = startRow,
        endRow = endRow,
        durationMinutes = durationMinutes,
        durationSeconds = durationSeconds,
        rowsWorked = rowsWorked,
        zoneId = zoneId,
    )

fun KnitSession.toEntity(): SessionEntity =
    SessionEntity(
        id = id,
        projectId = projectId,
        startedAt = startedAt,
        endedAt = endedAt,
        startRow = startRow,
        endRow = endRow,
        durationMinutes = durationMinutes,
        durationSeconds = durationSeconds,
        rowsWorked = rowsWorked,
        zoneId = zoneId,
    )

fun PatternAnnotationLayerEntity.toDomain(): PatternAnnotationLayer? {
    val owner =
        when {
            projectId != null && savedPatternId == null -> PatternAnnotationOwner.Project(projectId, documentKey)
            projectId == null && savedPatternId != null ->
                PatternAnnotationOwner.SavedPattern(
                    savedPatternId,
                    documentKey,
                )
            else -> null
        } ?: return null
    return PatternAnnotationLayer(
        id = id,
        owner = owner,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun PatternAnnotationLayer.toEntity(): PatternAnnotationLayerEntity =
    PatternAnnotationLayerEntity(
        id = id,
        projectId = (owner as? PatternAnnotationOwner.Project)?.projectId,
        savedPatternId = (owner as? PatternAnnotationOwner.SavedPattern)?.savedPatternId,
        documentKey = owner.documentKey,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun PatternAnnotationEntity.toDomain(): PatternAnnotation? {
    if (page < 0) return null
    val annotationKind = runCatching { PatternAnnotationKind.valueOf(kind) }.getOrNull() ?: return null
    val annotationPayload =
        PatternAnnotationPayloadCodec.decode(
            kind = annotationKind,
            payloadVersion = payloadVersion,
            json = payloadJson,
        ) ?: return null
    return PatternAnnotation(
        id = id,
        layerId = layerId,
        page = page,
        kind = annotationKind,
        payload = annotationPayload,
        zIndex = zIndex,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun PatternAnnotation.toEntity(): PatternAnnotationEntity =
    requireNotNull(PatternAnnotationPayloadCodec.encode(kind, payload)) {
        "Pattern annotation payload is invalid"
    }.let { encoded ->
        PatternAnnotationEntity(
            id = id,
            layerId = layerId,
            page = page,
            kind = kind.name,
            payloadVersion = encoded.payloadVersion,
            payloadJson = encoded.payloadJson,
            zIndex = zIndex,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
