package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.data.remote.PatternSearchParams
import com.finnvek.knittools.data.remote.PatternSearchResponse
import com.finnvek.knittools.data.remote.RavelryApiService
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RavelryRepository
    @Inject
    constructor(
        private val api: RavelryApiService,
        private val savedPatternRepository: SavedPatternRepository,
        private val counterProjectDao: CounterProjectDao,
        private val transactionRunner: DatabaseTransactionRunner,
    ) {
        suspend fun searchPatterns(params: PatternSearchParams): PatternSearchResponse = api.searchPatterns(params)

        suspend fun getPatternDetail(id: Int): PatternDetail = api.getPatternDetail(id)

        suspend fun importPatternByUrl(url: String): PatternDetail = api.importPatternByUrl(url)

        fun getSavedPatterns(): Flow<List<SavedPattern>> = savedPatternRepository.getAll()

        fun getSavedPatternCount(): Flow<Int> = savedPatternRepository.getCount()

        suspend fun isPatternSaved(ravelryId: Int): Boolean =
            savedPatternRepository.getByRavelryPatternId(ravelryId) != null

        suspend fun savePattern(detail: PatternDetail): Long =
            savedPatternRepository.saveRavelryPatternIfMissing(detail.toSavedPattern())

        suspend fun findDuplicateFor(detail: PatternDetail): SavedPattern? =
            savedPatternRepository.findDuplicateCandidate(
                pattern = detail.toSavedPattern(),
                includeTitleDesigner = false,
            )

        suspend fun deleteSavedPattern(id: Long) = savedPatternRepository.deleteById(id)

        suspend fun deleteSavedPatterns(ids: List<Long>) = savedPatternRepository.deleteByIds(ids)

        suspend fun getActiveProjectCount(): Int = counterProjectDao.getActiveProjectCount()

        suspend fun createProjectFromPattern(detail: PatternDetail): Long? =
            transactionRunner.run {
                val projectName =
                    ProjectNameRules.uniqueName(
                        requestedName = detail.name,
                        existingNames = counterProjectDao.getAllProjectsOnce().map { it.name },
                    ) ?: return@run null
                val now = System.currentTimeMillis()
                val savedId = savePattern(detail)
                counterProjectDao.insert(
                    CounterProjectEntity(
                        name = projectName,
                        createdAt = now,
                        updatedAt = now,
                        linkedPatternId = savedId,
                    ),
                )
            }
    }

private fun PatternDetail.toSavedPattern(): SavedPattern =
    SavedPattern(
        source = SavedPatternSource.Ravelry,
        ravelryPatternId = id,
        name = name,
        designerName = designer?.name ?: "",
        thumbnailUrl = mainPhotoUrl,
        difficulty = difficultyAverage,
        gaugeStitches = null,
        gaugeRows = rowGauge,
        needleSize = needleSizeText,
        yarnWeight = yarnWeight?.name,
        yardage = yardage ?: yardageMax,
        isFree = free,
        originalUrl = ravelryUrl,
        canonicalUrl = ravelryUrl,
    )
