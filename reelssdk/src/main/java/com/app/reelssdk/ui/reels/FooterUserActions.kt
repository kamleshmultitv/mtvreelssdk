package com.app.reelssdk.ui.reels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerCustomControls


@Composable
fun FooterUserActions(
    customControls: ReelsPlayerCustomControls? = null,
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig(),
    isWatchlisted: Boolean = false,
    onWatchlistClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    isFullScreen: Boolean = true,
    onFullScreenClick: () -> Unit = {}
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(end = 12.dp)
    ) {

        if (controlsConfig.share) {
            FooterAction(
                resId = customControls?.shareIconRes,
                defaultIcon = Icons.Default.Share,
                text = "Share",
                iconTintRes = customControls?.iconTintRes,
                showText = controlsConfig.showActionLabels,
                onClick = onShareClick
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (controlsConfig.settings) {
            FooterAction(
                resId = customControls?.settingsIconRes,
                defaultIcon = Icons.Default.Settings,
                text = "Settings",
                iconTintRes = customControls?.iconTintRes,
                showText = controlsConfig.showActionLabels,
                onClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        val showFullScreenButton =
            if (isFullScreen) controlsConfig.exitFullscreen else controlsConfig.fullscreen

        if (controlsConfig.watchlist) {
            val watchlistText = if (isWatchlisted) "Watchlisted" else "Watchlist"
            FooterAction(
                resId = if (isWatchlisted) {
                    customControls?.watchlistedIconRes
                } else {
                    customControls?.watchlistIconRes
                },
                defaultIcon = if (isWatchlisted) {
                    Icons.AutoMirrored.Filled.PlaylistAddCheck
                } else {
                    Icons.AutoMirrored.Filled.PlaylistAdd
                },
                text = watchlistText,
                iconTintRes = customControls?.iconTintRes,
                showText = controlsConfig.showActionLabels,
                onClick = onWatchlistClick
            )

            if (showFullScreenButton) {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        if (showFullScreenButton) {
            FooterAction(
                resId = if (isFullScreen) {
                    customControls?.exitFullScreenIconRes
                } else {
                    customControls?.fullScreenIconRes
                },
                defaultIcon = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                text = if (isFullScreen) "Collapse" else "Expand",
                iconTintRes = customControls?.iconTintRes,
                showText = controlsConfig.showActionLabels,
                onClick = onFullScreenClick
            )
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun FooterAction(
    resId: Int?,
    defaultIcon: ImageVector,
    text: String,
    iconTintRes: Int?,
    showText: Boolean,
    onClick: () -> Unit
) {
    if (showText) {
        UserActionWithText(
            resId = resId,
            defaultIcon = defaultIcon,
            text = text,
            iconTintRes = iconTintRes,
            onClick = onClick
        )
    } else {
        UserActionWithoutText(
            resId = resId,
            defaultIcon = defaultIcon,
            iconTintRes = iconTintRes,
            contentDescription = text,
            onClick = onClick
        )
    }
}
