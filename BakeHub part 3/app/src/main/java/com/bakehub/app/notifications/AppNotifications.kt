package com.bakehub.app.notifications

import com.bakehub.app.data.firebase.FirebaseClients
import com.bakehub.app.util.AppPreferences
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Wires the signed-in user's device into Firebase Cloud Messaging:
 * - subscribes/unsubscribes the "new_recipes" topic according to the Settings toggle, and
 * - stores the device's FCM token on users/{uid} so targeted pushes are possible later.
 */
object AppNotifications {

    /** Applies the user's notification preference. Safe to call repeatedly. */
    fun syncSubscription() {
        val messaging = FirebaseMessaging.getInstance()
        if (AppPreferences.notificationsEnabled) {
            messaging.subscribeToTopic(NotificationHelper.TOPIC_NEW_RECIPES)
        } else {
            messaging.unsubscribeFromTopic(NotificationHelper.TOPIC_NEW_RECIPES)
        }
    }

    fun registerToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { saveToken(it) }
    }

    fun saveToken(token: String) {
        val uid = FirebaseClients.auth.currentUser?.uid ?: return
        FirebaseClients.firestore.collection("users").document(uid)
            .set(
                mapOf("fcmToken" to token, "fcmUpdatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            )
    }

    /** Stop pushes for the previous account on this device when someone signs out / deletes. */
    fun onLogout() {
        val messaging = FirebaseMessaging.getInstance()
        messaging.unsubscribeFromTopic(NotificationHelper.TOPIC_NEW_RECIPES)
        messaging.deleteToken()
    }
}
