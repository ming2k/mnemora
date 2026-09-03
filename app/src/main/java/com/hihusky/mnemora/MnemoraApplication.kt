package com.hihusky.mnemora

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.hihusky.mnemora.initialization.DebugHooks
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MnemoraApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        DebugHooks.seedIfNeeded(this)
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context).build()
}
