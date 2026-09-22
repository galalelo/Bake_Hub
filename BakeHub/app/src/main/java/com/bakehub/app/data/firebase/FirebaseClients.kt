package com.bakehub.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Single shared access point for the two Firebase products BakeHub uses:
 * Authentication (email/password accounts) and Firestore (the app's database).
 *
 * These are lazily initialised so the FirebaseApp (configured from
 * app/google-services.json at build time) is guaranteed to be ready.
 */
object FirebaseClients {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
}
