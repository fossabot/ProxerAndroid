@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.util.Utils
import me.proxer.app.web.WebViewActivity
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import org.jetbrains.anko.dip
import java.util.concurrent.Semaphore

inline fun <T> Semaphore.lock(action: () -> T): T {
    acquire()

    return try {
        action()
    } finally {
        release()
    }
}

inline fun Context.getDrawableFromAttrs(resource: Int): Drawable {
    val styledAttributes = obtainStyledAttributes(intArrayOf(resource))
    val result = styledAttributes.getDrawable(0)

    styledAttributes.recycle()

    return result
}

inline fun Context.getQuantityString(id: Int, quantity: Int): String = resources
        .getQuantityString(id, quantity, quantity)

fun View.toastBelow(message: Int) = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED).let { view.measure(it, it) }

    val windowVisibleDisplayFrame = Rect().apply {
        (context as Activity).window.decorView.getWindowVisibleDisplayFrame(this)
    }

    val viewLocation = IntArray(2).apply { getLocationInWindow(this) }
    val viewLeft = viewLocation[0] - windowVisibleDisplayFrame.left
    val viewTop = viewLocation[1] - windowVisibleDisplayFrame.top
    val toastX = viewLeft + (width - view.measuredWidth) / 2
    val toastY = viewTop + height + dip(4)

    setGravity(Gravity.START or Gravity.TOP, toastX, toastY)
}.show()

fun CustomTabsHelperFragment.openHttpPage(activity: Activity, url: HttpUrl) {
    when (Utils.getNativeAppPackage(activity, url).isEmpty()) {
        true -> {
            CustomTabsIntent.Builder(session)
                    .setToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimary))
                    .setSecondaryToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark))
                    .addDefaultShareMenuItem()
                    .enableUrlBarHiding()
                    .setShowTitle(true)
                    .build()
                    .let {
                        CustomTabsHelperFragment.open(activity, it, url.androidUri(), { context, uri ->
                            WebViewActivity.navigateTo(context, uri.toString())
                        })
                    }
        }
        false -> activity.startActivity(Intent(Intent.ACTION_VIEW).setData(url.androidUri()))
    }
}

inline fun GlideRequests.defaultLoad(view: ImageView, url: HttpUrl): Target<Drawable> = load(url.toString())
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(view)

inline fun HttpUrl.androidUri(): Uri = Uri.parse(toString())
