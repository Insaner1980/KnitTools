package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.repository.PatternAnnotationRepository

sealed interface PatternAnnotationCommand {
    suspend fun apply(repository: PatternAnnotationRepository): PatternAnnotationCommand

    data class Insert(
        val annotation: PatternAnnotation,
    ) : PatternAnnotationCommand {
        override suspend fun apply(repository: PatternAnnotationRepository): PatternAnnotationCommand {
            val id = repository.insertAnnotation(annotation)
            return Delete(annotation.copy(id = id))
        }
    }

    data class Delete(
        val annotation: PatternAnnotation,
    ) : PatternAnnotationCommand {
        override suspend fun apply(repository: PatternAnnotationRepository): PatternAnnotationCommand {
            repository.deleteAnnotation(annotation.id)
            return Restore(annotation)
        }
    }

    data class Restore(
        val annotation: PatternAnnotation,
    ) : PatternAnnotationCommand {
        override suspend fun apply(repository: PatternAnnotationRepository): PatternAnnotationCommand {
            repository.restoreBatch(listOf(annotation))
            return Delete(annotation)
        }
    }

    data class Update(
        val before: PatternAnnotation,
        val after: PatternAnnotation,
    ) : PatternAnnotationCommand {
        override suspend fun apply(repository: PatternAnnotationRepository): PatternAnnotationCommand {
            repository.updateAnnotation(after)
            return Update(before = after, after = before)
        }
    }

    data class ClearPage(
        val layerId: Long,
        val page: Int,
        val removed: List<PatternAnnotation>,
    ) : PatternAnnotationCommand {
        override suspend fun apply(repository: PatternAnnotationRepository): PatternAnnotationCommand {
            repository.clearPage(layerId, page)
            return RestorePage(layerId, page, removed)
        }
    }

    data class RestorePage(
        val layerId: Long,
        val page: Int,
        val annotations: List<PatternAnnotation>,
    ) : PatternAnnotationCommand {
        override suspend fun apply(repository: PatternAnnotationRepository): PatternAnnotationCommand {
            repository.restoreBatch(annotations)
            return ClearPage(layerId, page, annotations)
        }
    }
}
