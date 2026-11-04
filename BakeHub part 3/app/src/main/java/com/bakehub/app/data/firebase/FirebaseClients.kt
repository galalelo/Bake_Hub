package com.bakehub.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Single shared access point for the Firebase products BakeHub uses:
 * Authentication (email/password + Google SSO) and Firestore (the app's database).
 *
 * Firestore is configured with an explicit on-disk cache so recipes, the user's profile and the
 * grocery list stay readable offline, and local edits are queued until the device reconnects.
 * Settings must be applied before the first Firestore call, which the lazy initialiser guarantees.
 */
object FirebaseClients {
    private const val OFFLINE_CACHE_BYTES = 100L * 1024 * 1024 // 100 MB

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(OFFLINE_CACHE_BYTES)
                        .build()
                )
                .build()
        }
    }
}
