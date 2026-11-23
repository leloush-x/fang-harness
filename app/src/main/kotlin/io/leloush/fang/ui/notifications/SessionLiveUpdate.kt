package io.leloush.fang.ui.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import io.leloush.fang.R

/**
 * The ongoing notification for a session still running. On Android 16 and later it asks to be shown as a Live Update;
 * on older versions the same notification reads as an ordinary ongoing one. The caller owns the channel and the
 * notification id, so Fang Harness's existing channels and ids stay untouched.
 */
internal object SessionLiveUpdate {
  fun build(context: Context, channelId: String, title: String, text: String?, open: PendingIntent): Notification =
    NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(title)
      .setContentText(text)
      .setContentIntent(open)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .setStyle(NotificationCompat.ProgressStyle().setProgressIndeterminate(true).setStyledByProgress(false))
      .setRequestPromotedOngoing(true)
      .build()
}
