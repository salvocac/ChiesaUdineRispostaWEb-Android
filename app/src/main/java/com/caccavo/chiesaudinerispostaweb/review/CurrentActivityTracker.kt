package com.caccavo.chiesaudinerispostaweb.review

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tiene traccia dell'activity attualmente in primo piano: serve al flusso di recensione
 * in-app, che può essere lanciato solo da un'activity visibile, mentre la fine della
 * lettura viene notificata dall'audio manager che conosce solo l'application context.
 */
object CurrentActivityTracker : Application.ActivityLifecycleCallbacks {

    private var current: WeakReference<Activity>? = null

    val resumedActivity: Activity?
        get() = current?.get()

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (current?.get() === activity) {
            current = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
