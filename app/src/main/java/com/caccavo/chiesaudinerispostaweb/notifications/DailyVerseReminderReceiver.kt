package com.caccavo.chiesaudinerispostaweb.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.caccavo.chiesaudinerispostaweb.MainActivity
import com.caccavo.chiesaudinerispostaweb.R

/**
 * Riceve sia l'allarme giornaliero delle 8:00 (mostra la notifica) sia il riavvio del
 * dispositivo (gli allarmi non sopravvivono al reboot, quindi li riprogramma).
 */
class DailyVerseReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyVerseNotificationScheduler.scheduleMorningReminder(context)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DailyVerseNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_book)
            .setContentTitle("Versetto del giorno")
            .setContentText("Leggi il versetto del giorno")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }
}
