package com.finnvek.knittools.ui.tips

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.domain.tips.TipContext
import com.finnvek.knittools.domain.tips.TipId
import com.finnvek.knittools.domain.tips.TipRules
import com.finnvek.knittools.domain.tips.TipScreen
import com.finnvek.knittools.ui.theme.OnboardingDimens
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

internal data class TipPlacement(
    val bubble: Rect,
    val above: Boolean,
)

private data class MeasuredTip(
    val id: TipId,
    val placement: TipPlacement?,
)

private data class TipHostEnvironment(
    val routeKey: String,
    val ready: Boolean,
    val blocked: Boolean,
    val viewport: Rect,
)

private val LocalTipHostEnvironment = staticCompositionLocalOf<TipHostEnvironment?> { null }

@Composable
fun TipHost(
    context: TipContext,
    onAction: (TipId) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentContent by rememberUpdatedState(content)
    val contentSlot = remember { movableContentOf { currentContent() } }
    val session = LocalTipSession.current
    if (session == null) {
        contentSlot()
        return
    }
    val state by session.state.collectAsStateWithLifecycle()
    val routeKey = LocalTipRouteKey.current
    val registry = remember { TipAnchorRegistry() }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow
        .collectAsState()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val blocked =
        LocalTipBlocked.current ||
            context.blocked ||
            keyboardVisible ||
            registry.focusedFields.isNotEmpty() ||
            lifecycle != Lifecycle.State.RESUMED
    val requestedContext = state.contextFor(context.copy(blocked = blocked))
    val canStartEntryDelay =
        requestedContext.screenReady && lifecycle == Lifecycle.State.RESUMED && state.routeKey == routeKey
    val entryDelayReady =
        remember(routeKey, state.routeEntryGeneration, context.screen, context.projectId, canStartEntryDelay) {
            mutableStateOf(false)
        }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    val environment = TipHostEnvironment(routeKey, entryDelayReady.value, blocked, viewport)
    val effectiveContext = state.contextFor(context.withEnvironment(environment))
    val workSessionRequest = state.workSessionRequestFor(routeKey, requestedContext)
    val presentation = state.presentation?.takeIf { it.routeKey == routeKey }
    val requestedPresentation = presentation != null && presentation == state.workSessionRequest
    val presentationContext = if (requestedPresentation) requestedContext else effectiveContext
    val presentationProject = presentation?.projectId
    val presentationMatchesProject = presentationProject == null || presentationProject == effectiveContext.projectId
    val presentationAllowed = presentationMatchesProject && (!requestedPresentation || workSessionRequest != null)
    val candidate =
        when {
            presentation != null -> presentation.id.takeIf { presentationAllowed }
            state.presentation != null -> null
            state.workSessionRequest != null -> workSessionRequest?.id
            else -> TipRules.next(effectiveContext)
        }
    val overlayId = candidate?.takeUnless { it == TipId.BACKUP }
    var measured by remember(routeKey, overlayId, context.projectId) { mutableStateOf<MeasuredTip?>(null) }
    val bubbleBounds =
        if (presentation?.id == TipId.BACKUP) {
            registry.anchors[TipId.BACKUP]?.bounds
        } else {
            measured?.takeIf { it.id == presentation?.id }?.placement?.bubble
        }
    val currentOutsideDismiss =
        rememberUpdatedState(
            presentation?.let { shown ->
                { session.dismiss(routeKey, shown.id) }
            },
        )
    val outsideDismiss =
        Modifier.dismissTipOutside(
            rememberUpdatedState(viewport),
            rememberUpdatedState(bubbleBounds),
            currentOutsideDismiss,
        )

    LaunchedEffect(entryDelayReady) {
        if (canStartEntryDelay) {
            delay(OnboardingDimens.TIP_ENTRY_DELAY_MILLIS)
            entryDelayReady.value = true
        }
    }
    LaunchedEffect(routeKey, requestedContext, state.workSessionRequest) {
        session.cancelInvalidWorkSessionRequest(routeKey, requestedContext)
    }
    LaunchedEffect(
        routeKey,
        state.routeEntryGeneration,
        context.screen,
        context.projectId,
        requestedContext.screenReady,
        lifecycle,
    ) {
        if (context.screen == TipScreen.COUNTER &&
            requestedContext.screenReady &&
            lifecycle == Lifecycle.State.RESUMED
        ) {
            context.projectId?.let { session.recordCounterOpen(routeKey, it, LocalDate.now().toEpochDay()) }
        }
    }
    LaunchedEffect(overlayId, measured, effectiveContext, presentation, workSessionRequest, requestedContext) {
        if (presentation != null || overlayId == null) return@LaunchedEffect
        val hasPlacement = measured?.let { it.id == overlayId && it.placement != null } == true
        if (!hasPlacement) return@LaunchedEffect
        if (workSessionRequest != null) {
            session.tryShowRequestedWorkSession(routeKey, requestedContext)
        } else if (TipRules.next(effectiveContext) == overlayId) {
            session.tryShow(routeKey, overlayId, effectiveContext.projectId)
        }
    }
    LaunchedEffect(presentation, blocked, presentationContext.screenReady, measured, presentationAllowed) {
        val shown = presentation ?: return@LaunchedEffect
        val placementLost = shown.id != TipId.BACKUP && measured?.id == shown.id && measured?.placement == null
        if (blocked || !presentationContext.screenReady || placementLost || !presentationAllowed) {
            session.releasePresentation(shown)
        }
    }
    LaunchedEffect(presentation) {
        if (presentation?.id == TipId.UNDO) {
            delay(OnboardingDimens.TIP_UNDO_HIDE_MILLIS)
            session.dismiss(routeKey, TipId.UNDO)
        }
    }
    DisposableEffect(session, routeKey) {
        onDispose {
            session.state.value.presentation?.takeIf { it.routeKey == routeKey }?.let {
                session.releasePresentation(it)
            }
        }
    }

    Box(modifier.fillMaxSize().onGloballyPositioned { viewport = it.boundsInRoot() }.then(outsideDismiss)) {
        CompositionLocalProvider(LocalTipAnchors provides registry, LocalTipHostEnvironment provides environment) {
            contentSlot()
        }
        if (overlayId != null) {
            key(overlayId) {
                MeasuredTipOverlay(
                    id = overlayId,
                    context = effectiveContext,
                    viewport = viewport,
                    anchor = registry.anchors[overlayId]?.bounds,
                    protectedBounds = registry.protectedBounds.values.toList(),
                    presentation =
                        presentation?.takeIf {
                            it.id == overlayId && presentationAllowed && presentationContext.screenReady && !blocked
                        },
                    onMeasured = { measured = it },
                    onVisible = session::markPresentationVisible,
                    onDismiss = { session.dismiss(routeKey, overlayId) },
                    onAction = {
                        session.dismiss(routeKey, overlayId, acted = true)
                        onAction(overlayId)
                    },
                )
            }
        }
    }
}

private fun TipContext.withEnvironment(environment: TipHostEnvironment): TipContext =
    copy(
        screenReady = screenReady && environment.ready,
        blocked = blocked || environment.blocked,
        millisSinceEntry = if (environment.ready) TipRules.ENTRY_DELAY_MILLIS else 0L,
    )

private fun Modifier.dismissTipOutside(
    viewport: State<Rect>,
    bubble: State<Rect?>,
    onDismiss: State<(() -> Unit)?>,
): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val down = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                if (down != null && bubble.value?.contains(down.position + viewport.value.topLeft) != true) {
                    onDismiss.value?.invoke()
                }
            }
        }
    }

@Composable
private fun MeasuredTipOverlay(
    id: TipId,
    context: TipContext,
    viewport: Rect,
    anchor: Rect?,
    protectedBounds: List<Rect>,
    presentation: TipPresentation?,
    onMeasured: (MeasuredTip) -> Unit,
    onVisible: (TipPresentation) -> Unit,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
) {
    val text = tipText(id, context)
    val actionLabel = tipActionLabel(id)
    val visible = presentation != null
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(OnboardingDimens.TIP_ENTER_DURATION_MILLIS),
        label = "stitchMarkerTipEnter",
    )
    SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
        val slideDistance = OnboardingDimens.TipSlideDistance.toPx()
        val motionProtectedBounds =
            protectedBounds.map { Rect(it.left, it.top - slideDistance, it.right, it.bottom + slideDistance) }
        val bubbleConstraints =
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxWidth =
                    minOf(
                        constraints.maxWidth,
                        OnboardingDimens.TipMaxWidth.roundToPx(),
                    ),
            )
        val measuredBubble =
            subcompose("measurement") {
                StitchMarkerTip(
                    text,
                    onDismiss,
                    actionLabel = actionLabel,
                    onAction = onAction,
                    exposeSemantics = false,
                )
            }.single().measure(bubbleConstraints)
        val placement =
            anchor?.let {
                findTipPlacement(
                    viewport,
                    it,
                    IntSize(measuredBubble.width, measuredBubble.height),
                    motionProtectedBounds,
                    OnboardingDimens.TipMarkerSize.toPx(),
                    OnboardingDimens.TipScreenInset.toPx(),
                )
            }
        onMeasured(MeasuredTip(id, placement))
        val bubble =
            if (presentation != null && placement != null) {
                subcompose("bubble") {
                    LaunchedEffect(presentation, progress > 0f) {
                        if (progress > 0f) {
                            // Kertamerkintä odottaa ruutua, jossa kupla on aidosti näkyvä.
                            withFrameNanos {}
                            onVisible(presentation)
                        }
                    }
                    StitchMarkerTip(text, onDismiss, actionLabel = actionLabel, onAction = onAction)
                }.single().measure(bubbleConstraints)
            } else {
                null
            }
        val decoration =
            if (visible && placement != null) {
                subcompose("decoration") {
                    TipAnchorDecoration(
                        placement.bubble.translate(-viewport.topLeft),
                        anchor.translate(-viewport.topLeft),
                        placement.above,
                        Modifier.fillMaxSize(),
                    )
                }.single().measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
            } else {
                null
            }
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (visible && placement != null) {
                decoration?.placeWithLayer(0, 0) { alpha = progress }
                bubble?.placeWithLayer(
                    (placement.bubble.left - viewport.left).roundToInt(),
                    (placement.bubble.top - viewport.top).roundToInt(),
                ) {
                    alpha = progress
                    translationY = (1f - progress) * slideDistance *
                        if (placement.above) 1f else -1f
                }
            }
        }
    }
}

/** Molemmat sijoitukset tarkistetaan koko suojattua ohjainjoukkoa vasten ennen näyttöbudjetin käyttöä. */
internal fun findTipPlacement(
    viewport: Rect,
    anchor: Rect,
    bubble: IntSize,
    protectedBounds: List<Rect>,
    gap: Float,
    inset: Float,
): TipPlacement? {
    if (anchor.isEmpty || viewport.isEmpty || bubble.width > viewport.width - inset * 2f) return null
    if (anchor.left < viewport.left ||
        anchor.top < viewport.top ||
        anchor.right > viewport.right ||
        anchor.bottom > viewport.bottom
    ) {
        return null
    }
    val left =
        (anchor.center.x - bubble.width / 2f).coerceIn(
            viewport.left + inset,
            viewport.right - inset - bubble.width,
        )
    return listOf(true, false).firstNotNullOfOrNull { above ->
        val top = if (above) anchor.top - gap - bubble.height else anchor.bottom + gap
        val bounds = Rect(left, top, left + bubble.width, top + bubble.height)
        val fits = bounds.top >= viewport.top + inset && bounds.bottom <= viewport.bottom - inset
        if (fits && !bounds.overlaps(anchor) && protectedBounds.none(bounds::overlaps)) {
            TipPlacement(bounds, above)
        } else {
            null
        }
    }
}

/** Varmuuskopioinnin kertaluonteinen vihje käyttää listan omaa tilaa, ei kelluvaa kuplaa. */
@Composable
fun InlineTipCard(
    id: TipId,
    context: TipContext,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(id == TipId.BACKUP)
    val session = LocalTipSession.current ?: return
    val environment = LocalTipHostEnvironment.current ?: return
    val registry = LocalTipAnchors.current ?: return
    val state by session.state.collectAsStateWithLifecycle()
    val effectiveContext = state.contextFor(context.withEnvironment(environment))
    val presentation = state.presentation?.takeIf { it.routeKey == environment.routeKey && it.id == id }
    val shown = presentation != null
    val eligible = TipRules.next(effectiveContext) == id && state.presentation == null
    if (!shown && !eligible) return
    var size by remember { mutableStateOf(IntSize.Zero) }
    val bounds = registry.anchors[id]?.bounds
    val fullyVisible =
        bounds != null &&
            bounds.width >= size.width &&
            bounds.height >= size.height &&
            !bounds.isEmpty &&
            bounds.left >= environment.viewport.left &&
            bounds.top >= environment.viewport.top &&
            bounds.right <= environment.viewport.right &&
            bounds.bottom <= environment.viewport.bottom
    LaunchedEffect(eligible, fullyVisible, bounds) {
        if (eligible && fullyVisible) session.tryShow(environment.routeKey, id, effectiveContext.projectId)
    }
    LaunchedEffect(presentation, fullyVisible, effectiveContext.blocked) {
        if (presentation == null) return@LaunchedEffect
        if (!fullyVisible || effectiveContext.blocked) {
            session.releasePresentation(presentation)
        } else {
            withFrameNanos {}
            session.markPresentationVisible(presentation)
        }
    }
    Layout(
        content = {
            StitchMarkerTip(
                text = tipText(id, effectiveContext),
                onDismiss = { session.dismiss(environment.routeKey, id) },
                actionLabel = tipActionLabel(id),
                exposeSemantics = shown && fullyVisible && !effectiveContext.blocked,
                onAction = {
                    session.dismiss(environment.routeKey, id, acted = true)
                    onAction()
                },
            )
        },
        modifier = modifier.tipAnchor(id).onSizeChanged { size = it },
    ) { measurables, constraints ->
        val card = measurables.single().measure(constraints)
        layout(card.width, card.height) {
            if (shown && !effectiveContext.blocked) card.place(0, 0)
        }
    }
}
