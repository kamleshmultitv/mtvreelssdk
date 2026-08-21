package com.app.reelssdk.ui.reels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.app.reelssdk.listener.ReelsPlayerStateListener
import com.app.reelssdk.model.ReelsEpisodeSelectorConfig
import com.app.reelssdk.model.ReelsEpisodeSelectorItem
import com.app.reelssdk.model.ReelsEpisodeSelectorListener
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.MainContainer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsContainer(
    contentList: List<ReelsPlayerModel>?,
    initialIndex: Int? = 0,
    playerStateListener: ReelsPlayerStateListener?,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit,
    startInFullScreen: Boolean = false,
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig(),
    episodeSelectorConfig: ReelsEpisodeSelectorConfig = ReelsEpisodeSelectorConfig(),
    episodeSelectorListener: ReelsEpisodeSelectorListener? = null
) {
    val pageCount = contentList?.size ?: 0
    var isFullScreen by remember { mutableStateOf(startInFullScreen) }
    val coroutineScope = rememberCoroutineScope()
    val showEpisodeSelector =
        episodeSelectorConfig.enabled && episodeSelectorConfig.episodes.isNotEmpty()
    var isEpisodeSelectorExpanded by remember { mutableStateOf(false) }
    val internalEpisodeSelectorListener = object : ReelsEpisodeSelectorListener {
        override fun onEpisodeSelected(index: Int, item: ReelsEpisodeSelectorItem) {
            isEpisodeSelectorExpanded = false
            episodeSelectorListener?.onEpisodeSelected(index, item)
        }

        override fun onLockedEpisodeClick(index: Int, item: ReelsEpisodeSelectorItem?) {
            episodeSelectorListener?.onLockedEpisodeClick(index, item)
        }

        override fun onWatchNowClick(index: Int, item: ReelsEpisodeSelectorItem?) {
            episodeSelectorListener?.onWatchNowClick(index, item)
        }

        override fun onEpisodeSelectorExpandedChanged(isExpanded: Boolean) {
            isEpisodeSelectorExpanded = isExpanded
            episodeSelectorListener?.onEpisodeSelectorExpandedChanged(isExpanded)
        }
    }
    val initialPage = when {
        pageCount == 0 -> 0
        initialIndex == null -> 0
        initialIndex < 0 -> 0
        initialIndex >= pageCount -> pageCount - 1
        else -> initialIndex
    }

    val pagerState = rememberPagerState(initialPage = initialPage) {
        pageCount
    }

    LaunchedEffect(initialPage, pageCount) {
        if (pageCount > 0 && pagerState.currentPage != initialPage) {
            pagerState.scrollToPage(initialPage)
        }
    }

    LaunchedEffect(startInFullScreen) {
        isFullScreen = startInFullScreen
    }

    LaunchedEffect(showEpisodeSelector) {
        if (!showEpisodeSelector) {
            isEpisodeSelectorExpanded = false
        }
    }

    LaunchedEffect(pagerState, pageCount) {
        if (pageCount == 0) return@LaunchedEffect

        snapshotFlow {
            pagerState.settledPage.coerceIn(0, pageCount - 1)
        }.distinctUntilChanged().collect { settledPage ->
            playerStateListener?.onReelChanged(settledPage)
        }
    }

    LaunchedEffect(pagerState, pageCount) {
        if (pageCount == 0) return@LaunchedEffect

        snapshotFlow {
            val currentPage = pagerState.currentPage.coerceIn(0, pageCount - 1)
            val targetPage = pagerState.targetPage.coerceIn(0, pageCount - 1)
            when {
                pagerState.isScrollInProgress && targetPage != currentPage -> targetPage
                currentPage + 1 < pageCount -> currentPage + 1
                currentPage - 1 >= 0 -> currentPage - 1
                else -> null
            }
        }.distinctUntilChanged().collect { preloadIndex ->
            preloadIndex?.let { playerStateListener?.onPreloadNext(it) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = REELS_PREFETCH_PAGE_COUNT,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(REELS_MAX_PAGES_PER_FLING)
            ),
            key = { page ->
                contentList?.getOrNull(page)?.id
                    ?: contentList?.getOrNull(page)?.videoUrl
                    ?: contentList?.getOrNull(page)?.hlsUrl
                    ?: page
            }
        ) { page ->
            MainContainer(
                contentList = contentList,
                index = page,
                playerStateListener = playerStateListener,
                onPlayerBack = onPlayerBack,
                setFullScreen = setFullScreen,
                startInFullScreen = startInFullScreen,
                isFullScreen = isFullScreen,
                onFullScreenModeChanged = { full ->
                    isFullScreen = full
                },
                isReelPageActive = page == pagerState.settledPage,
                controlsConfig = controlsConfig,
                showEpisodeSelector = showEpisodeSelector,
                isEpisodeSelectorExpanded = isEpisodeSelectorExpanded,
                onPlaybackCompleted = {
                    val nextPage = page + 1
                    if (
                        page == pagerState.settledPage &&
                        nextPage < pageCount &&
                        !pagerState.isScrollInProgress
                    ) {
                        isEpisodeSelectorExpanded = false
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(nextPage)
                        }
                    }
                }
            )
        }

        ReelsEpisodeSelector(
            config = episodeSelectorConfig,
            listener = internalEpisodeSelectorListener,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(4f)
        )
    }
}

private const val REELS_PREFETCH_PAGE_COUNT = 1
private const val REELS_MAX_PAGES_PER_FLING = 1
