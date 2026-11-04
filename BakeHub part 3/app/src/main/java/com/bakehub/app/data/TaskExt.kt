package com.bakehub.app.data

import com.bakehub.app.util.NetworkMonitor
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val WRITE_ACK_TIMEOUT_MS = 8_000L

/**
 * Awaits a Firestore write, but never blocks the UI forever when the device is offline.
 *
 * Firestore applies a write to its local cache immediately and queues it for the server, but the
 * returned Task only completes once the SERVER acknowledges it, which cannot happen offline.
 * - Online: waits for the acknowledgement (so permission errors still surface to the caller).
 * - Offline (or the connection drops mid-write): returns null straight away; Firestore keeps the
 *   write queued and syncs it automatically on reconnect.
 */
suspend fun <T> Task<T>.awaitOrQueue(): T? =
    if (NetworkMonitor.isOnline.value) withTimeoutOrNull(WRITE_ACK_TIMEOUT_MS) { await() } else null
