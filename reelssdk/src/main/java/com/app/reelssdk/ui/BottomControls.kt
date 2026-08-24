package com.app.reelssdk.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.app.reelssdk.model.CuePoint
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerModel

@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    playerModelList: List<ReelsPlayerModel>?,
    index: Int,
    currentPosition: Long,
    duration: Long,
    exoPlayer: ExoPlayer,
    onSeek: (Long) -> Unit,
    cuePoints: List<CuePoint> = emptyList(),
    onDragStateChange: (Boolean) -> Unit = {},
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig()
) {
    val model = playerModelList?.getOrNull(index)
    val isLive = model?.isLive ?: false

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (controlsConfig.seekbar) {
            CustomSlider(
                playerModel = model,
                currentPosition = currentPosition,
                duration = duration,
                cuePoints = cuePoints,
                isLive = isLive,
                allowSpritePreview = controlsConfig.spritePreview,
                exoPlayer = exoPlayer,
                showControls = {},
                onDragStateChange = onDragStateChange,
                onSeek = onSeek
            )
        }
    }
}
