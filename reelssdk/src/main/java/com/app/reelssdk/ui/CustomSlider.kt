package com.app.reelssdk.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.app.reelssdk.model.CuePoint
import com.app.reelssdk.model.CueType
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.sprite.SpriteThumbnail
import com.app.reelssdk.ui.sprite.SpriteUtils
import com.app.reelssdk.utils.PlayerUtils.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSlider(
    modifier: Modifier = Modifier,
    playerModel: ReelsPlayerModel? = null,
    currentPosition: Long,
    duration: Long,
    cuePoints: List<CuePoint> = emptyList(),
    onSeek: (Long) -> Unit,
    showControls: (Boolean) -> Unit,
    isLive: Boolean = false,
    allowSpritePreview: Boolean = true,
    exoPlayer: ExoPlayer? = null,
    onDragStateChange: (Boolean) -> Unit = {},
    onPreviewChange: (Long) -> Unit = {}
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    /* ---------- ANIMATIONS ---------- */

    val animatedTrackHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSeeking) 8.dp else 2.dp,
        animationSpec = androidx.compose.animation.core.spring(),
        label = "trackHeight"
    )

    val animatedContainerHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSeeking) 12.dp else 4.dp,
        animationSpec = androidx.compose.animation.core.spring(),
        label = "containerHeight"
    )

    /* ---------- LIVE EDGE ---------- */

    val isAtLiveEdge = remember(currentPosition, duration, isLive) {
        isLive && (duration - currentPosition) < 10_000L
    }

    val explicitSpriteUrl = if (allowSpritePreview) {
        playerModel?.spriteUrl?.takeIf { it.isNotBlank() }
    } else {
        null
    }
    val manifestUrl = remember(playerModel, allowSpritePreview) {
        if (allowSpritePreview) playerModel?.previewManifestUrl() else null
    }
    var resolvedSpriteUrl by remember(playerModel?.id, explicitSpriteUrl, manifestUrl, allowSpritePreview) {
        mutableStateOf(explicitSpriteUrl)
    }

    LaunchedEffect(explicitSpriteUrl, manifestUrl, isLive, allowSpritePreview) {
        if (!allowSpritePreview || isLive) {
            resolvedSpriteUrl = null
            return@LaunchedEffect
        }

        if (!explicitSpriteUrl.isNullOrBlank()) {
            resolvedSpriteUrl = explicitSpriteUrl
            return@LaunchedEffect
        }

        resolvedSpriteUrl = null
        if (!manifestUrl.isNullOrBlank()) {
            resolvedSpriteUrl = withContext(Dispatchers.IO) {
                SpriteUtils.preferredSpriteUrl(
                    SpriteUtils.fetchAndParseManifest(manifestUrl)
                )
            }
        }
    }

    /* ---------- SYNC POSITION ---------- */

    LaunchedEffect(currentPosition, duration) {
        if (!isSeeking && duration > 0) {
            sliderPosition = currentPosition.toFloat() / duration
        }
    }

    /* ---------- LIVE BLINK ---------- */

    val infiniteTransition = rememberInfiniteTransition(label = "liveIndicator")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {

            /* ---------- CURRENT TIME ---------- */

            if (!isLive) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                )
            }

            /* ---------- SEEK BAR ---------- */

            val showSeekPreview = isSeeking && !isLive && duration > 0
            val previewTimeMs =
                ((playerModel?.seekTo ?: 0L) + (sliderPosition * duration).toLong())
                    .coerceAtLeast(0L)
            val hasSpritePreview = !resolvedSpriteUrl.isNullOrBlank()
            val previewSeekbarGap = 4.dp
            val sliderAreaHeight = when {
                !showSeekPreview -> animatedContainerHeight
                hasSpritePreview -> 152.dp + previewSeekbarGap + animatedContainerHeight
                else -> 28.dp + previewSeekbarGap + animatedContainerHeight
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(sliderAreaHeight)
            ) {
                if (showSeekPreview) {
                    val previewBubbleWidth = if (hasSpritePreview) 104.dp else 70.dp
                    val maxPreviewBubbleOffset = (maxWidth - previewBubbleWidth).coerceAtLeast(0.dp)
                    val previewBubbleOffset =
                        ((maxWidth * sliderPosition) - (previewBubbleWidth / 2))
                            .coerceIn(0.dp, maxPreviewBubbleOffset)

                    SeekPreviewBubble(
                        spriteUrl = resolvedSpriteUrl,
                        positionMs = previewTimeMs,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = previewBubbleOffset)
                    )
                }

                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Slider(
                        value = sliderPosition,
                        valueRange = 0f..1f,

                        onValueChange = { value ->
                            sliderPosition = value

                            if (!isSeeking) {
                                isSeeking = true
                                onDragStateChange(true)
                            }

                            showControls(true) // ✅ ALWAYS keep controls visible

                            if (duration > 0) {
                                onPreviewChange((value * duration).toLong())
                            }
                        },

                        onValueChangeFinished = {

                            val baseOffset = playerModel?.seekTo ?: 0L
                            val seekPosition = baseOffset + (sliderPosition * duration).toLong()

                            onSeek(seekPosition)

                            isSeeking = false
                            onDragStateChange(false)
                            showControls(true)
                        },

                        colors = SliderDefaults.colors(
                            thumbColor = Color.Transparent,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),

                        thumb = {
                            Box(modifier = Modifier.size(0.dp))
                        },

                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(animatedContainerHeight)
                            .drawBehind {
                                val trackHeightPx = animatedTrackHeight.toPx()
                                val trackY = size.height / 2 - trackHeightPx / 2

                                drawRoundRect(
                                    color = Color.Gray.copy(alpha = 0.5f),
                                    topLeft = Offset(0f, trackY),
                                    size = Size(size.width, trackHeightPx),
                                    cornerRadius = CornerRadius(trackHeightPx / 2)
                                )

                                drawRoundRect(
                                    color = if (isLive && isAtLiveEdge)
                                        Color.Red
                                    else
                                        Color.Red.copy(alpha = 0.7f),
                                    topLeft = Offset(0f, trackY),
                                    size = Size(size.width * sliderPosition, trackHeightPx),
                                    cornerRadius = CornerRadius(trackHeightPx / 2)
                                )

                                if (duration > 0) {
                                    cuePoints.forEach { cue ->
                                        val x = (cue.positionMs.toFloat() / duration) * size.width
                                        val markerSize =
                                            if (isSeeking) 6.dp.toPx() else 4.dp.toPx()

                                        drawRoundRect(
                                            color = when (cue.type) {
                                                CueType.AD -> Color.Yellow
                                                CueType.L_BAND -> Color.Green
                                            },
                                            topLeft = Offset(
                                                x - markerSize / 2,
                                                size.height / 2 - markerSize / 2
                                            ),
                                            size = Size(markerSize, markerSize),
                                            cornerRadius = CornerRadius(markerSize / 2)
                                        )
                                    }
                                }

                                if (duration > 0) {
                                    val pointerWidthPx =
                                        if (isSeeking) 3.dp.toPx() else trackHeightPx
                                    val maxPointerLeft =
                                        (size.width - pointerWidthPx).coerceAtLeast(0f)
                                    val pointerLeft =
                                        (size.width * sliderPosition - pointerWidthPx / 2)
                                            .coerceIn(0f, maxPointerLeft)

                                    drawRoundRect(
                                        color = Color.Red,
                                        topLeft = Offset(pointerLeft, trackY),
                                        size = Size(pointerWidthPx, trackHeightPx),
                                        cornerRadius = CornerRadius(trackHeightPx / 2)
                                    )
                                }
                            }
                    )
                }
            }

            /* ---------- DURATION / LIVE ---------- */

            if (!isLive) {
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, end = 12.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isAtLiveEdge) Color.Transparent
                            else Color.Gray.copy(alpha = 0.3f)
                        )
                        .clickable {
                            onSeek(duration)
                            exoPlayer?.play()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(if (isAtLiveEdge) blinkAlpha else 1f)
                            .clip(CircleShape)
                            .background(if (isAtLiveEdge) Color.Red else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAtLiveEdge) "LIVE" else "Go Live",
                        color = if (isAtLiveEdge) Color.Red else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SeekPreviewBubble(
    spriteUrl: String?,
    positionMs: Long,
    modifier: Modifier = Modifier
) {
    val hasSprite = !spriteUrl.isNullOrBlank()
    val previewWidth = if (hasSprite) 104.dp else 70.dp
    val previewHeight = if (hasSprite) 142.dp else 28.dp
    val notchHeight = if (hasSprite) 10.dp else 0.dp
    val previewShape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .width(previewWidth)
            .height(previewHeight + notchHeight)
    ) {
        if (!hasSprite) {
            PreviewTimeLabel(
                positionMs = positionMs,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .requiredSize(previewWidth, previewHeight)
                    .shadow(
                        elevation = 12.dp,
                        shape = previewShape,
                        clip = false
                    )
                    .clip(previewShape)
                    .background(Color(0xF0121212))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.68f),
                        shape = previewShape
                    )
            ) {
                SpriteThumbnail(
                    spriteUrl = spriteUrl,
                    positionMs = positionMs,
                    widthDp = 104,
                    heightDp = 142,
                    cornerRadiusDp = 10,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                )

                PreviewTimeLabel(
                    positionMs = positionMs,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
        }

        if (hasSprite) {
            PreviewNotch(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 18.dp, height = notchHeight),
                hasBorder = true
            )
        }
    }
}

@Composable
private fun PreviewTimeLabel(
    positionMs: Long,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatTime(positionMs),
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color.Black.copy(alpha = 0.68f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun PreviewNotch(
    modifier: Modifier = Modifier,
    hasBorder: Boolean
) {
    Canvas(modifier = modifier) {
        val notch = Path().apply {
            moveTo(size.width / 2f, size.height)
            lineTo(0f, 0f)
            lineTo(size.width, 0f)
            close()
        }

        drawPath(
            path = notch,
            color = Color(0xE6121212)
        )

        if (hasBorder) {
            drawPath(
                path = notch,
                color = Color.White.copy(alpha = 0.72f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

private fun ReelsPlayerModel.previewManifestUrl(): String? {
    return listOfNotNull(hlsUrl, mpdUrl, videoUrl)
        .firstOrNull { it.isNotBlank() && it.isManifestUrl() }
}

private fun String.isManifestUrl(): Boolean {
    val clean = substringBefore('?').substringBefore('#')
    return clean.endsWith(".m3u8", ignoreCase = true) ||
        clean.endsWith(".mpd", ignoreCase = true)
}

@Preview(name = "Seek Preview With Sprite", widthDp = 136, heightDp = 176, showBackground = true)
@Composable
private fun SeekPreviewBubbleWithSpritePreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202020))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(152.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .requiredSize(104.dp, 142.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(10.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xF0121212))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.68f),
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                PlaceholderSpriteFrame(modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                )
                PreviewTimeLabel(
                    positionMs = 18_000L,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }

            PreviewNotch(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 18.dp, height = 10.dp),
                hasBorder = true
            )
        }
    }
}

@Preview(name = "Seek Preview Timer Only", widthDp = 104, heightDp = 72, showBackground = true)
@Composable
private fun SeekPreviewBubbleTimerOnlyPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202020))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        SeekPreviewBubble(
            spriteUrl = null,
            positionMs = 18_000L
        )
    }
}

@Composable
private fun PlaceholderSpriteFrame(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF1C2533))
        drawRoundRect(
            color = Color(0xFFBFA16A),
            topLeft = Offset(size.width * 0.06f, size.height * 0.08f),
            size = Size(size.width * 0.88f, size.height * 0.34f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawCircle(
            color = Color(0xFFD7B489),
            radius = size.width * 0.18f,
            center = Offset(size.width * 0.50f, size.height * 0.36f)
        )
        drawRoundRect(
            color = Color(0xFF5B697E),
            topLeft = Offset(size.width * 0.24f, size.height * 0.52f),
            size = Size(size.width * 0.52f, size.height * 0.34f),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.14f),
            topLeft = Offset.Zero,
            size = size
        )
    }
}
