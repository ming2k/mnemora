package com.hihusky.mnemora

import android.app.Application
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.hihusky.mnemora.initialization.DebugHooks
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MnemoraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupImageLoader()
        DebugHooks.seedIfNeeded(this)
    }

    private fun setupImageLoader() {
        val imageLoader = ImageLoader.Builder(this)
            .components {
                // GIF: prefer ImageDecoder on API 28+, fallback to GifDecoder
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .build()
        Coil.setImageLoader(imageLoader)
    }
}
