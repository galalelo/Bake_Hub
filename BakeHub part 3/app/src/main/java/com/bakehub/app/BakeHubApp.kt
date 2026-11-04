package com.bakehub.app

import android.app.Application
import com.bakehub.app.data.OnlineRecipeCache
import com.bakehub.app.notifications.NotificationHelper
import com.bakehub.app.util.AppPreferences
import com.bakehub.app.util.NetworkMonitor

/**
 * Application entry point. Initialises the small app-wide services that need a
 * Context before any screen is shown: preferences, connectivity monitoring (offline
 * mode), the offline recipe cache and the notification channels.
 *
 * FirebaseApp itself is initialised automatically from google-services.json.
 */
class BakeHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(this)
        NetworkMonitor.start(this)
        OnlineRecipeCache.init(this)
        NotificationHelper.createChannels(this)
    }
}
