package com.app.reelssdk.ui.reels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.SelectorHeader
import com.app.reelssdk.ui.SettingsLayoutMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsSettingsSheet(
    playerModel: ReelsPlayerModel?,
    exoPlayer: ExoPlayer?,
    selectedItemsState: MutableMap<Int, Int>,
    selectedOptionId: Int?,
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig(),
    onSelectedOptionChange: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetBackground = Color(0xFF111111)

    ModalBottomSheet(
        onDismissRequest = {
            exoPlayer?.play()
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth(),
        sheetState = sheetState,
        containerColor = sheetBackground,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.34f)
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
