package com.app.reelssdk.ui

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.app.reelssdk.listener.AdsListener
import com.app.reelssdk.listener.ReelsPlayerStateListener
import com.app.reelssdk.model.CuePoint
import com.app.reelssdk.model.CueType
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerCustomControls
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.ads.LShapeAdContainer
import com.app.reelssdk.ui.reels.ReelsShareSheet
import com.app.reelssdk.ui.reels.ReelsSettingsSheet
import com.app.reelssdk.utils.PlayerUtils
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun MainContainer(
    contentList: List<ReelsPlayerModel>?,
    index: Int? = 0,
    playerStateListener: ReelsPlayerStateListener?,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit,
    startInFullScreen: Boolean = false,
    isFullScreen: Boolean = startInFullScreen,
    onFullScreenModeChanged: (Boolean) -> Unit = {},
    isReelPageActive: Boolean = true,
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig(),
    showEpisodeSelector: Boolean = false,
    isEpisodeSelectorExpanded: Boolean = false,
    isInPictureInPictureMode: Boolean = false,
    onControlsVisibilityChanged: (Boolean) -> Unit = {},
    onPlaybackCompleted: () -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        MainContainerPreviewSurface(
            playerModel = contentList?.getOrNull(index ?: 0),
            isReelPageActive = isReelPageActive
        )
        return
    }

    val context = LocalContext.current
    var contentDuration by remember { mutableLongStateOf(0L) }


    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    val safeIndex = remember(contentList, index) {
        val size = contentList?.size ?: 0
        when {
            size == 0 -> 0
            index == null -> 0
            index < 0 -> 0
            index >= size -> size - 1
            else -> index
        }
    }

    val selectedIndex = remember { mutableIntStateOf(safeIndex) }

    LaunchedEffect(index, contentList) {
        val size = contentList?.size ?: return@LaunchedEffect
        selectedIndex.intValue = index?.coerceIn(0, size - 1) ?: 0
    }

    val playerModel = contentList?.getOrNull(selectedIndex.intValue)
    val modelControlsConfig = playerModel?.controlsConfig ?: controlsConfig

    var isControllerVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isPlayerPlaying by remember { mutableStateOf(false) }
    var isSeekbarDragging by remember { mutableStateOf(false) }
    var isSettingsClick by remember { mutableStateOf(false) }
    var isShareSheetClick by remember { mutableStateOf(false) }
    var fastForwardIndicatorSide by remember { mutableStateOf<HoldSide?>(null) }
    var isHoldMuteIndicatorVisible by remember { mutableStateOf(false) }
    val reelsSettingsSelectedItems = remember(selectedIndex.intValue) {
        mutableStateMapOf<Int, Int>()
    }
    var reelsSettingsSelectedOption by remember(selectedIndex.intValue) {
        mutableStateOf<Int?>(null)
    }
    var isAdsShowing by remember { mutableStateOf(false) }
    var showLShapeAd by remember { mutableStateOf(false) }
    val triggeredLBands = remember { mutableSetOf<String>() }
    val coroutineScope = rememberCoroutineScope()
    val currentOnPlaybackCompleted by rememberUpdatedState(onPlaybackCompleted)

    val imaCuePoints = remember {
        mutableStateListOf<CuePoint>()
    }

    val isLive = playerModel?.isLive == true

    val playbackUrl = remember(playerModel) {
        val url = when {
            // Live content always uses liveUrl
            isLive -> playerModel.liveUrl

            // DRM content must use MPD
            playerModel?.drm == "1" && !playerModel.mpdUrl.isNullOrEmpty() -> playerModel.mpdUrl

            // Non‑DRM: prefer HLS, then MPD as fallback
            !playerModel?.hlsUrl.isNullOrEmpty() -> playerModel.hlsUrl
            !playerModel?.mpdUrl.isNullOrEmpty() -> playerModel.mpdUrl

            else -> ""
        }

        // ✅ DEBUG: Log URL selection
        android.util.Log.d(
            "MtvReelsPlayerSdk",
            "Selected playbackUrl: $url (DRM=${playerModel?.drm}, isLive=$isLive)"
        )

        url
    }

    val subtitleUri = if (isLive) "" else playerModel?.srt.orEmpty()

    var hasReleasedStartupBitrate by remember(selectedIndex.intValue, playbackUrl) { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(selectedIndex.intValue, playbackUrl) { mutableStateOf(false) }
    val posterUrl = remember(playerModel) {
        playerModel?.thumbnail?.takeIf { it.isNotBlank() }
            ?: playerModel?.imageUrl?.takeIf { it.isNotBlank() }
    }

    val adsListener = remember {
        object : AdsListener {

            override fun onAdsLoaded() {
                isControllerVisible = false
                isAdsShowing = true
            }

            override fun onAdStarted() {
                isControllerVisible = false
                isAdsShowing = true
                showLShapeAd = false
                playerStateListener?.onAdStateChanged(true)
            }

            override fun onAdCompleted() {
                isControllerVisible = true
                isAdsShowing = false
                playerStateListener?.onAdStateChanged(false)

                showLShapeAd = true

                coroutineScope.launch {
                    delay(200000)
                    showLShapeAd = false
                }
            }

            override fun onAllAdsCompleted() {
                isControllerVisible = true
                isAdsShowing = false
            }

            override fun onAdError(message: String) {
                isControllerVisible = true
                isAdsShowing = false
            }
        }
    }

    val playerWithAds = remember(selectedIndex.intValue, playbackUrl) {
        val model = playerModel ?: return@remember null
        val urlString = playbackUrl ?: ""

        android.util.Log.d(
            "MtvReelsPlayerSdk",
            "Creating player with url: $urlString, drmToken: ${if (model.drmToken.isNullOrBlank()) "null" else "present"}"
        )

        PlayerUtils.createPlayer(
            context = context,
            contentList,
            selectedIndex.intValue,
            videoUrl = urlString,
            drmToken = model.drmToken,
            srt = subtitleUri,
            playerView = playerView,
            adsConfig = model.adsConfig,
            adsListener = adsListener,
            playWhenReady = isReelPageActive,
            fastStart = true
        )
    }

    val exoPlayer = playerWithAds?.first
    val adsLoader = playerWithAds?.second

    val lBandCuePoints = remember(contentDuration, selectedIndex.intValue) {
        val interval = playerModel?.gamAdsConfig?.timeIntervalInMilliseconds
        if (contentDuration <= 0L || interval == null || interval <= 0L) {
            emptyList()
        } else {
            val cueList = mutableListOf<CuePoint>()
            var position = interval
            var count = 1
            while (position < contentDuration) {
                cueList.add(
                    CuePoint(
                        positionMs = position,
                        id = "lband_$count",
                        type = CueType.L_BAND
                    )
                )
                position += interval   // now safe (Long, not nullable)
                count++
            }
            cueList
        }
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}

        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
                if (!hasReleasedStartupBitrate) {
                    hasReleasedStartupBitrate = true
                    player.trackSelectionParameters =
                        player.trackSelectionParameters
                            .buildUpon()
                            .setMaxVideoBitrate(Int.MAX_VALUE)
                            .setForceHighestSupportedBitrate(false)
                            .build()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_IDLE -> {
                        isLoading = false
                    }

                    Player.STATE_BUFFERING -> {
                        isLoading = true
                        playerStateListener?.onBuffering(true)
                    }

                    Player.STATE_READY -> {

                        isLoading = false

                        val d = player.duration

                        if (d != C.TIME_UNSET && d > 0) {
                            contentDuration = d
                        }

                        playerStateListener?.onBuffering(false)

                        val duration =
                            if (player.duration != C.TIME_UNSET && player.duration > 0)
                                player.duration
                            else
                                0L

                        playerStateListener?.onPlayerReady(duration)

                        exoPlayer.trackSelectionParameters =
                            exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setForceHighestSupportedBitrate(false)
                                .build()
                    }


                    Player.STATE_ENDED -> {
                        isLoading = false
                        playerStateListener?.onPlaybackCompleted()
                        currentOnPlaybackCompleted()
                    }

                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                tracks.groups.forEach { group ->
                    Log.d("TRACK", "TrackGroup type=${group.type}")
                    for (i in 0 until group.mediaTrackGroup.length) {
                        val format = group.mediaTrackGroup.getFormat(i)
                        Log.d(
                            "TRACK",
                            "  mime=${format.sampleMimeType}, codecs=${format.codecs}"
                        )
                    }
                }
            }


            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayerPlaying = isPlaying
                playerStateListener?.onPlayStateChanged(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("MtvReelsPlayerSdk", "❌ PLAYER ERROR", error)
                android.util.Log.e("MtvReelsPlayerSdk", "Error code: ${error.errorCode}")
                android.util.Log.e("MtvReelsPlayerSdk", "Error message: ${error.message}")
                android.util.Log.e("MtvReelsPlayerSdk", "Cause: ${error.cause?.message}")
                playerStateListener?.onPlayerError(error)
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (!timeline.isEmpty) {
                    val d = player.duration
                    if (d > 0) {
                        contentDuration = d
                    }
                }

                if (timeline.isEmpty) return
                val period = Timeline.Period()
                timeline.getPeriod(0, period)
                imaCuePoints.clear()
                for (adGroupIndex in 0 until period.adGroupCount) {
                    val timeUs = period.getAdGroupTimeUs(adGroupIndex)
                    val positionMs =
                        if (timeUs == C.TIME_END_OF_SOURCE) exoPlayer.duration else timeUs / 1000
                    imaCuePoints.add(
                        CuePoint(
                            id = "ima_$adGroupIndex",
                            positionMs = positionMs,
                            type = CueType.AD
                        )
                    )
                }
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            adsLoader?.setPlayer(null)
            player.release()
        }
    }

    LaunchedEffect(selectedIndex.intValue) {
        triggeredLBands.clear()
        imaCuePoints.clear()
    }

    LaunchedEffect(isReelPageActive, isControllerVisible) {
        if (isReelPageActive) {
            onControlsVisibilityChanged(isControllerVisible)
        }
    }

    LaunchedEffect(
        exoPlayer,
        isReelPageActive,
        isEpisodeSelectorExpanded,
        isSettingsClick,
        isShareSheetClick
    ) {
        if (isReelPageActive && !isEpisodeSelectorExpanded && !isSettingsClick && !isShareSheetClick) {
            exoPlayer?.playWhenReady = true
            exoPlayer?.play()
        } else {
            exoPlayer?.playWhenReady = false
            exoPlayer?.pause()
        }
    }

    LaunchedEffect(
        isReelPageActive,
        isControllerVisible,
        isPlayerPlaying,
        isSeekbarDragging,
        isAdsShowing,
        isSettingsClick,
        isShareSheetClick,
        selectedIndex.intValue
    ) {
        if (
            !isReelPageActive ||
            !isControllerVisible ||
            !isPlayerPlaying ||
            isSeekbarDragging ||
            isAdsShowing ||
            isSettingsClick ||
            isShareSheetClick
        ) {
            return@LaunchedEffect
        }

        delay(3000L)

        if (
            exoPlayer?.isPlaying == true &&
            isControllerVisible &&
            !isSeekbarDragging &&
            !isAdsShowing &&
            !isSettingsClick &&
            !isShareSheetClick
        ) {
            isControllerVisible = false
        }
    }

    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            isControllerVisible = false
            isSettingsClick = false
            isShareSheetClick = false
            fastForwardIndicatorSide = null
            isHoldMuteIndicatorVisible = false
            showLShapeAd = false
        }
    }

    LaunchedEffect(exoPlayer, lBandCuePoints, isInPictureInPictureMode) {

        val player = exoPlayer ?: return@LaunchedEffect

        while (true) {

            val currentPos = player.currentPosition

            // 🔥 L-BAND CUE HANDLER
            lBandCuePoints.forEach { cue ->

                if (!triggeredLBands.contains(cue.id)
                    && currentPos >= cue.positionMs
                    && !isAdsShowing
                    && isFullScreen
                    && !isInPictureInPictureMode
                ) {

                    triggeredLBands.add(cue.id)

                    showLShapeAd = true

                    coroutineScope.launch {
                        delay(15_000L)

                        if (!isAdsShowing) {
                            showLShapeAd = false
                        }
                    }
                }
            }

            delay(1000L)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        val reelsCollapsedHeight = maxWidth * 16 / 9

        LShapeAdContainer(
            playerModel = playerModel,
            isFullScreen = isFullScreen,
            isVisible = showLShapeAd && !isAdsShowing && !isInPictureInPictureMode,
            closeButtonEnabled = modelControlsConfig.adClose,
            closeIconRes = playerModel?.customControls?.adCloseIconRes,
            iconTintRes = playerModel?.customControls?.iconTintRes,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val videoSurfaceModifier = Modifier.then(
                    if (isFullScreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(reelsCollapsedHeight)
                    }
                )

                AndroidView(
                    factory = { playerView },
                    update = { view ->
                        if (view.player !== exoPlayer) {
                            view.player = exoPlayer
                        }

                        val targetResizeMode =
                            if (isFullScreen) {
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            } else {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        if (view.resizeMode != targetResizeMode) {
                            view.resizeMode = targetResizeMode
                        }

                        view.subtitleView?.apply {
                            setApplyEmbeddedStyles(false)
                            setApplyEmbeddedFontSizes(false)
                            setStyle(
                                CaptionStyleCompat(
                                    android.graphics.Color.WHITE,
                                    "#80000000".toColorInt(),
                                    android.graphics.Color.TRANSPARENT,
                                    CaptionStyleCompat.EDGE_TYPE_NONE,
                                    android.graphics.Color.TRANSPARENT,
                                    android.graphics.Typeface.DEFAULT
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .then(videoSurfaceModifier)
                        .background(Color.Black)
                        .then(
                            if (!isInPictureInPictureMode) {
                                Modifier.pointerInput(isSettingsClick, isShareSheetClick, exoPlayer) {
                                    var holdMutePreviousVolume: Float? = null
                                    var holdFastForwardPreviousParams: PlaybackParameters? = null
                                    detectTapGestures(
                                        onPress = { offset ->
                                            val holdSide = holdSideForOffset(offset.x, size.width)
                                            try {
                                                tryAwaitRelease()
                                            } finally {
                                                if (holdSide != null) {
                                                    restoreFastForwardHold(
                                                        player = exoPlayer,
                                                        previousParameters = holdFastForwardPreviousParams
                                                    )
                                                    fastForwardIndicatorSide = null
                                                    isHoldMuteIndicatorVisible = false
                                                    holdFastForwardPreviousParams = null
                                                } else {
                                                    restoreHoldMute(
                                                        player = exoPlayer,
                                                        previousVolume = holdMutePreviousVolume,
                                                        playerStateListener = playerStateListener
                                                    )
                                                    isHoldMuteIndicatorVisible = false
                                                    holdMutePreviousVolume = null
                                                }
                                            }
                                        },
                                        onLongPress = { offset ->
                                            val holdSide = holdSideForOffset(offset.x, size.width)
                                            if (holdSide != null) {
                                                holdFastForwardPreviousParams =
                                                    startFastForwardHold(player = exoPlayer)
                                                fastForwardIndicatorSide = holdSide
                                                isHoldMuteIndicatorVisible = false
                                            } else {
                                                holdMutePreviousVolume = muteForHold(
                                                    player = exoPlayer,
                                                    playerStateListener = playerStateListener
                                                )
                                                isHoldMuteIndicatorVisible =
                                                    holdMutePreviousVolume != null
                                            }
                                        },
                                        onTap = {
                                            if (!isSettingsClick && !isShareSheetClick) {
                                                isControllerVisible = !isControllerVisible
                                            }
                                        }
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                )

                if (
                    !isInPictureInPictureMode &&
                    !hasRenderedFirstFrame &&
                    !posterUrl.isNullOrBlank()
                ) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .then(videoSurfaceModifier)
                            .background(Color.Black)
                            .zIndex(1f)
                    )
                }

                if (
                    !isInPictureInPictureMode &&
                    !isControllerVisible &&
                    !isSettingsClick &&
                    !isShareSheetClick
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(exoPlayer) {
                                var holdMutePreviousVolume: Float? = null
                                var holdFastForwardPreviousParams: PlaybackParameters? = null
                                detectTapGestures(
                                    onPress = { offset ->
                                        val holdSide = holdSideForOffset(offset.x, size.width)
                                        try {
                                            tryAwaitRelease()
                                        } finally {
                                            if (holdSide != null) {
                                                restoreFastForwardHold(
                                                    player = exoPlayer,
                                                    previousParameters = holdFastForwardPreviousParams
                                                )
                                                fastForwardIndicatorSide = null
                                                isHoldMuteIndicatorVisible = false
                                                holdFastForwardPreviousParams = null
                                            } else {
                                                restoreHoldMute(
                                                    player = exoPlayer,
                                                    previousVolume = holdMutePreviousVolume,
                                                    playerStateListener = playerStateListener
                                                )
                                                isHoldMuteIndicatorVisible = false
                                                holdMutePreviousVolume = null
                                            }
                                        }
                                    },
                                    onLongPress = { offset ->
                                        val holdSide = holdSideForOffset(offset.x, size.width)
                                        if (holdSide != null) {
                                            holdFastForwardPreviousParams =
                                                startFastForwardHold(player = exoPlayer)
                                            fastForwardIndicatorSide = holdSide
                                            isHoldMuteIndicatorVisible = false
                                        } else {
                                            holdMutePreviousVolume = muteForHold(
                                                player = exoPlayer,
                                                playerStateListener = playerStateListener
                                            )
                                            isHoldMuteIndicatorVisible = holdMutePreviousVolume != null
                                        }
                                    },
                                    onTap = {
                                        isControllerVisible = true
                                    }
                                )
                            }
                    )
                }

                if (!isInPictureInPictureMode && !isControllerVisible && isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }

                if (!isInPictureInPictureMode) {
                    fastForwardIndicatorSide?.let { side ->
                        if (modelControlsConfig.fastForwardIndicator) {
                            FastForwardHoldIndicator(
                                side = side,
                                customControls = playerModel?.customControls,
                                modifier = Modifier.zIndex(5f)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isHoldMuteIndicatorVisible &&
                        !isInPictureInPictureMode &&
                        modelControlsConfig.holdMuteIndicator,
                    modifier = Modifier.zIndex(5f),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    HoldMuteIndicator(customControls = playerModel?.customControls)
                }

                AnimatedVisibility(
                    visible = isControllerVisible &&
                        !isEpisodeSelectorExpanded &&
                        !isSettingsClick &&
                        !isShareSheetClick &&
                        !isInPictureInPictureMode,
                    modifier = Modifier.zIndex(3f),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    exoPlayer?.let { player ->
                        CustomPlayerController(
                            playerModelList = contentList,
                            index = selectedIndex.intValue,
                            totalDuration = contentDuration,
                            isFullScreen = { full ->
                                onFullScreenModeChanged(full)
                                setFullScreen(full)
                                playerStateListener?.onFullScreenChanged(full)
                            },
                            isCurrentlyFullScreen = isFullScreen,
                            exoPlayer = player,
                            modifier = Modifier.fillMaxSize(),
                            isControlsVisible = isControllerVisible,
                            onShowControls = { isControllerVisible = it },
                            isLoading = isLoading,
                            playerStateListener = playerStateListener,
                            onBackPressed = { onPlayerBack(true) },
                            cuePoints = imaCuePoints,
                            controlsConfig = modelControlsConfig,
                            onSeekbarDraggingChanged = { dragging ->
                                isSeekbarDragging = dragging
                            },
                            onShareClick = {
                                player.pause()
                                isShareSheetClick = true
                            },
                            onSettingsClick = {
                                player.pause()
                                isSettingsClick = true
                            },
                            liftBottomControls = showEpisodeSelector
                        )
                    }
                }

                if (!isInPictureInPictureMode) {
                    ReelsShareSheet(
                        playerModel = playerModel,
                        controlsConfig = modelControlsConfig,
                        visible = isShareSheetClick,
                        onDismiss = {
                            isShareSheetClick = false
                            isControllerVisible = true
                            exoPlayer?.playWhenReady = true
                            exoPlayer?.play()
                        }
                    )
                }

                if (!isInPictureInPictureMode) {
                    ReelsSettingsSheet(
                        playerModel = playerModel,
                        exoPlayer = exoPlayer,
                        selectedItemsState = reelsSettingsSelectedItems,
                        selectedOptionId = reelsSettingsSelectedOption,
                        controlsConfig = modelControlsConfig,
                        visible = isSettingsClick,
                        onSelectedOptionChange = { optionId ->
                            reelsSettingsSelectedOption = optionId
                        },
                        onDismiss = {
                            isSettingsClick = false
                            isControllerVisible = true
                            exoPlayer?.playWhenReady = true
                            exoPlayer?.play()
                        }
                    )
                }
            }
        }
    }
}

private fun muteForHold(
    player: Player?,
    playerStateListener: ReelsPlayerStateListener?
): Float? {
    player ?: return null

    val previousVolume = player.volume
    player.volume = 0f
    playerStateListener?.onMuteStateChanged(true)

    return previousVolume
}

private fun restoreHoldMute(
    player: Player?,
    previousVolume: Float?,
    playerStateListener: ReelsPlayerStateListener?
) {
    if (player == null || previousVolume == null) return

    player.volume = if (previousVolume > 0f) previousVolume else 1f
    playerStateListener?.onMuteStateChanged(false)
}

private fun holdSideForOffset(x: Float, width: Int): HoldSide? {
    if (width <= 0) return null

    val sideWidth = width * 0.33f
    return when {
        x <= sideWidth -> HoldSide.Left
        x >= width - sideWidth -> HoldSide.Right
        else -> null
    }
}

private fun startFastForwardHold(player: Player?): PlaybackParameters? {
    player ?: return null

    val previousParameters = player.playbackParameters
    player.playbackParameters = PlaybackParameters(2f, previousParameters.pitch)

    return previousParameters
}

private fun restoreFastForwardHold(
    player: Player?,
    previousParameters: PlaybackParameters?
) {
    if (player == null || previousParameters == null) return

    player.playbackParameters = previousParameters
}

@Composable
private fun HoldMuteIndicator(
    customControls: ReelsPlayerCustomControls? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-36).dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.28f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                CustomIcon(
                    resId = customControls?.holdMuteIconRes,
                    defaultIcon = Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Muted",
                    tint = customControls?.iconTintRes,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun FastForwardHoldIndicator(
    side: HoldSide,
    customControls: ReelsPlayerCustomControls? = null,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "fastForwardHold")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fastForwardHoldScale"
    )

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalArrangement = if (side == HoldSide.Left) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomIcon(
                resId = customControls?.fastForwardIconRes,
                defaultIcon = Icons.Default.FastForward,
                contentDescription = "Fast forward",
                tint = customControls?.iconTintRes,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "2x",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private enum class HoldSide {
    Left,
    Right
}

@Composable
private fun MainContainerPreviewSurface(
    playerModel: ReelsPlayerModel?,
    isReelPageActive: Boolean
) {
    val title = playerModel?.episodeTitle
        ?: playerModel?.title
        ?: "MainContainer preview"
    val subtitle = if (isReelPageActive) "Reels active page" else "Reels prefetched page"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF171717))
        )

        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        Text(
            text = "Static Compose preview",
            color = Color.White.copy(alpha = 0.56f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}

@Preview(name = "MainContainer Reels", widthDp = 360, heightDp = 720, showBackground = true)
@Composable
private fun MainContainerReelsPreview() {
    var isFullScreen by remember { mutableStateOf(true) }

    MainContainer(
        contentList = mainContainerPreviewItems(),
        index = 0,
        playerStateListener = null,
        onPlayerBack = {},
        setFullScreen = {},
        startInFullScreen = true,
        isFullScreen = isFullScreen,
        onFullScreenModeChanged = { isFullScreen = it },
        isReelPageActive = true
    )
}

private fun mainContainerPreviewItems(): List<ReelsPlayerModel> =
    listOf(
        ReelsPlayerModel(
            title = "Preview Reel",
            episodeTitle = "Morning Flow",
            hlsUrl = "https://example.com/reel.m3u8"
        )
    )
