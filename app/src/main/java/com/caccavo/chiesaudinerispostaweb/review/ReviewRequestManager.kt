package com.caccavo.chiesaudinerispostaweb.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Chiede la recensione su Google Play alla fine di una lettura, al massimo una volta
 * ogni 5 giorni. Google Play decide comunque se mostrare davvero la finestra (limite di
 * sistema), quindi qui registriamo la data solo quando la richiesta viene effettivamente
 * inoltrata al sistema.
 */
object ReviewRequestManager {

    private const val PREFS_NAME = "review_request"
    private const val KEY_LAST_REQUEST = "lastReviewRequestDate"
    private const val MINIMUM_INTERVAL_MS = 5L * 24 * 60 * 60 * 1000

    fun requestIfDue(context: Context) {
        // Senza un'activity in primo piano (es. lettura finita a schermo bloccato) non
        // chiediamo nulla e non consumiamo la finestra dei 5 giorni.
        val activity: Activity = CurrentActivityTracker.resumedActivity ?: return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_REQUEST, 0L)
        if (System.currentTimeMillis() - last < MINIMUM_INTERVAL_MS) return
        prefs.edit().putLong(KEY_LAST_REQUEST, System.currentTimeMillis()).apply()

        val manager = ReviewManagerFactory.create(context)
        manager.requestReviewFlow().addOnSuccessListener { reviewInfo ->
            CurrentActivityTracker.resumedActivity?.let { current ->
                manager.launchReviewFlow(current, reviewInfo)
            }
        }
    }
}
