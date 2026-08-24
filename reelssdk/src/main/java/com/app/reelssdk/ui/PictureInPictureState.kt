package com.app.reelssdk.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer

@Composable
internal fun rememberPictureInPictureMode(overrideValue: Boolean? = null): Boolean {
    if (overrideValue != null) return overrideValue

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var isInPictureInPicture by remember(activity) {
        mutableStateOf(activity?.isInPictureInPictureModeCompat() == true)
    }

    DisposableEffect(activity) {
        isInPictureInPicture = activity?.isInPictureInPictureModeCompat() == true

        val componentActivity = activity as? ComponentActivity
            ?: return@DisposableEffect onDispose {}
        val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
            isInPictureInPicture = info.isInPictureInPictureMode
        }

        componentActivity.addOnPictureInPictureModeChangedListener(listener)
        onDispose {
            componentActivity.removeOnPictureInPictureModeChangedListener(listener)
        }
    }

    return isInPictureInPicture
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Activity.isInPictureInPictureModeCompat(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode
