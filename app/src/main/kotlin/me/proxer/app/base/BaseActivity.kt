package me.proxer.app.base

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.openHttpPage
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
abstract class BaseActivity : AppCompatActivity(), LifecycleRegistryOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val customTabsHelper by lazy { CustomTabsHelperFragment.attachTo(this) }

    fun setLikelyUrl(url: HttpUrl) = customTabsHelper.mayLaunchUrl(url.androidUri(), Bundle(), emptyList())
    fun showPage(url: HttpUrl) = customTabsHelper.openHttpPage(this, url)

    override fun getLifecycle() = lifecycleRegistry
}
