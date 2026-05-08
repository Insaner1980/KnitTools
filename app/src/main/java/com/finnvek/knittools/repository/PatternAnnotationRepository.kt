package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.PatternAnnotationDao
import com.finnvek.knittools.data.local.PatternAnnotationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatternAnnotationRepository
    @Inject
    constructor(
        private val dao: PatternAnnotationDao,
    ) {
        fun getAnnotationsForPage(
            projectId: Long,
            page: Int,
        ): Flow<List<PatternAnnotationEntity>> = dao.getAnnotationsForPage(projectId, page)

        suspend fun addAnnotation(
            projectId: Long,
            page: Int,
            pathData: String,
            color: String,
            strokeWidth: Float,
        ): Long =
            dao.insert(
                PatternAnnotationEntity(
                    projectId = projectId,
                    page = page,
                    pathData = pathData,
                    color = color,
                    strokeWidth = strokeWidth,
                ),
            )

        suspend fun clearProject(projectId: Long) = dao.deleteForProject(projectId)

        suspend fun clearPage(
            projectId: Long,
            page: Int,
        ) = dao.deleteForPage(projectId, page)

        suspend fun deleteById(id: Long) = dao.deleteById(id)
    }
