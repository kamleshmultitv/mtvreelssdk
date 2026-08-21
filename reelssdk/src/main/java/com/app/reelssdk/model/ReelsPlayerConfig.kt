package com.app.reelssdk.model

data class ReelsPlayerConfig(
    val controls: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig()
)

data class ReelsPlayerControlsConfig(
    val play: Boolean = true,
    val pause: Boolean = true,
    val settings: Boolean = true,
    val fullscreen: Boolean = true,
    val exitFullscreen: Boolean = true,
    val share: Boolean = true,
    val bookmark: Boolean = true,
    val seekbar: Boolean = true
)

data class ReelsEpisodeSelectorItem(
    val id: String,
    val title: String = "",
    val episodeNumber: Int? = null,
    val seasonId: String? = null,
    val isLocked: Boolean = false,
    val progressFraction: Float? = null,
    val ageRating: String? = null,
    val genre: String? = null
)

data class ReelsEpisodeSelectorConfig(
    val enabled: Boolean = false,
    val episodes: List<ReelsEpisodeSelectorItem> = emptyList(),
    val currentIndex: Int = 0,
    val totalCount: Int = episodes.size,
    val groupSize: Int = 10,
    val columns: Int = 5,
    val showWatchNowOverlay: Boolean = true,
    val watchNowText: String = "Watch Now",
    val continueWatchingText: String = "Continue Watching"
)

interface ReelsEpisodeSelectorListener {
    fun onEpisodeSelected(index: Int, item: ReelsEpisodeSelectorItem) {}
    fun onLockedEpisodeClick(index: Int, item: ReelsEpisodeSelectorItem?) {}
    fun onWatchNowClick(index: Int, item: ReelsEpisodeSelectorItem?) {}
    fun onEpisodeSelectorExpandedChanged(isExpanded: Boolean) {}
}
