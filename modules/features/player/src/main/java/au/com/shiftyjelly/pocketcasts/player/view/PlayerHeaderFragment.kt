package au.com.shiftyjelly.pocketcasts.player.view

import android.animation.LayoutTransition
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.ads.AdReportFragment
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.ad.AdBanner
import au.com.shiftyjelly.pocketcasts.compose.ad.rememberAdColors
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedNonNullVisibility
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.player.binding.setSeekBarState
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterPlayerHeaderBinding
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivity
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivityContract
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.PlayerControls
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.PlayerHeadingSection
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.PlayerVisuals
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.PlayerVisualsState
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.VisualContentState
import au.com.shiftyjelly.pocketcasts.player.view.shelf.PlayerShelf
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptPageWrapper
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptSearchViewModel
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel
import au.com.shiftyjelly.pocketcasts.player.view.video.VideoActivity
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.NavigationState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.SnackbarMessage
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.TransitionState
import au.com.shiftyjelly.pocketcasts.reimagine.ShareDialogFragment
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog.ButtonType.Danger
import au.com.shiftyjelly.pocketcasts.views.extensions.spring
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val UP_NEXT_FLING_VELOCITY_THRESHOLD = 1000.0f

@AndroidEntryPoint
class PlayerHeaderFragment : BaseFragment(), PlayerClickListener {
    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var warningsHelper: WarningsHelper

    private val viewModel: PlayerViewModel by activityViewModels()
    private val shelfSharedViewModel: ShelfSharedViewModel by activityViewModels()
    private val transcriptViewModel by viewModels<TranscriptViewModel>({ requireParentFragment() })
    private val transcriptSearchViewModel by viewModels<TranscriptSearchViewModel>({ requireParentFragment() })
    private var binding: AdapterPlayerHeaderBinding? = null
    private val sourceView = SourceView.PLAYER

    private val activityLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(BookmarkActivityContract()) { result ->
        showViewBookmarksSnackbar(result)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = AdapterPlayerHeaderBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.seekBar.changeListener = object : PlayerSeekBar.OnUserSeekListener {
            override fun onSeekPositionChangeStop(progress: Duration, seekComplete: () -> Unit) {
                val progressMs = progress.inWholeMilliseconds.toInt()
                viewModel.seekToMs(progressMs, seekComplete)
                playbackManager.trackPlaybackSeek(progressMs, SourceView.PLAYER)
            }

            override fun onSeekPositionChanging(progress: Duration) {}

            override fun onSeekPositionChangeStart() {
            }
        }

        setupArtworkSectionComposeView()
        setupPlayerHeadingSectionComposeView()
        setupPlayerControlsComposeView()
        setupShelfComposeView()

        setupTranscriptPage()
        observeTranscriptPageTransition()

        setupUpNextDrag(binding)

        viewModel.listDataLive.observe(viewLifecycleOwner) {
            val podcastHeader = it.podcastHeader

            binding.playerGroup.setBackgroundColor(podcastHeader.backgroundColor)
            binding.seekBar.setSeekBarState(
                duration = podcastHeader.durationMs.milliseconds,
                position = podcastHeader.positionMs.milliseconds,
                chapters = podcastHeader.chapters,
                playbackSpeed = podcastHeader.playbackEffects.playbackSpeed,
                adjustDuration = podcastHeader.adjustRemainingTimeDuration,
                bufferedUpTo = podcastHeader.bufferedUpToMs,
                isBuffering = podcastHeader.isBuffering,
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                podcastColorsFlow().collect { podcastColors ->
                    val playerColors = PlayerColors(theme.activeTheme, podcastColors ?: PodcastColors.ForUserEpisode)

                    binding.playerGroup.setBackgroundColor(playerColors.background01.toArgb())
                    binding.seekBar.setTintColor(playerColors.highlight01.toArgb(), theme.activeTheme)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.snackbarMessages.collect { message ->
                    if (message == PlayerViewModel.SnackbarMessage.ShowBatteryWarningIfAppropriate) {
                        warningsHelper.showBatteryWarningSnackbarIfAppropriate(snackbarParentView = view)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationState.collect { navigationState ->
                    when (navigationState) {
                        is PlayerViewModel.NavigationState.ShowStreamingWarningDialog -> {
                            warningsHelper.streamingWarningDialog(episode = navigationState.episode, snackbarParentView = view, sourceView = sourceView)
                                .show(parentFragmentManager, "streaming dialog")
                        }

                        PlayerViewModel.NavigationState.ShowSkipForwardLongPressOptionsDialog -> {
                            LongPressOptionsFragment().show(parentFragmentManager, "longpressoptions")
                        }

                        is PlayerViewModel.NavigationState.OpenChapterAt -> {
                            (parentFragment as? PlayerContainerFragment)?.openChaptersAt(navigationState.chapter)
                        }

                        is PlayerViewModel.NavigationState.OpenPodcastPage -> {
                            (activity as? FragmentHostListener)?.let { listener ->
                                listener.closePlayer()
                                listener.openPodcastPage(navigationState.podcastUuid, navigationState.source.analyticsValue)
                            }
                        }

                        is PlayerViewModel.NavigationState.OpenChapterUrl -> {
                            val chapterUrl = navigationState.chapterUrl
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = chapterUrl.toUri()
                            try {
                                startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Timber.e(e)
                                UiUtil.displayAlertError(requireContext(), getString(LR.string.player_open_url_failed, chapterUrl), null)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupArtworkSectionComposeView() {
        binding?.artworkSectionComposeView?.setContentWithViewCompositionStrategy {
            val state by remember { playerVisualsStateFlow() }.collectAsState(PlayerVisualsState.Empty)
            val podcastColors by remember { podcastColorsFlow() }.collectAsState(null)
            val ads by viewModel.activeAds.collectAsState()
            val player by viewModel.playerFlow.collectAsState()

            AppTheme(theme.activeTheme) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp),
                ) {
                    AnimatedNonNullVisibility(
                        item = ads.firstOrNull().takeIf { podcastColors != null },
                        enter = adEnterTransition,
                        exit = adExitTransition,
                    ) { ad ->
                        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
                            AdBanner(
                                ad = ad,
                                colors = rememberAdColors().bannerAd,
                                onAdClick = {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, ad.ctaUrl.toUri())
                                        startActivity(intent)
                                    }.onFailure { LogBuffer.e("Ads", it, "Failed to open an ad: ${ad.id}") }
                                },
                                onOptionsClick = {
                                    if (parentFragmentManager.findFragmentByTag("ad_report") == null) {
                                        AdReportFragment
                                            .newInstance(ad, podcastColors)
                                            .show(parentFragmentManager, "ad_report")
                                    }
                                },
                                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp),
                            )
                        }
                    }
                    PlayerVisuals(
                        state = state,
                        player = player,
                        onChapterUrlClick = viewModel::onChapterUrlClick,
                        configureVideoView = { videoView ->
                            videoView.setOnClickListener { onFullScreenVideoClick() }
                        },
                    )
                }
            }
        }
    }

    private fun setupPlayerHeadingSectionComposeView() {
        binding?.playerHeadingSectionComposeView?.setContentWithViewCompositionStrategy {
            AppTheme(theme.activeTheme) {
                PlayerHeadingSection(
                    playerViewModel = viewModel,
                    shelfSharedViewModel = shelfSharedViewModel,
                )
            }
        }
    }

    private fun setupPlayerControlsComposeView() {
        binding?.playerControlsComposeView?.setContentWithViewCompositionStrategy {
            val podcastColors by remember { podcastColorsFlow() }.collectAsState(null)

            AppTheme(theme.activeTheme) {
                CompositionLocalProvider(
                    LocalPodcastColors provides podcastColors,
                ) {
                    PlayerControls(
                        playerViewModel = viewModel,
                    )
                }
            }
        }
    }

    private fun setupShelfComposeView() {
        binding?.shelfComposeView?.setContentWithViewCompositionStrategy {
            AppTheme(theme.activeTheme) {
                PlayerShelf(
                    theme = theme,
                    shelfSharedViewModel = shelfSharedViewModel,
                    transcriptViewModel = transcriptViewModel,
                    playerViewModel = viewModel,
                )
                LaunchedEffect(Unit) {
                    observeShelfItemNavigationState()
                }
                LaunchedEffect(Unit) {
                    observeShelfItemSnackbarMessages()
                }
            }
        }
    }

    private fun observeShelfItemSnackbarMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                shelfSharedViewModel.snackbarMessages.collect { message ->
                    val text = when (message) {
                        SnackbarMessage.EpisodeDownloadStarted -> LR.string.episode_queued_for_download
                        SnackbarMessage.EpisodeRemoved -> LR.string.episode_was_removed
                        SnackbarMessage.TranscriptNotAvailable -> LR.string.transcript_error_not_available
                        SnackbarMessage.ShareNotAvailable -> LR.string.sharing_is_not_available_for_private_podcasts
                    }
                    showSnackBar(text = getString(text))
                }
            }
        }
    }

    private fun observeShelfItemNavigationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                shelfSharedViewModel.navigationState.collect { navigationState ->
                    when (navigationState) {
                        NavigationState.ShowEffectsOption -> {
                            EffectsFragment().show(parentFragmentManager, "effects")
                        }

                        NavigationState.ShowSleepTimerOptions -> {
                            SleepFragment().show(parentFragmentManager, "sleep_sheet")
                        }

                        is NavigationState.ShowShareDialog -> {
                            ShareDialogFragment
                                .newThemedInstance(navigationState.podcast, navigationState.episode, theme, SourceView.PLAYER)
                                .show(parentFragmentManager, "share_dialog")
                        }

                        is NavigationState.ShowPodcast -> {
                            (activity as FragmentHostListener).closePlayer()
                            (activity as? FragmentHostListener)?.openPodcastPage(navigationState.podcast.uuid, SourceView.PLAYER.analyticsValue)
                        }

                        is NavigationState.ShowCloudFiles -> {
                            (activity as FragmentHostListener).closePlayer()
                            (activity as? FragmentHostListener)?.openCloudFiles()
                        }

                        is NavigationState.ShowMarkAsPlayedConfirmation -> {
                            context?.let {
                                ConfirmationDialog()
                                    .setForceDarkTheme(true)
                                    .setSummary(it.getString(LR.string.player_mark_as_played))
                                    .setIconId(IR.drawable.ic_markasplayed)
                                    .setButtonType(Danger(it.getString(LR.string.player_mark_as_played_button)))
                                    .setOnConfirm {
                                        navigationState.onMarkAsPlayedConfirmed(navigationState.episode)
                                    }
                                    .show(childFragmentManager, "mark_as_played")
                            }
                        }

                        is NavigationState.ShowPodcastEpisodeArchiveConfirmation -> {
                            ConfirmationDialog()
                                .setForceDarkTheme(true)
                                .setSummary(resources.getString(LR.string.player_archive_summary))
                                .setIconId(IR.drawable.ic_archive)
                                .setButtonType(Danger(resources.getString(LR.string.player_archive_title)))
                                .setOnConfirm { navigationState.onArchiveConfirmed(navigationState.episode) }
                                .show(childFragmentManager, "archive")
                        }

                        is NavigationState.ShowUserEpisodeDeleteConfirmation -> {
                            CloudDeleteHelper.getDeleteDialog(navigationState.episode, navigationState.deleteState, navigationState.deleteFunction, resources)
                                .show(childFragmentManager, "archive")
                        }

                        NavigationState.ShowMoreActions -> {
                            // stop double taps
                            if (childFragmentManager.fragments.firstOrNull() is ShelfBottomSheet) return@collect
                            viewModel.episode?.let {
                                ShelfBottomSheet.newInstance(
                                    episodeId = it.uuid,
                                ).show(childFragmentManager, "shelf_bottom_sheet")
                            }
                        }

                        NavigationState.ShowAddBookmark -> {
                            val bookmarkArguments = viewModel.createBookmarkArguments()
                            if (bookmarkArguments != null) {
                                activityLauncher.launch(BookmarkActivity.launchIntent(requireContext(), bookmarkArguments))
                            }
                        }

                        is NavigationState.StartUpsellFlow -> startUpsellFlow(navigationState.source)
                    }
                }
            }
        }
    }

    private fun setupTranscriptPage() {
        binding?.transcriptPage?.setContent {
            TranscriptPageWrapper(
                playerViewModel = viewModel,
                shelfSharedViewModel = shelfSharedViewModel,
                transcriptViewModel = transcriptViewModel,
                searchViewModel = transcriptSearchViewModel,
                theme = theme,
                onClickSubscribe = {
                    val uiState = transcriptViewModel.uiState.value
                    transcriptViewModel.track(AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_SUBSCRIBE_TAPPED, uiState.podcastAndEpisode)
                    OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.Upsell(OnboardingUpgradeSource.GENERATED_TRANSCRIPTS))
                },
            )
        }
    }

    private fun observeTranscriptPageTransition() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                shelfSharedViewModel.transitionState.collect { transitionState ->
                    val uiState = transcriptViewModel.uiState.value

                    when (transitionState) {
                        is TransitionState.OpenTranscript -> {
                            if (uiState.showPaywall) {
                                transcriptViewModel.track(AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_SHOWN, uiState.podcastAndEpisode)
                            }
                            binding?.openTranscript(
                                hidePlayerControls = !transitionState.showPlayerControls,
                            )
                        }

                        is TransitionState.CloseTranscript -> {
                            val event = if (uiState.showPaywall) {
                                AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_DISMISSED
                            } else {
                                AnalyticsEvent.TRANSCRIPT_DISMISSED
                            }
                            transcriptViewModel.track(event, uiState.podcastAndEpisode)
                            binding?.closeTranscript()
                        }
                    }
                }
            }
        }
    }

    private fun AdapterPlayerHeaderBinding.openTranscript(
        hidePlayerControls: Boolean,
    ) {
        if (playerGroup.layoutTransition == null) {
            playerGroup.layoutTransition = LayoutTransition()
        }
        transcriptPage.isVisible = true
        shelfComposeView.isInvisible = true
        val shelfOffset = shelfComposeView.height + shelfComposeView.marginTop
        with(seekBar) {
            isInvisible = hidePlayerControls
            spring(SpringAnimation.TRANSLATION_Y).animateToFinalPosition(shelfOffset.toFloat() + 32.dpToPx(context))
        }
        with(playerControlsComposeView) {
            isInvisible = hidePlayerControls
            spring(SpringAnimation.SCALE_X).animateToFinalPosition(0.6f)
            spring(SpringAnimation.SCALE_Y).animateToFinalPosition(0.6f)
            spring(SpringAnimation.TRANSLATION_Y).animateToFinalPosition(shelfOffset.toFloat())
        }
        val containerFragment = parentFragment as? PlayerContainerFragment
        containerFragment?.updateTabsVisibility(false)
        root.setScrollingEnabled(false)
    }

    private fun AdapterPlayerHeaderBinding.closeTranscript() {
        shelfComposeView.isVisible = true
        transcriptPage.isVisible = false
        with(seekBar) {
            isVisible = true
            spring(SpringAnimation.TRANSLATION_Y).animateToFinalPosition(0f)
        }
        with(playerControlsComposeView) {
            isVisible = true
            spring(SpringAnimation.SCALE_X).animateToFinalPosition(1f)
            spring(SpringAnimation.SCALE_Y).animateToFinalPosition(1f)
            spring(SpringAnimation.TRANSLATION_Y).animateToFinalPosition(0f)
        }
        val containerFragment = parentFragment as? PlayerContainerFragment
        containerFragment?.updateTabsVisibility(true)
        root.setScrollingEnabled(true)
        playerGroup.layoutTransition = null // Reset to null to avoid animation when changing children visibility anytime later
    }

    private fun setupUpNextDrag(binding: AdapterPlayerHeaderBinding) {
        val flingGestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if (binding.transcriptPage.isVisible) return false
                    val containerFragment = parentFragment as? PlayerContainerFragment ?: return false
                    val upNextBottomSheetBehavior = containerFragment.upNextBottomSheetBehavior

                    return if (velocityY < 0 && abs(velocityY) >= UP_NEXT_FLING_VELOCITY_THRESHOLD && upNextBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                        containerFragment.openUpNext()
                        true
                    } else {
                        false
                    }
                }
            },
        )
        @Suppress("ClickableViewAccessibility")
        binding.root.setOnTouchListener { _, event ->
            // This check is a workaround for a behavior between velocityY detected by flingGestureDetector and dragging player bottom sheet.
            // When only the player is expanded and we fling down the velocityY should be positive indicating that direction.
            // However, regardless of flinging up or down the velocityY is always negative because the player's view drags along
            // with a finger and thus velocity computation "gets confused" because MotionEvent positions are relative to the view.
            //
            // Because the fling motion is detected only after we release the finger it means that the player bottom sheet
            // is no longer in an expanded state but in a dragging or a collapsing state.
            if ((activity as? FragmentHostListener)?.getPlayerBottomSheetState() == BottomSheetBehavior.STATE_EXPANDED) {
                flingGestureDetector.onTouchEvent(event)
            } else {
                false
            }
        }
    }

    override fun onShowNotesClick(episodeUuid: String) {
        val fragment = NotesFragment.newInstance(episodeUuid)
        openBottomSheet(fragment)
    }

    private fun startUpsellFlow(source: OnboardingUpgradeSource) {
        val onboardingFlow = OnboardingFlow.Upsell(
            source = source,
        )
        OnboardingLauncher.openOnboardingFlow(requireActivity(), onboardingFlow)
    }

    override fun onClosePlayer() {
        (activity as FragmentHostListener).closePlayer()
    }

    override fun onPictureInPictureClick() {
        val context = context ?: return
        context.startActivity(VideoActivity.buildIntent(enterPictureInPicture = true, context = context))
    }

    override fun onFullScreenVideoClick() {
        val context = context ?: return
        context.startActivity(VideoActivity.buildIntent(context = context))
    }

    private fun openBottomSheet(fragment: Fragment) {
        (activity as FragmentHostListener).showBottomSheet(fragment)
    }

    override fun onSeekPositionChangeStop(progress: Int, seekComplete: () -> Unit) {
        viewModel.seekToMs(progress, seekComplete)
    }

    private fun showViewBookmarksSnackbar(result: BookmarkActivityContract.BookmarkResult?) {
        val view = view
        if (result == null || view == null) {
            return
        }

        val snackbarMessage = if (result.isExistingBookmark) {
            getString(LR.string.bookmark_updated, result.title)
        } else {
            getString(LR.string.bookmark_added, result.title)
        }
        val viewBookmarksAction = View.OnClickListener {
            (parentFragment as? PlayerContainerFragment)?.openBookmarks()
        }

        Snackbar.make(view, snackbarMessage, Snackbar.LENGTH_LONG)
            .setAction(LR.string.settings_view, viewBookmarksAction)
            .setActionTextColor(result.tintColor)
            .setBackgroundTint(ThemeColor.primaryUi01(Theme.ThemeType.DARK))
            .setTextColor(ThemeColor.primaryText01(Theme.ThemeType.DARK))
            .show()
    }

    private fun showSnackBar(text: CharSequence) {
        parentFragment?.view?.let {
            Snackbar.make(it, text, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ThemeColor.primaryUi01(Theme.ThemeType.LIGHT))
                .setTextColor(ThemeColor.primaryText01(Theme.ThemeType.LIGHT))
                .show()
        }
    }

    private fun playerVisualsStateFlow(): Flow<PlayerVisualsState> {
        return viewModel.listDataLive.asFlow()
            .distinctUntilChanged(::isListDataEquivalentForVisuals)
            .map(::createPlayerVisualState)
    }

    private fun isListDataEquivalentForVisuals(old: PlayerViewModel.ListData, new: PlayerViewModel.ListData): Boolean {
        return old.podcastHeader.episode?.uuid == new.podcastHeader.episode?.uuid &&
            old.podcastHeader.useEpisodeArtwork == new.podcastHeader.useEpisodeArtwork &&
            old.podcastHeader.chapter?.index == new.podcastHeader.chapter?.index &&
            old.podcastHeader.isPrepared == new.podcastHeader.isPrepared
    }

    private fun createPlayerVisualState(listData: PlayerViewModel.ListData): PlayerVisualsState {
        val header = listData.podcastHeader
        val contentState = when {
            header.isVideo -> VisualContentState.DisplayVideo(
                chapterUrl = header.chapter?.url,
            )

            header.episode == null -> VisualContentState.NoContent
            else -> VisualContentState.DisplayArtwork(
                episode = header.episode,
                chapterArtworkPath = header.chapter?.imagePath,
                chapterUrl = header.chapter?.url,
                canDisplayEpisodeArtwork = header.useEpisodeArtwork,
            )
        }
        return PlayerVisualsState(contentState, header.isPrepared)
    }

    private fun podcastColorsFlow(): Flow<PodcastColors?> {
        return combine(
            viewModel.episodeFlow,
            viewModel.podcastFlow,
        ) { episode, podcast ->
            if (episode != null) {
                podcast?.let(::PodcastColors) ?: PodcastColors.ForUserEpisode
            } else {
                null
            }
        }
    }
}

private val fadeIn = fadeIn(spring(stiffness = Spring.StiffnessVeryLow))
private val fadeOut = fadeOut(spring(stiffness = Spring.StiffnessVeryLow))
private val expandVertically = expandVertically(spring(stiffness = Spring.StiffnessMediumLow))
private val shrinkVertically = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))

private val adEnterTransition = fadeIn + expandVertically
private val adExitTransition = fadeOut + shrinkVertically
