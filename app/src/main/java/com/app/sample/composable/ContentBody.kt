package com.app.sample.composable

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.app.mtvdownloader.entity.DownloadEntity
import com.app.reelssdk.listener.ReelsPlayerStateListener
import com.app.reelssdk.model.ReelsEpisodeSelectorConfig
import com.app.reelssdk.model.ReelsEpisodeSelectorItem
import com.app.reelssdk.model.ReelsEpisodeSelectorListener
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.MtvReelsPlayerSdk
import com.app.sample.R
import com.app.sample.composable.download.DownloadPlayer
import com.app.sample.composable.download.DownloadedContentList
import com.app.sample.model.ContentItem
import com.app.sample.model.DeepLinkResponse
import com.app.sample.model.OverrideContent
import com.app.sample.utils.FileUtils.buildPlayerContentList

@Composable
fun ContentBody(
    context: Context,
    contentItem: ContentItem? = null,
    pagingItems: LazyPagingItems<ContentItem>,
    selectedIndex: MutableIntState,
    overrideContent: OverrideContent?,
    deepLinkContent: DeepLinkResponse?,
    isDeepLink: Boolean? = false,
    isFullScreen: Boolean,
    isInPictureInPictureMode: Boolean = false,
    onFullScreenChange: (Boolean) -> Unit,
    onOverrideContent: (OverrideContent?) -> Unit
) {
    // 🔥 IMPORTANT: no derivedStateOf here
    val contentList = remember(
        pagingItems.itemSnapshotList.items,
        overrideContent,
        deepLinkContent,
        contentItem
    ) {
        buildPlayerContentList(
            context = context,
            pagingItems = pagingItems,
            overrideContent = overrideContent,
            deepLinkContent = deepLinkContent,
            contentItem = contentItem
        )
    }

    val downloadedContentList = remember {
        mutableStateListOf<DownloadEntity>()
    }

    var showDownloadedList by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<DownloadEntity?>(null) }
    val episodeSelectorItems = remember(contentList) {
        contentList.mapIndexed { index, item ->
            ReelsEpisodeSelectorItem(
                id = item.id?.takeIf { it.isNotBlank() } ?: index.toString(),
                title = item.episodeTitle?.takeIf { it.isNotBlank() }
                    ?: item.title.orEmpty(),
                episodeNumber = item.episodeNumber?.toIntOrNull() ?: (index + 1),
                seasonId = item.seasonNumber,
                isLocked = false,
                ageRating = item.ageRating ?: item.contentRating
            )
        }
    }
    val episodeSelectorConfig = ReelsEpisodeSelectorConfig(
        enabled = episodeSelectorItems.isNotEmpty(),
        episodes = episodeSelectorItems,
        currentIndex = selectedIndex.intValue,
        totalCount = episodeSelectorItems.size
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 🎬 SDK Video Player (KEYED)
            key(contentList) {
                MtvReelsPlayerSdk(
                    contentList = contentList,
                    index = selectedIndex.intValue,

                    // ✅ Back handling (no logic change)
                    onPlayerBack = {
                        if (isFullScreen) {
                            onFullScreenChange(false)
                        }
                    },

                    // ✅ Fullscreen toggle (same as before)
                    setFullScreen = { isFull ->
                        onFullScreenChange(isFull)
                    },
                    episodeSelectorConfig = episodeSelectorConfig,
                    isInPictureInPictureMode = isInPictureInPictureMode,
                    episodeSelectorListener = object : ReelsEpisodeSelectorListener {
                        override fun onEpisodeSelected(
                            index: Int,
                            item: ReelsEpisodeSelectorItem
                        ) {
                            selectedIndex.intValue = index
                            onOverrideContent(null)
                        }

                        override fun onLockedEpisodeClick(
                            index: Int,
                            item: ReelsEpisodeSelectorItem?
                        ) {
                            Log.d("CLIENT", "Locked episode click: index=$index, id=${item?.id}")
                        }

                        override fun onWatchNowClick(
                            index: Int,
                            item: ReelsEpisodeSelectorItem?
                        ) {
                            if (item?.isLocked == true) {
                                onLockedEpisodeClick(index, item)
                            } else {
                                selectedIndex.intValue = index
                                onOverrideContent(null)
                            }
                        }

                        override fun onEpisodeSelectorExpandedChanged(isExpanded: Boolean) {
                            Log.d("CLIENT", "Episode selector expanded: $isExpanded")
                        }
                    },

                    playerStateListener = object : ReelsPlayerStateListener {

                        override fun onPlayerReady(durationMs: Long) {
                            Log.d("CLIENT", "Player ready: $durationMs")
                        }

                        override fun onPlayStateChanged(isPlaying: Boolean) {
                            Log.d("CLIENT", "Playing: $isPlaying")
                        }

                        override fun onPlaybackCompleted() {
                            Log.d("CLIENT", "Playback completed")
                        }

                        override fun onReelChanged(position: Int) {
                            selectedIndex.intValue = position
                        }

                        override fun onPreloadNext(index: Int) {
                            if (index in 0 until pagingItems.itemCount) {
                                val preloadItem = pagingItems[index]
                                Log.d(
                                    "CLIENT",
                                    "Preload requested: index=$index, loaded=${preloadItem != null}"
                                )
                            }
                        }

                        override fun onFullScreenChanged(isFullScreen: Boolean) {
                            Log.d("CLIENT", "Full screen: $isFullScreen")
                        }

                        override fun onAdStateChanged(isAdPlaying: Boolean) {
                            Log.d("CLIENT", "Ad playing = $isAdPlaying")
                        }

                        override fun onWatchlistClick(
                            index: Int,
                            item: ReelsPlayerModel?,
                            isWatchlisted: Boolean
                        ) {
                            Log.d(
                                "CLIENT",
                                "Watchlist click: index=$index, id=${item?.id}, isWatchlisted=$isWatchlisted"
                            )
                        }
                    }
                )
            }

            // 📜 Content List
            if (!isInPictureInPictureMode) {
                ContentList(
                    pagingItems = pagingItems,
                    onItemClick = { index ->
                        selectedIndex.intValue = index
                        onOverrideContent(null)
                    },
                    downloadContentList = { list ->
                        downloadedContentList.clear()
                        downloadedContentList.addAll(list)
                    }
                )
            }
        }

        // ➕ Floating Action Button

        if (!isInPictureInPictureMode && !isFullScreen && downloadedContentList.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    showDownloadedList = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 160.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = colorResource(R.color.white),
                shape = RoundedCornerShape(8.dp)
            ) {
                // ✅ FAB content (ICON / TEXT REQUIRED)
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Downloads"
                )
            }
        }

        if (!isInPictureInPictureMode && showDownloadedList) {
            DownloadedContentList(downloadContentList = downloadedContentList,
                onItemClick = { item ->
                    selectedItem = item
                },
                onBackClick = {
                    showDownloadedList = false
                })
        }

        selectedItem?.let { item ->
            DownloadPlayer(item,
                onBack = {
                    selectedItem = null
                })
        }
    }
}
