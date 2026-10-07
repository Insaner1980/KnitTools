package com.finnvek.knittools.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R

private val MAX_CONTENT_WIDTH = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: (() -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    wrapTitle: Boolean = false,
    // Näkymä, jonka sisältö näyttää jo saman nimen otsikkona, piilottaa yläpalkin otsikon kunnes se vieritetään pois.
    showTitle: Boolean = true,
    // Näkymän omat yläpalkin toiminnot, esim. ylivuotovalikko harvoin käytetyille toiminnoille.
    // Vain tavallisessa yläpalkissa: slottia ei saa käyttää kahdessa haarassa, eikä yksikään
    // wrapTitle-näkymä tarvitse toimintoja.
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = snackbarHost,
        topBar = {
            if (wrapTitle) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                TopAppBarDefaults.windowInsets,
                            ).heightIn(min = 64.dp)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Box(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        if (showTitle) {
                            Text(title, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    if (onSettings != null) {
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        if (showTitle) {
                            val titleStyle =
                                MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            BasicText(
                                text = title,
                                style = titleStyle,
                                autoSize =
                                    TextAutoSize.StepBased(
                                        minFontSize = 16.sp,
                                        maxFontSize = titleStyle.fontSize,
                                    ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    },
                    actions = {
                        if (onSettings != null) {
                            IconButton(onClick = onSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        actions()
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                )
            }
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH)) {
                content(PaddingValues())
            }
        }
    }
}

/**
 * Yläpalkin otsikko näytölle, jonka sisältö alkaa samalla nimellä: otsikko näkyy vasta kun
 * [headerModifier]illa merkitty nimilohko on vieritetty pois, joten nimi ei näy kahdesti.
 */
@Stable
class ScrollTitleState internal constructor(
    val scrollState: ScrollState,
) {
    private var headerHeightPx by mutableIntStateOf(0)

    val showTitle: Boolean by derivedStateOf { headerHeightPx > 0 && scrollState.value >= headerHeightPx }

    val headerModifier: Modifier = Modifier.onSizeChanged { headerHeightPx = it.height }
}

@Composable
fun rememberScrollTitleState(): ScrollTitleState {
    val scrollState = rememberScrollState()
    return remember(scrollState) { ScrollTitleState(scrollState) }
}
