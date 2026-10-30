package com.finnvek.knittools.ui.layout

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** Dropdowns own a popup window, so the route's content clipping cannot protect them. */
@Composable
internal fun AdaptiveDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    val folds = LocalWindowFolds.current
    if (!LocalAdaptiveModalWindow.current || folds.none { it.isSeparating || it.isOccluding }) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            shape = shape,
            containerColor = containerColor,
            content = content,
        )
        return
    }
    if (!expanded) return
    val root = LocalView.current.rootView
    val location = IntArray(2).also(root::getLocationOnScreen)
    val activityLocation = IntArray(2)
    LocalActivity.current
        ?.window
        ?.decorView
        ?.getLocationOnScreen(activityLocation)
    val origin = IntOffset(location[0] - activityLocation[0], location[1] - activityLocation[1])
    val insets =
        ViewCompat.getRootWindowInsets(root)?.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                WindowInsetsCompat.Type.ime(),
        )
    val available =
        IntRect(
            origin.x + (insets?.left ?: 0),
            origin.y + (insets?.top ?: 0),
            origin.x + root.width - (insets?.right ?: 0),
            origin.y + root.height - (insets?.bottom ?: 0),
        )
    val pane = unobstructedWindowBounds(available, folds, LocalLayoutDirection.current).translate(-origin)
    Popup(
        popupPositionProvider = PaneMenuPositionProvider(pane),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            shadowElevation = 3.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = with(LocalDensity.current) { pane.width.toDp() })
                        .heightIn(max = with(LocalDensity.current) { pane.height.toDp() })
                        .width(IntrinsicSize.Max)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                content = content,
            )
        }
    }
}

internal class PaneMenuPositionProvider(
    private val pane: IntRect,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x =
            if (layoutDirection ==
                LayoutDirection.Ltr
            ) {
                anchorBounds.left
            } else {
                anchorBounds.right - popupContentSize.width
            }
        val y =
            if (anchorBounds.bottom + popupContentSize.height <= pane.bottom) {
                anchorBounds.bottom
            } else {
                anchorBounds.top - popupContentSize.height
            }
        return IntOffset(
            x.coerceIn(pane.left, (pane.right - popupContentSize.width).coerceAtLeast(pane.left)),
            y.coerceIn(pane.top, (pane.bottom - popupContentSize.height).coerceAtLeast(pane.top)),
        )
    }
}
