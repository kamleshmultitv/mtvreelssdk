package com.app.reelssdk.ui.reels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.reelssdk.model.ReelsEpisodeSelectorConfig
import com.app.reelssdk.model.ReelsEpisodeSelectorItem
import com.app.reelssdk.model.ReelsEpisodeSelectorListener
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerCustomControls
import com.app.reelssdk.ui.CustomIcon

@Composable
fun ReelsEpisodeSelector(
    config: ReelsEpisodeSelectorConfig,
    modifier: Modifier = Modifier,
    customControls: ReelsPlayerCustomControls? = null,
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig(),
    listener: ReelsEpisodeSelectorListener? = null
) {
    if (!controlsConfig.episodeSelector || !config.enabled || config.episodes.isEmpty()) return

    val safeTotal = maxOf(config.totalCount, config.episodes.size)
    if (safeTotal <= 0) return

    val safeGroupSize = config.groupSize.coerceAtLeast(1)
    val safeColumns = config.columns.coerceAtLeast(1)
    val safeIndex = config.currentIndex.coerceIn(0, safeTotal - 1)
    val currentItem = config.episodes.getOrNull(safeIndex)
    val currentEpisodeNumber = currentItem?.episodeNumber ?: (safeIndex + 1)
    val groupCount = (safeTotal + safeGroupSize - 1) / safeGroupSize
    val groups = List(groupCount) { group ->
        val start = group * safeGroupSize + 1
        val end = minOf(safeTotal, (group + 1) * safeGroupSize)
        EpisodeRange(group = group, label = "$start - $end")
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedGroup by remember(safeIndex, safeGroupSize) {
        mutableIntStateOf(safeIndex / safeGroupSize)
    }
    val selectorShape = RoundedCornerShape(24.dp)
    val selectorBackgroundBrush = if (expanded) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xEE111111),
                Color(0xEE111111)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.08f),
                Color.Black.copy(alpha = 0.34f)
            )
        )
    }
    val selectorBorderColor = if (expanded) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.26f)
    }
    val expandedChipHeight = 32.dp
    val expandedChipShape = RoundedCornerShape(16.dp)
    val trailingIconRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "episodeSelectorTrailingIconRotation"
    )

    LaunchedEffect(safeIndex) {
        if (expanded) {
            expanded = false
            listener?.onEpisodeSelectorExpandedChanged(false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .background(
                brush = selectorBackgroundBrush,
                shape = selectorShape
            )
            .border(
                width = 1.dp,
                color = selectorBorderColor,
                shape = selectorShape
            )
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clickable {
                    expanded = !expanded
                    listener?.onEpisodeSelectorExpandedChanged(expanded)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (controlsConfig.episodeSelectorLeadingIcon) {
                CustomIcon(
                    resId = customControls?.episodeSelectorIconRes,
                    defaultIcon = Icons.Default.MovieCreation,
                    contentDescription = null,
                    tint = customControls?.iconTintRes,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))
            }

            Text(
                text = "Episode $currentEpisodeNumber of $safeTotal",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            if (controlsConfig.episodeSelectorTrailingIcon) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            rotationZ = trailingIconRotation
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = expanded,
                        animationSpec = tween(durationMillis = 180),
                        label = "episodeSelectorTrailingCustomIcon"
                    ) { isExpanded ->
                        CustomIcon(
                            resId = if (isExpanded) {
                                customControls?.episodeSelectorCollapseIconRes
                            } else {
                                customControls?.episodeSelectorExpandIconRes
                            },
                            defaultIcon = if (isExpanded) {
                                Icons.Default.Close
                            } else {
                                Icons.Default.ChevronRight
                            },
                            contentDescription = null,
                            tint = customControls?.iconTintRes,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(expandedChipHeight)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.22f),
                                shape = expandedChipShape
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$safeTotal Episodes",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LazyRow(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groups, key = { it.group }) { range ->
                            val selected = range.group == selectedGroup

                            Box(
                                modifier = Modifier
                                    .height(expandedChipHeight)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) {
                                            Color(0xFFFFC94A)
                                        } else {
                                            Color.White.copy(alpha = 0.25f)
                                        },
                                        shape = expandedChipShape
                                    )
                                    .clickable { selectedGroup = range.group }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = range.label,
                                    color = if (selected) Color(0xFFFFC94A) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val startIndex = (selectedGroup * safeGroupSize).coerceIn(0, safeTotal - 1)
                val endIndexExclusive = minOf(safeTotal, startIndex + safeGroupSize)
                val visibleIndexes = remember(startIndex, endIndexExclusive) {
                    (startIndex until endIndexExclusive).toList()
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(safeColumns),
                    modifier = Modifier.heightIn(max = 200.dp),
                    userScrollEnabled = visibleIndexes.size > safeColumns
                ) {
                    gridItems(
                        items = visibleIndexes,
                        key = { index: Int ->
                            config.episodes.getOrNull(index)?.id ?: "episode_$index"
                        }
                    ) { index: Int ->
                        val item = config.episodes.getOrNull(index)

                        EpisodeTile(
                            index = index,
                            item = item,
                            isCurrent = index == safeIndex,
                            customControls = customControls,
                            controlsConfig = controlsConfig,
                            onClick = {
                                if (item?.isLocked == true) {
                                    listener?.onLockedEpisodeClick(index, item)
                                } else if (item != null) {
                                    expanded = false
                                    listener?.onEpisodeSelectorExpandedChanged(false)
                                    listener?.onEpisodeSelected(index, item)
                                }
                            }
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun EpisodeTile(
    index: Int,
    item: ReelsEpisodeSelectorItem?,
    isCurrent: Boolean,
    customControls: ReelsPlayerCustomControls?,
    controlsConfig: ReelsPlayerControlsConfig,
    onClick: () -> Unit
) {
    val progress = item?.progressFraction?.coerceIn(0f, 1f) ?: 0f

    Box(
        modifier = Modifier
            .padding(4.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCurrent) Color(0xFFE53935) else Color(0xFF2A2A2A))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item?.episodeNumber?.toString() ?: (index + 1).toString(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )

        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Color(0xFFFFC94A))
                )
            }
        }

        if (item?.isLocked == true && controlsConfig.lockedEpisodeIcon) {
            CustomIcon(
                resId = customControls?.lockedEpisodeIconRes,
                defaultIcon = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = customControls?.iconTintRes,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(12.dp)
            )
        }
    }
}

private data class EpisodeRange(
    val group: Int,
    val label: String
)
