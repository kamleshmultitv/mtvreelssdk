# MTV Reels SDK

Standalone vertical reels player for Android apps. This module is intentionally separate from the mobile player SDK so host apps can install both artifacts in one APK without duplicate classes.

## Dependency

```kotlin
dependencies {
    implementation("com.github.kamleshmultitv:mtvreelssdk:reels-1.0.7")
}
```

## Sample App

This repository includes a sample app in `:app`.

For local SDK development, the sample app uses the source module:

```kotlin
implementation(project(":reelssdk"))
```

The sample app also keeps the existing mobile SDK dependency so both SDKs are tested in the same APK:

```kotlin
implementation("com.github.kamleshmultitv:mtvplayersdk:mobile-2.0.36")
implementation(project(":reelssdk"))
```

To test the published JitPack artifact instead of local source, switch the app dependency to:

```kotlin
implementation("com.github.kamleshmultitv:mtvreelssdk:reels-1.0.7")
```

## Public API

```kotlin
import com.app.reelssdk.listener.ReelsPlayerStateListener
import com.app.reelssdk.model.ReelsPlayerConfig
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerCustomControls
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.MtvReelsPlayerSdk

MtvReelsPlayerSdk(
    contentList = reels,
    index = 0,
    playerConfig = ReelsPlayerConfig(
        controls = ReelsPlayerControlsConfig(
            play = true,
            pause = true,
            seekbar = true,
            settings = true,
            share = true,
            bookmark = true,
            fullscreen = true,
            exitFullscreen = true
        )
    ),
    playerStateListener = object : ReelsPlayerStateListener {
        override fun onReelChanged(position: Int) {}
        override fun onPreloadNext(index: Int) {}
    },
    onPlayerBack = {},
    setFullScreen = {}
)
```

## Publishing

The module publishes:

- `groupId`: `com.github.kamleshmultitv`
- `artifactId`: `mtvreelssdk`
- `version`: `reels-1.0.7`

JitPack should run:

```bash
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :reelssdk:publishToMavenLocal
```

## Package Isolation

All reels SDK Kotlin classes use `com.app.reelssdk.*`. The old mobile SDK keeps `com.app.videosdk.*`, so a host app can depend on both:

```kotlin
implementation("com.github.kamleshmultitv:mtvplayersdk:mobile-2.0.36")
implementation("com.github.kamleshmultitv:mtvreelssdk:reels-1.0.7")
```
