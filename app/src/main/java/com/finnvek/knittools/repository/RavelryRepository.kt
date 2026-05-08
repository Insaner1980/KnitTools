package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.data.remote.PatternSearchParams
import com.finnvek.knittools.data.remote.PatternSearchResponse
import com.finnvek.knittools.data.remote.RavelryApiService
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
    ) {
        suspend fun searchPatterns(params: PatternSearchParams): PatternSearchResponse = api.searchPatterns(params)

        suspend fun getPatternDetail(id: Int): PatternDetail = api.getPatternDetail(id)

        fun getSavedPatterns(): Flow<List<SavedPatternEntity>> = savedPatternRepository.getAll()

        fun getSavedPatternCount(): Flow<Int> = savedPatternRepository.getCount()

        suspend fun isPatternSaved(ravelryId: Int): Boolean = savedPatternRepository.getByRavelryId(ravelryId) != null

        suspend fun savePattern(detail: PatternDetail): Long =
            savedPatternRepository.save(
                SavedPatternEntity(
                    ravelryId = detail.id,
                    name = detail.name,
                    designerName = detail.designer?.name ?: "",
                    thumbnailUrl = detail.mainPhotoUrl,
                    difficulty = detail.difficultyAverage,
                    gaugeStitches = null,
                    gaugeRows = detail.rowGauge,
                    needleSize = detail.needleSizeText,
                    yarnWeight = detail.yarnWeight?.name,
                    yardage = detail.yardage ?: detail.yardageMax,
                    isFree = detail.free,
                    patternUrl = detail.ravelryUrl,
                ),
            )

        suspend fun deleteSavedPattern(id: Long) = savedPatternRepository.deleteById(id)

        suspend fun createProjectFromPattern(detail: PatternDetail): Long {
            val savedId = savePattern(detail)
            return counterProjectDao.insert(
                CounterProjectEntity(
                    name = detail.name,
                    linkedPatternId = savedId,
                ),
            )
        }
    }
