package com.proxerme.app.stream.resolver

import android.net.Uri
import com.proxerme.app.stream.StreamResolver
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult

/**
 * Resolver for the Daisuki hoster. Currently it only redirects to the homepage as the app does not
 * offer support for sending Intents and the mobile website is not usable without the app.
 *
 * @author Ruben Gees
 */
class DaisukiResolver : StreamResolver() {

    override val name = "Daisuki"

    override fun resolve(url: String): StreamResolutionResult {
        return StreamResolutionResult(Uri.parse("http://daisuki.net"), "text/html")
    }
}