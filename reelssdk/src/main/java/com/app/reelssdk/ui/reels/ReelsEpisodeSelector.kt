package com.app.reelssdk.ui.reels

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.reelssdk.model.ReelsEpisodeSelectorConfig
import com.app.reelssdk.model.ReelsEpisodeSelectorItem
import com.app.reelssdk.model.ReelsEpisodeSelectorListener

@Composable
fun ReelsEpisodeSelector(
    config: ReelsEpisodeSelectorConfig,
    modifier: Modifier = Modifier,
    listener: ReelsEpisodeSelectorListener? = null
) {
    if (!config.enabled || config.episodes.isEmpty()) return

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
    val selectorBackgroundColor =
        if (expanded) Color(0xFF111111) else Color(0x99111111)
    val selectorBorderColor =
        if (expanded) Color.Transparent else Color.White.copy(alpha = 0.18f)

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
                color = selectorBackgroundColor,
                shape = selectorShape
            )
            .border(
                width = 1.dp,
                color = selectorBorderColor,
                shape = selectorShape
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clickable {
                    expanded = !expanded
                    listener?.onEpisodeSelectorExpandedChanged(expanded)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Episode $currentEpisodeNumber/$safeTotal",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (expanded) "Close" else "Episodes",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$safeTotal Episodes",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.22f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                LazyRow(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(groups, key = { it.group }) { range ->
                        val selected = range.group == selectedGroup

                        Text(
                            text = range.label,
                            color = if (selected) Color(0xFFFFC94A) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) {
                                        Color(0xFFFFC94A)
                                    } else {
                                        Color.White.copy(alpha = 0.25f)
                                    },
                                    shape = CircleShape
                                )
                                .clickable { selectedGroup = range.group }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

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

        if (item?.isLocked == true) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color.White,
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
