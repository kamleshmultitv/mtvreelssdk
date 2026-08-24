package com.app.reelssdk.model

data class ReelsPlayerConfig(
    val controls: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig()
)

data class ReelsPlayerControlsConfig(
    val back: Boolean = true,
    val title: Boolean = true,
    val playPause: Boolean = true,
    val showActionLabels: Boolean = true,
    val settings: Boolean = true,
    val settingsClose: Boolean = true,
    val fullscreen: Boolean = true,
    val exitFullscreen: Boolean = true,
    val share: Boolean = true,
    val shareMore: Boolean = true,
    val watchlist: Boolean = true,
    val seekbar: Boolean = true,
    val spritePreview: Boolean = true,
    val episodeSelector: Boolean = true,
    val episodeSelectorLeadingIcon: Boolean = true,
    val episodeSelectorTrailingIcon: Boolean = true,
    val lockedEpisodeIcon: Boolean = true,
    val holdMuteIndicator: Boolean = true,
    val fastForwardIndicator: Boolean = true,
    val adClose: Boolean = true
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
