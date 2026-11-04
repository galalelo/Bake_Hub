package com.bakehub.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bakehub.app.MainActivity
import com.bakehub.app.R

/** Notification channels and builders for BakeHub's two kinds of notification. */
object NotificationHelper {
    const val CHANNEL_NEW_RECIPES = "new_recipes"
    const val CHANNEL_TIMERS = "bake_timers"
    const val TOPIC_NEW_RECIPES = "new_recipes"

    private const val ID_TIMER = 1001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NEW_RECIPES,
                context.getString(R.string.channel_new_recipes_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.channel_new_recipes_desc) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TIMERS,
                context.getString(R.string.channel_timers_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.channel_timers_desc) }
        )
    }

    /** False on Android 13+ until the user has granted POST_NOTIFICATIONS. */
    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** "New community recipe" push, shown in the user's current app language. */
    fun showNewRecipe(context: Context, recipeTitle: String, author: String, recipeId: String) {
        post(
            context,
            id = recipeId.hashCode(),
            channel = CHANNEL_NEW_RECIPES,
            title = context.getString(R.string.notif_new_recipe_title),
            body = context.getString(R.string.notif_new_recipe_body, recipeTitle, author)
        )
    }

    /** Local notification when a Bake Mode step timer finishes (useful if the screen is off). */
    fun showTimerDone(context: Context, recipeTitle: String) {
        post(
            context,
            id = ID_TIMER,
            channel = CHANNEL_TIMERS,
            title = context.getString(R.string.notif_timer_title),
            body = context.getString(R.string.notif_timer_body, recipeTitle)
        )
    }

    private fun post(context: Context, id: Int, channel: String, title: String, body: String) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }
}
