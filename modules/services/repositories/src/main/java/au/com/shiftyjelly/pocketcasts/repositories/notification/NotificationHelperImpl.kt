package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.NotificationBroadcastReceiver.Companion.INTENT_EXTRA_NOTIFICATION_TAG
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.provider.Settings as OsSettings
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class NotificationHelperImpl @Inject constructor(@ApplicationContext private val context: Context) : NotificationHelper {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    override fun hasNotificationsPermission() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun openNotificationSettings(activity: Activity?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity == null) return

        val intent = Intent(OsSettings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(OsSettings.EXTRA_APP_PACKAGE, activity.packageName)
        activity.startActivity(intent)
    }

    override fun isShowing(notificationId: Int): Boolean {
        return notificationManager?.activeNotifications?.firstOrNull { it.id == notificationId } != null
    }

    override fun setupNotificationChannels() {
        // we only need to create notification channels in Android O and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelList = ArrayList<NotificationChannel>()
        // set up playback channel
        val playbackChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PLAYBACK.id, "Playback", NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(LR.string.notification_channel_description_playback)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(playbackChannel)

        // set up download channel
        val downloadChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_DOWNLOAD.id, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(LR.string.notification_channel_description_download)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(downloadChannel)

        // set up new episode channel
        val episodeChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_EPISODE.id, "New Episodes", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(LR.string.notification_channel_description_episode)
            setShowBadge(true)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(episodeChannel)

        val playbackErrorChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PLAYBACK_ERROR.id, "Playback Errors", NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(LR.string.notification_channel_description_playback_error)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(playbackErrorChannel)

        val podcastImportChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PODCAST.id, "Podcast Import", NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(LR.string.notification_channel_description_podcast_import)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(podcastImportChannel)

        val signInErrorChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_SIGN_IN_ERROR.id, "Sign-in Error", NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(LR.string.notification_channel_description_sign_in_error)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(signInErrorChannel)

        val bookmarkChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_BOOKMARK.id, "Bookmark", NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(LR.string.notification_channel_description_bookmark)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(bookmarkChannel)

        val fixDownloadsChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_FIX_DOWNLOADS.id, "Fix Downloads", NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(LR.string.notification_channel_description_fix_downloads)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(fixDownloadsChannel)

        val fixDownloadsCompleteChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_FIX_DOWNLOADS_COMPLETE.id, "Fix Downloads Complete", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(LR.string.notification_channel_description_fix_downloads_complete)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(fixDownloadsCompleteChannel)

        val dailyRemindersChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_DAILY_REMINDERS.id, "Daily Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(LR.string.notification_channel_description_daily_reminders)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(dailyRemindersChannel)

        val trendingAndRecommendationsChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_TRENDING_AND_RECOMMENDATIONS.id, "Trending & Recommendations", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(LR.string.notification_channel_description_trending_and_recommendations)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(trendingAndRecommendationsChannel)

        val newFeaturesAndTipsChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_NEW_FEATURES_AND_TIPS.id, "New Features & Tips", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(LR.string.notification_channel_description_new_features_and_tips)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(newFeaturesAndTipsChannel)

        val offersChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_OFFERS.id, "Offers", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(LR.string.notification_channel_description_offers)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(offersChannel)

        notificationManager.createNotificationChannels(channelList)
    }

    override fun downloadChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_DOWNLOAD.id)
    }

    override fun playbackChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PLAYBACK.id)
    }

    override fun episodeNotificationChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_EPISODE.id)
    }

    override fun playbackErrorChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PLAYBACK_ERROR.id).setPriority(NotificationCompat.PRIORITY_MAX)
    }

    override fun podcastImportChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PODCAST.id)
    }

    override fun bookmarkChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_BOOKMARK.id)
    }

    override fun downloadsFixChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_FIX_DOWNLOADS.id)
    }

    override fun downloadsFixCompleteChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_FIX_DOWNLOADS_COMPLETE.id)
    }

    override fun dailyRemindersChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_DAILY_REMINDERS.id)
    }

    override fun trendingAndRecommendationsChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_TRENDING_AND_RECOMMENDATIONS.id)
    }

    override fun featuresAndTipsChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_NEW_FEATURES_AND_TIPS.id)
    }

    override fun offersChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_OFFERS.id)
    }

    /**
     * Opens the system notification activity for the episode channel.
     */
    override fun openEpisodeNotificationSettings(activity: Activity?) {
        openNotificationChannelSettings(activity, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_EPISODE.id)
    }

    override fun openDailyReminderNotificationSettings(activity: Activity?) {
        openNotificationChannelSettings(activity, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_DAILY_REMINDERS.id)
    }

    override fun openTrendingAndRecommendationsNotificationSettings(activity: Activity?) {
        openNotificationChannelSettings(activity, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_TRENDING_AND_RECOMMENDATIONS.id)
    }

    override fun openNewFeaturesAndTipsNotificationSettings(activity: Activity?) {
        openNotificationChannelSettings(activity, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_NEW_FEATURES_AND_TIPS.id)
    }

    override fun openOffersNotificationSettings(activity: Activity?) {
        openNotificationChannelSettings(activity, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_OFFERS.id)
    }

    override fun removeNotification(intentExtras: Bundle?, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationTag = intentExtras?.getString(INTENT_EXTRA_NOTIFICATION_TAG, null)
        if (!notificationTag.isNullOrBlank()) {
            manager.cancel(notificationId)
        }
    }

    private fun openNotificationChannelSettings(activity: Activity?, channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity == null) return

        val intent = Intent(OsSettings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        intent.putExtra(OsSettings.EXTRA_APP_PACKAGE, activity.packageName)
        intent.putExtra(OsSettings.EXTRA_CHANNEL_ID, channelId)
        activity.startActivity(intent)
    }
}
