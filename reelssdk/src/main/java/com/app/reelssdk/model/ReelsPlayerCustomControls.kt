package com.app.reelssdk.model

import androidx.annotation.ColorRes

data class ReelsPlayerCustomControls(

    // Global tint applied to all SDK fallback/custom icons when provided.
    @param:ColorRes
    @field:ColorRes
    val iconTintRes: Int? = null,

    val playIconRes: Int? = null,
    val pauseIconRes: Int? = null,

    val backIconRes: Int? = null,
    val settingsIconRes: Int? = null,
    val settingsCloseIconRes: Int? = null,
    val fullScreenIconRes: Int? = null,
    val exitFullScreenIconRes: Int? = null,
    val crossFadeIconRes: Int? = null,

    val shareIconRes: Int? = null,
    val shareMoreIconRes: Int? = null,
    val watchlistIconRes: Int? = null,
    val watchlistedIconRes: Int? = null,

    val episodeSelectorIconRes: Int? = null,
    val episodeSelectorExpandIconRes: Int? = null,
    val episodeSelectorCollapseIconRes: Int? = null,
    val lockedEpisodeIconRes: Int? = null,

    val holdMuteIconRes: Int? = null,
    val fastForwardIconRes: Int? = null,
    val adCloseIconRes: Int? = null,

)
