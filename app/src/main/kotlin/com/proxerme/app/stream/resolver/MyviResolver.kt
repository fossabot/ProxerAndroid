package com.proxerme.app.stream.resolver

import com.proxerme.app.application.MainApplication
import com.proxerme.app.stream.StreamResolver
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.util.androidUri
import okhttp3.HttpUrl
import okhttp3.Request


/**
 * Resolver for myvi.ru. It is currently not added, as the StreamActivity can not handle the result
 * properly.
 *
 * @author Ruben Gees
 */
class MyviResolver : StreamResolver() {

    override val name = "Myvi"

    override fun resolve(url: String): StreamResolutionResult {
        val response = MainApplication.httpClient.newCall(Request.Builder()
                .get()
                .url("http://myvi.ru/player/api/Video/Get/${HttpUrl.parse(url).pathSegments().last()}?sig")
                .build()).execute()

        val resultUrl = MainApplication.moshi.adapter(SprutoResult::class.java)
                .fromJson(validateAndGetResult(response)).url

        return StreamResolutionResult(resultUrl.androidUri(), "video/mp4")
    }

    private class SprutoResult(private val sprutoData: SprutoData) {
        val url: HttpUrl
            get() = HttpUrl.parse(sprutoData.playlist.first().video.first().url)
    }

    private class SprutoData(val playlist: Array<SprutoPlaylistItem>)
    private class SprutoPlaylistItem(val video: Array<SprutoVideo>)
    private class SprutoVideo(val url: String)
}