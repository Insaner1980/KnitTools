package com.finnvek.knittools.data.local

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard

fun CounterProjectEntity.toDomain(): CounterProject =
    CounterProject(
        id = id,
        name = name,
        count = count,
        secondaryCount = secondaryCount,
        stepSize = stepSize,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sectionName = sectionName,
        stitchCount = stitchCount,
        isCompleted = isCompleted,
        totalRows = totalRows,
        completedAt = completedAt,
        yarnCardIds = yarnCardIds,
        linkedPatternId = linkedPatternId,
        patternUri = patternUri,
        patternName = patternName,
        currentPatternPage = currentPatternPage,
        patternRowMapping = patternRowMapping,
        stitchTrackingEnabled = stitchTrackingEnabled,
        currentStitch = currentStitch,
        targetRows = targetRows,
    )

fun CounterProject.toEntity(): CounterProjectEntity =
    CounterProjectEntity(
        id = id,
        name = name,
        count = count,
        secondaryCount = secondaryCount,
        stepSize = stepSize,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sectionName = sectionName,
        stitchCount = stitchCount,
        isCompleted = isCompleted,
        totalRows = totalRows,
        completedAt = completedAt,
        yarnCardIds = yarnCardIds,
        linkedPatternId = linkedPatternId,
        patternUri = patternUri,
        patternName = patternName,
        currentPatternPage = currentPatternPage,
        patternRowMapping = patternRowMapping,
        stitchTrackingEnabled = stitchTrackingEnabled,
        currentStitch = currentStitch,
        targetRows = targetRows,
    )

fun ProjectCounterEntity.toDomain(): ProjectCounter =
    ProjectCounter(
        id = id,
        projectId = projectId,
        name = name,
        count = count,
        stepSize = stepSize,
        repeatAt = repeatAt,
        sortOrder = sortOrder,
        createdAt = createdAt,
        counterType = counterType,
        startingStitches = startingStitches,
        stitchChange = stitchChange,
        shapeEveryN = shapeEveryN,
        repeatStartRow = repeatStartRow,
        repeatEndRow = repeatEndRow,
        totalRepeats = totalRepeats,
        currentRepeat = currentRepeat,
    )

fun ProjectCounter.toEntity(): ProjectCounterEntity =
    ProjectCounterEntity(
        id = id,
        projectId = projectId,
        name = name,
        count = count,
        stepSize = stepSize,
        repeatAt = repeatAt,
        sortOrder = sortOrder,
        createdAt = createdAt,
        counterType = counterType,
        startingStitches = startingStitches,
        stitchChange = stitchChange,
        shapeEveryN = shapeEveryN,
        repeatStartRow = repeatStartRow,
        repeatEndRow = repeatEndRow,
        totalRepeats = totalRepeats,
        currentRepeat = currentRepeat,
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
        ravelryId = ravelryId,
        name = name,
        designerName = designerName,
        thumbnailUrl = thumbnailUrl,
        difficulty = difficulty,
        gaugeStitches = gaugeStitches,
        gaugeRows = gaugeRows,
        needleSize = needleSize,
        yarnWeight = yarnWeight,
        yardage = yardage,
        isFree = isFree,
        patternUrl = patternUrl,
        savedAt = savedAt,
    )

fun SavedPattern.toEntity(): SavedPatternEntity =
    SavedPatternEntity(
        id = id,
        ravelryId = ravelryId,
        name = name,
        designerName = designerName,
        thumbnailUrl = thumbnailUrl,
        difficulty = difficulty,
        gaugeStitches = gaugeStitches,
        gaugeRows = gaugeRows,
        needleSize = needleSize,
        yarnWeight = yarnWeight,
        yardage = yardage,
        isFree = isFree,
        patternUrl = patternUrl,
        savedAt = savedAt,
    )

fun YarnCardEntity.toDomain(): YarnCard =
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
    )

fun PatternAnnotationEntity.toDomain(): PatternAnnotation =
    PatternAnnotation(
        id = id,
        projectId = projectId,
        page = page,
        pathData = pathData,
        color = color,
        strokeWidth = strokeWidth,
        createdAt = createdAt,
    )

fun PatternAnnotation.toEntity(): PatternAnnotationEntity =
    PatternAnnotationEntity(
        id = id,
        projectId = projectId,
        page = page,
        pathData = pathData,
        color = color,
        strokeWidth = strokeWidth,
        createdAt = createdAt,
    )
