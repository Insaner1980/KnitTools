package com.finnvek.knittools.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.PatternDisplayNames

@Composable
internal fun projectDocumentDisplayName(label: String): String =
    PatternDisplayNames.forDisplay(label, stringResource(R.string.pattern_images_document_fallback))
