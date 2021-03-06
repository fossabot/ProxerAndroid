package me.proxer.app.manga

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder
import rapid.decoder.BitmapDecoder

/**
 * @author Ruben Gees
 */
class RapidImageDecoder : ImageDecoder {

    override fun decode(context: Context, uri: Uri) = BitmapDecoder.from(context, uri)
        .config(Bitmap.Config.RGB_565)
        .useBuiltInDecoder()
        .decode()
        ?: throw IllegalStateException("decoded bitmap is null")
}
