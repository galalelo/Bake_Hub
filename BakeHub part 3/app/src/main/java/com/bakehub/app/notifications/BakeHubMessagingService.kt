package com.bakehub.app.notifications

import com.bakehub.app.data.firebase.FirebaseClients
import com.bakehub.app.util.AppPreferences
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives real-time pushes from the `notifyNewRecipe` Cloud Function.
 *
 * The function sends a *data-only* message (type, recipeId, title, author) so this service is
 * called whether the app is open or closed, and the notification text is built here in the
 * user's own app language instead of being fixed by the server.
 */
class BakeHubMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "new_recipe") return
        if (!AppPreferences.notificationsEnabled) return

        val title = data["title"].orEmpty()
        val author = data["author"].orEmpty()
        val recipeId = data["recipeId"].orEmpty()
        if (title.isBlank() || recipeId.isBlank()) return

        // Don't notify people about recipes they just published themselves.
        if (author.isNotBlank() && author == FirebaseClients.auth.currentUser?.displayName) return

        NotificationHelper.showNewRecipe(applicationContext, title, author, recipeId)
    }

    override fun onNewToken(token: String) {
        AppNotifications.saveToken(token)
    }
}
