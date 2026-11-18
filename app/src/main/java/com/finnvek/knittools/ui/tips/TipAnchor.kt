package com.finnvek.knittools.ui.tips

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import com.finnvek.knittools.domain.tips.TipId

internal data class TipAnchorBounds(
    val owner: Any,
    val bounds: Rect,
)

internal class TipAnchorRegistry {
    val anchors = mutableStateMapOf<TipId, TipAnchorBounds>()
    val protectedBounds = mutableStateMapOf<Any, Rect>()
    val focusedFields = mutableStateMapOf<Any, Boolean>()

    fun remove(owner: Any) {
        anchors.entries.removeAll { it.value.owner === owner }
        protectedBounds.remove(owner)
        focusedFields.remove(owner)
    }
}

internal val LocalTipAnchors = staticCompositionLocalOf<TipAnchorRegistry?> { null }

fun Modifier.tipAnchor(id: TipId): Modifier = this then TipAnchorElement(id, protectTarget = false)

/** Laskurin ohjaimet suojataan myös silloin, kun vihje on kiinni toisessa ohjaimessa. */
fun Modifier.tipProtected(): Modifier = this then TipAnchorElement(id = null, protectTarget = true)

fun Modifier.tipBlockWhenFocused(): Modifier = this then TipFocusBlockElement

private data object TipFocusBlockElement : ModifierNodeElement<TipFocusBlockNode>() {
    override fun create() = TipFocusBlockNode()

    override fun update(node: TipFocusBlockNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "tipBlockWhenFocused"
    }
}

private class TipFocusBlockNode :
    Modifier.Node(),
    FocusEventModifierNode,
    CompositionLocalConsumerModifierNode {
    private var registry: TipAnchorRegistry? = null

    override fun onFocusEvent(focusState: FocusState) {
        val currentRegistry = currentValueOf(LocalTipAnchors)
        if (registry !== currentRegistry) registry?.focusedFields?.remove(this)
        registry = currentRegistry
        if (focusState.hasFocus) {
            currentRegistry?.focusedFields?.set(this, true)
        } else {
            currentRegistry?.focusedFields?.remove(this)
        }
    }

    override fun onDetach() {
        registry?.focusedFields?.remove(this)
        registry = null
    }
}

private data class TipAnchorElement(
    val id: TipId?,
    val protectTarget: Boolean,
) : ModifierNodeElement<TipAnchorNode>() {
    override fun create() = TipAnchorNode(id, protectTarget)

    override fun update(node: TipAnchorNode) {
        node.update(id, protectTarget)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = if (protectTarget) "tipProtected" else "tipAnchor"
        properties["id"] = id
    }
}

private class TipAnchorNode(
    private var id: TipId?,
    private var protectTarget: Boolean,
) : Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var registry: TipAnchorRegistry? = null
    private var bounds: Rect? = null

    fun update(
        newId: TipId?,
        newProtectTarget: Boolean,
    ) {
        registry?.remove(this)
        id = newId
        protectTarget = newProtectTarget
        publishBounds()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val currentRegistry = currentValueOf(LocalTipAnchors)
        if (registry !== currentRegistry) registry?.remove(this)
        registry = currentRegistry
        bounds = coordinates.boundsInRoot()
        publishBounds()
    }

    private fun publishBounds() {
        val currentBounds = bounds ?: return
        val currentRegistry = registry ?: return
        id?.let { currentRegistry.anchors[it] = TipAnchorBounds(this, currentBounds) }
        if (protectTarget) currentRegistry.protectedBounds[this] = currentBounds
    }

    override fun onDetach() {
        registry?.remove(this)
        registry = null
        bounds = null
    }
}
