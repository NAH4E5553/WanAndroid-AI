package com.personal.wanandroid.core.ui.component.list

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.personal.wanandroid.core.ui.R
import kotlin.math.roundToInt

private enum class SwipeRevealPosition { Closed, Action }

@Stable
class SwipeRevealListState<K : Any> internal constructor() {
    var revealedKey: K? by mutableStateOf(null)
        private set

    internal val collapseOnVerticalScroll = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && available.y != 0f) close()
            return Offset.Zero
        }
    }

    fun isRevealed(key: K): Boolean = revealedKey == key

    fun reveal(key: K) {
        revealedKey = key
    }

    fun close(key: K) {
        if (revealedKey == key) close()
    }

    fun close() {
        revealedKey = null
    }
}

@Composable
fun <K : Any> rememberSwipeRevealListState(): SwipeRevealListState<K> =
    remember { SwipeRevealListState() }

fun Modifier.collapseSwipeRevealOnVerticalScroll(state: SwipeRevealListState<*>): Modifier =
    nestedScroll(state.collapseOnVerticalScroll)

/**
 * A single destructive list action revealed by dragging the foreground to the left.
 * The caller owns which key is revealed so a list can keep at most one action open.
 */
@Composable
fun SwipeRevealActionItem(
    itemKey: Any,
    revealed: Boolean,
    actionContentDescription: String,
    actionIconRes: Int = R.drawable.ic_delete,
    onRevealed: () -> Unit,
    onClosed: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    foregroundModifier: Modifier = Modifier,
    actionModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    actionWidth: Dp = 72.dp,
    enabled: Boolean = true,
    actionEnabled: Boolean = enabled,
    content: @Composable () -> Unit
) {
    val state = remember(itemKey) { AnchoredDraggableState(SwipeRevealPosition.Closed) }
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    SideEffect {
        state.updateAnchors(
            DraggableAnchors {
                SwipeRevealPosition.Closed at 0f
                SwipeRevealPosition.Action at -actionWidthPx
            }
        )
    }
    LaunchedEffect(revealed) {
        if (!revealed && state.settledValue != SwipeRevealPosition.Closed) {
            state.animateTo(SwipeRevealPosition.Closed)
        }
    }
    LaunchedEffect(state.settledValue) {
        when (state.settledValue) {
            SwipeRevealPosition.Closed -> onClosed()
            SwipeRevealPosition.Action -> onRevealed()
        }
    }
    Box(modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.matchParentSize().padding(contentPadding),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxHeight().width(actionWidth).then(actionModifier)
            ) {
                if (state.targetValue == SwipeRevealPosition.Action) {
                    IconButton(
                        onClick = onAction,
                        enabled = actionEnabled,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(actionIconRes),
                            contentDescription = actionContentDescription,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(contentPadding).offset {
                IntOffset(state.requireOffset().roundToInt(), 0)
            }.anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
                enabled = enabled
            ).then(foregroundModifier)
        ) {
            content()
        }
    }
}
