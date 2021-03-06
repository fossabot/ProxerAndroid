package me.proxer.app.chat.prv.conference.info

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.commitNow
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.util.extension.startActivity

/**
 * @author Ruben Gees
 */
class ConferenceInfoActivity : DrawerActivity() {

    companion object {
        private const val CONFERENCE_EXTRA = "conference"

        fun navigateTo(context: Activity, conference: LocalConference) {
            context.startActivity<ConferenceInfoActivity>(CONFERENCE_EXTRA to conference)
        }
    }

    val conference: LocalConference
        get() = intent.getParcelableExtra(CONFERENCE_EXTRA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, ConferenceInfoFragment.newInstance())
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = conference.topic
    }
}
