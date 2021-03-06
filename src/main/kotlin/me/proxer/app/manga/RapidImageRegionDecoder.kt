package me.proxer.app.manga

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
import rapid.decoder.BitmapDecoder

/**
 * @author Ruben Gees
 */
class RapidImageRegionDecoder : ImageRegionDecoder {

    private var decoder: BitmapDecoder? = null

    override fun init(context: Context, uri: Uri): Point {
        decoder = BitmapDecoder.from(context, uri)
            .config(Bitmap.Config.RGB_565)

        return Point(decoder?.sourceWidth() ?: -1, decoder?.sourceHeight() ?: -1)
    }

    @Synchronized
    override fun decodeRegion(sRect: Rect, sampleSize: Int): Bitmap {
        return decoder?.reset()
            ?.region(sRect)
            ?.scale(sRect.width() / sampleSize, sRect.height() / sampleSize)
            ?.useBuiltInDecoder()
            ?.decode()
            ?: throw IllegalStateException("decoded bitmap is null")
    }

    override fun isReady() = decoder != null

    override fun recycle() {
        BitmapDecoder.destroyMemoryCache()
        BitmapDecoder.destroyDiskCache()

        decoder?.reset()
        decoder = null
    }
}
