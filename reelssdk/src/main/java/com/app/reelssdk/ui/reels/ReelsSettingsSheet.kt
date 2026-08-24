package com.app.reelssdk.ui.reels

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.SelectorHeader
import com.app.reelssdk.ui.SettingsLayoutMode

@Composable
fun ReelsSettingsSheet(
    playerModel: ReelsPlayerModel?,
    exoPlayer: ExoPlayer?,
    selectedItemsState: MutableMap<Int, Int>,
    selectedOptionId: Int?,
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig(),
    visible: Boolean = true,
    onSelectedOptionChange: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val scrimInteractionSource = remember { MutableInteractionSource() }
    val sheetInteractionSource = remember { MutableInteractionSource() }
    val sheetBackground = Color(0xFF111111)
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    BackHandler(enabled = visible, onBack = onDismiss)

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 260,
                easing = LinearEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 220,
                easing = LinearEasing
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(
                    interactionSource = scrimInteractionSource,
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = tween(
                    durationMillis = 420,
                    easing = LinearOutSlowInEasing
                )
            ) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 420,
                    easing = LinearOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 260,
                    delayMillis = 80,
                    easing = LinearEasing
                )
            ),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = tween(
                    durationMillis = 320,
                    easing = FastOutLinearInEasing
                )
            ) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 320,
                    easing = FastOutLinearInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 220,
                    easing = LinearEasing
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.34f)
                    .clip(sheetShape)
                    .background(sheetBackground)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = sheetShape
                    )
                    .clickable(
                        interactionSource = sheetInteractionSource,
                        indication = null,
                        onClick = {}
                    )
                    .navigationBarsPadding()
            ) {
                SelectorHeader(
                    playerModel = playerModel,
                    exoPlayer = exoPlayer,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    layoutMode = SettingsLayoutMode.TwoPane,
                    backgroundColor = sheetBackground,
                    selectedItemsState = selectedItemsState,
                    selectedOptionId = selectedOptionId,
                    showCloseIcon = controlsConfig.settingsClose,
                    onSelectedOptionChange = onSelectedOptionChange,
                    closeOptionCard = { isOpen ->
                        if (!isOpen) onDismiss()
                    }
                )
            }
        }
    }
}
