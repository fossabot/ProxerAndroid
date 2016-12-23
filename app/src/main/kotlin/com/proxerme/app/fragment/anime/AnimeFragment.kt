package com.proxerme.app.fragment.anime

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.proxerme.app.R
import com.proxerme.app.activity.AnimeActivity
import com.proxerme.app.activity.ProfileActivity
import com.proxerme.app.adapter.anime.StreamAdapter
import com.proxerme.app.fragment.anime.AnimeFragment.AnimeInput
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.StreamResolutionTask
import com.proxerme.app.task.StreamResolutionTask.*
import com.proxerme.app.task.framework.*
import com.proxerme.app.util.ErrorUtils
import com.proxerme.app.util.Validators
import com.proxerme.app.util.ViewUtils
import com.proxerme.app.util.bindView
import com.proxerme.app.view.MediaControlView
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.ProxerException.UNPARSABLE
import com.proxerme.library.connection.anime.entity.Stream
import com.proxerme.library.connection.anime.request.LinkRequest
import com.proxerme.library.connection.anime.request.StreamsRequest
import com.proxerme.library.connection.info.request.SetUserInfoRequest
import com.proxerme.library.connection.ucp.request.SetReminderRequest
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import com.proxerme.library.parameters.ViewStateParameter
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class AnimeFragment : SingleLoadingFragment<AnimeInput, Array<Stream>>() {

    companion object {
        private const val ARGUMENT_ID = "id"
        private const val ARGUMENT_EPISODE = "episode"
        private const val ARGUMENT_TOTAL_EPISODES = "total_episodes"
        private const val ARGUMENT_LANGUAGE = "language"

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

    private val reminderSuccess = { nothing: Void? ->
        Snackbar.make(root, R.string.fragment_set_reminder_success, Snackbar.LENGTH_LONG).show()
    }

    private val reminderException = { exception: Exception ->
        val action = ErrorUtils.handle(activity as AppCompatActivity, exception)

        ViewUtils.makeMultilineSnackbar(root,
                getString(R.string.fragment_set_reminder_error, action.message),
                Snackbar.LENGTH_LONG).setAction(action.buttonMessage, action.buttonAction).show()
    }

    private val streamResolverSuccess = { result: StreamResolutionResult ->
        if (result.intent.type == "text/html") {
            showPage(HttpUrl.parse(result.intent.data.toString()))
        } else {
            try {
                context.startActivity(result.intent)
            } catch (exception: ActivityNotFoundException) {
                result.notFoundAction.invoke(activity as AppCompatActivity)
            }
        }
    }

    private val streamResolverException = { exception: Exception ->
        when (exception) {
            is NoResolverException -> {
                Snackbar.make(root, R.string.error_unsupported_hoster, Snackbar.LENGTH_LONG).show()
            }
            is StreamResolutionException -> {
                Snackbar.make(root, R.string.error_stream_resolution, Snackbar.LENGTH_LONG).show()
            }
            else -> {
                val action = ErrorUtils.handle(activity as AppCompatActivity, exception)

                Snackbar.make(root, action.message, Snackbar.LENGTH_LONG)
                        .setAction(action.buttonMessage, action.buttonAction).show()
            }
        }
    }

    private val urlTransform = { it: String ->
        try {
            HttpUrl.parse(when {
                it.isBlank() -> throw ProxerException(UNPARSABLE)
                it.startsWith("//") -> "http:$it"
                else -> it
            })
        } catch(exception: Exception) {
            throw ProxerException(UNPARSABLE)
        }
    }

    override val section = SectionManager.Section.ANIME

    override val isWorking: Boolean
        get() = super.isWorking && streamResolverTask.isWorking

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

    private val reminderTask = constructReminderTask()

    private val streamResolverTask = constructStreamResolverTask()

    private lateinit var header: MediaControlView

    private val root: ViewGroup by bindView(R.id.root)
    private val streams: RecyclerView by bindView(R.id.streams)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        streamAdapter = StreamAdapter({ adapter.getRealPosition(it) })
        adapter = EasyHeaderFooterAdapter(streamAdapter)

        streamAdapter.callback = object : StreamAdapter.StreamAdapterCallback() {
            override fun onUploaderClick(item: Stream) {
                ProfileActivity.navigateTo(activity, item.uploaderId, item.uploader)
            }

            override fun onTranslatorGroupClick(item: Stream) {
                item.subgroupId?.let {
                    showPage(ProxerUrlHolder.getSubgroupUrl(it,
                            ProxerUrlHolder.DEVICE_QUERY_PARAMETER_DEFAULT))
                }
            }

            override fun onWatchClick(item: Stream) {
                streamResolverTask.cancel()
                streamResolverTask.execute(item.id)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        header = inflater.inflate(R.layout.item_media_header, container, false) as MediaControlView

        header.textResolver = object : MediaControlView.TextResourceResolver {
            override fun finish() = context.getString(R.string.finished_media)
            override fun next() = context.getString(R.string.fragment_anime_next_episode)
            override fun previous() = context.getString(R.string.fragment_anime_previous_episode)
            override fun reminderThis() =
                    context.getString(R.string.fragment_anime_reminder_this_episode)

            override fun reminderNext() =
                    context.getString(R.string.fragment_anime_reminder_next_episode)
        }

        return inflater.inflate(R.layout.fragment_anime, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        streams.layoutManager = LinearLayoutManager(context)
        streams.adapter = adapter

        header.onReminderClickListener = {
            reminderTask.execute(ReminderInput(id, it, language, false))
        }

        header.onFinishClickListener = {
            reminderTask.execute(ReminderInput(id, it, language, true))
        }

        header.onSwitchClickListener = {
            switchEpisode(it)
        }

        header.setUploader(null)
        header.setTranslatorGroup(null)
        header.setDate(null)
    }

    override fun present(data: Array<Stream>) {
        if (data.isEmpty()) {
            showError(getString(R.string.error_no_data_anime), null)
        } else {
            header.setEpisodeInfo(totalEpisodes, episode)
            streamAdapter.replace(data)
            adapter.setHeader(header)
        }
    }

    override fun clear() {
        adapter.removeHeader()
        streamAdapter.clear()

        super.clear()
    }

    override fun onDestroyView() {
        adapter.removeHeader()
        streams.adapter = null
        streams.layoutManager = null

        super.onDestroyView()
    }

    override fun onDestroy() {
        reminderTask.destroy()
        streamResolverTask.destroy()
        streamAdapter.removeCallback()

        super.onDestroy()
    }

    override fun constructTask(): ListenableTask<AnimeInput, Array<Stream>> {
        return ProxerLoadingTask({ StreamsRequest(it.id, it.episode, it.language) })
    }

    override fun constructInput(): AnimeInput {
        return AnimeInput(id, episode, language)
    }

    private fun constructReminderTask(): Task<ReminderInput, Void?> {
        return ValidatingTask(ProxerLoadingTask({
            if (it.isFinished) {
                SetUserInfoRequest(it.id, ViewStateParameter.FINISHED)
            } else {
                SetReminderRequest(it.id, it.episode, it.language, CategoryParameter.ANIME)
            }
        }), { Validators.validateLogin() }, reminderSuccess, reminderException)
    }

    private fun constructStreamResolverTask(): Task<String, StreamResolutionResult> {
        return ListeningTask((StreamedTask(ProxerLoadingTask(::LinkRequest),
                StreamResolutionTask(), urlTransform)),
                streamResolverSuccess, streamResolverException).onStart {
            setRefreshing(true)
        }.onFinish {
            updateRefreshing()
        }
    }

    private fun switchEpisode(newEpisode: Int) {
        arguments.putInt(ARGUMENT_EPISODE, newEpisode)
        (activity as AnimeActivity).updateEpisode(newEpisode)

        reset()
    }

    open class AnimeInput(val id: String, val episode: Int, val language: String)
    class ReminderInput(id: String, episode: Int, language: String, val isFinished: Boolean) :
            AnimeInput(id, episode, language)
}