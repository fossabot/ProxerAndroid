package com.proxerme.app.fragment.anime

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.activity.AnimeActivity
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.anime.StreamAdapter
import com.proxerme.app.application.MainApplication
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.app.view.MediaControlView
import com.proxerme.library.connection.ProxerCall
import com.proxerme.library.connection.anime.entity.Stream
import com.proxerme.library.connection.anime.request.StreamsRequest
import com.proxerme.library.connection.ucp.request.SetReminderRequest
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import org.joda.time.DateTime

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class AnimeFragment : EasyLoadingFragment<Array<Stream>>() {

    companion object {
        private const val ARGUMENT_ID = "id"
        private const val ARGUMENT_EPISODE = "episode"
        private const val ARGUMENT_TOTAL_EPISODES = "total_episodes"
        private const val ARGUMENT_LANGUAGE = "language"

        private const val REMINDER_EPISODE_STATE = "reminder_episode_state"

        fun newInstance(id: String, episode: Int, totalEpisodes: Int, language: String):
                AnimeFragment {
            return AnimeFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                    this.putInt(ARGUMENT_EPISODE, episode)
                    this.putInt(ARGUMENT_TOTAL_EPISODES, totalEpisodes)
                    this.putString(ARGUMENT_LANGUAGE, language)
                }
            }
        }
    }

    override val section = SectionManager.Section.ANIME

    private val id: String
        get() = arguments.getString(ARGUMENT_ID)
    private val totalEpisodes: Int
        get() = arguments.getInt(ARGUMENT_TOTAL_EPISODES)
    private val episode: Int
        get() = arguments.getInt(ARGUMENT_EPISODE)
    private val language: String
        get() = arguments.getString(ARGUMENT_LANGUAGE)

    private lateinit var streamAdapter: StreamAdapter
    private lateinit var adapter: EasyHeaderFooterAdapter
    override var result: Array<Stream>?
        get() {
            return streamAdapter.items.toTypedArray()
        }
        set(value) {
            if (value == null) {
                streamAdapter.clear()
                streamAdapter.clear()

                adapter.removeHeader()
            } else {
                streamAdapter.replace(value)

                adapter.setHeader(header)
            }
        }

    private var reminderEpisode: Int? = null
    private var reminderTask: ProxerCall? = null

    private lateinit var header: MediaControlView

    private val root: ViewGroup by bindView(R.id.root)
    private val streams: RecyclerView by bindView(R.id.streams)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            reminderEpisode = it.getInt(REMINDER_EPISODE_STATE)

            if (reminderEpisode == 0) {
                reminderEpisode = null
            }
        }

        streamAdapter = StreamAdapter(savedInstanceState)
        adapter = EasyHeaderFooterAdapter(streamAdapter)

        synchronize(reminderEpisode)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        header = inflater.inflate(R.layout.item_media_header, container, false) as MediaControlView

        return inflater.inflate(R.layout.fragment_anime, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        streams.layoutManager = LinearLayoutManager(context)
        streams.adapter = adapter

        header.onTranslatorGroupClickListener = {
            result?.firstOrNull()?.subgroupId?.let {
                Utils.viewLink(context, ProxerUrlHolder.getSubgroupUrl(it,
                        ProxerUrlHolder.DEVICE_QUERY_PARAMETER_DEFAULT).toString())
            }
        }

        header.onUploaderClickListener = {
            result?.firstOrNull()?.let {
                UserActivity.navigateTo(activity, it.uploaderId, it.uploader)
            }
        }

        header.onReminderClickListener = {
            if (it != reminderEpisode) {
                synchronize(it)
            }
        }

        header.onSwitchClickListener = {
            switchEpisode(it)
        }

        if (savedInstanceState != null && !streamAdapter.isEmpty()) {
            adapter.setHeader(header)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(REMINDER_EPISODE_STATE, reminderEpisode ?: 0)
        streamAdapter.saveInstanceState(outState)
    }

    override fun showContent(result: Array<Stream>) {
        if (result.isNotEmpty()) {
            header.setUploader(result.first().uploader)
            header.setTranslatorGroup(result.first().subgroup ?:
                    context.getString(R.string.fragment_anime_empty_subgoup))
            header.setDate(DateTime(result.first().time * 1000))
            header.setEpisodeInfo(totalEpisodes, episode)
        }
    }

    override fun clear() {
        adapter.removeHeader()
        adapter.removeFooter()

        streamAdapter.clear()
    }

    override fun constructLoadingRequest(): LoadingRequest<Array<Stream>> {
        return LoadingRequest(StreamsRequest(id, episode, language))
    }

    private fun switchEpisode(newEpisode: Int) {
        arguments.putInt(ARGUMENT_EPISODE, newEpisode)
        (activity as AnimeActivity).updateEpisode(newEpisode)

        reset()
    }

    @Synchronized
    private fun synchronize(episodeToSet: Int? = null) {
        if (episodeToSet == null) {
            reminderTask?.cancel()

            reminderTask = null
            reminderEpisode = null
        } else if (episodeToSet != reminderEpisode) {
            reminderTask?.cancel()

            reminderEpisode = episodeToSet
            reminderTask = MainApplication.proxerConnection.execute(SetReminderRequest(id,
                    reminderEpisode!!, language, CategoryParameter.ANIME),
                    {
                        reminderTask = null
                        reminderEpisode = null

                        Snackbar.make(root, R.string.fragment_set_reminder_success,
                                Snackbar.LENGTH_LONG).show()
                    },
                    {
                        reminderTask = null
                        reminderEpisode = null

                        Snackbar.make(root, R.string.fragment_set_reminder_error,
                                Snackbar.LENGTH_LONG).show()
                    })
        }
    }
}