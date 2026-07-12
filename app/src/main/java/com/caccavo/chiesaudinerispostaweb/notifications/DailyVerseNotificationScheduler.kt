package com.caccavo.chiesaudinerispostaweb.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Promemoria mattutino del versetto del giorno: una notifica ogni giorno alle 8:00,
 * come sulla versione iOS. Usa un allarme ripetuto inesatto (il sistema può spostarlo
 * di qualche minuto per risparmiare batteria) e viene riprogrammato al riavvio del
 * dispositivo dal DailyVerseReminderReceiver.
 */
object DailyVerseNotificationScheduler {

    const val CHANNEL_ID = "daily-verse-reminder"
    private const val REQUEST_CODE = 1001

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Versetto del giorno",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Promemoria mattutino per leggere il versetto del giorno"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun scheduleMorningReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyVerseReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val next8am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next8am.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
