package com.hayirlicumalarsil.scan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hayirlicumalarsil.MainActivity
import com.hayirlicumalarsil.R

/**
 * Cumartesi hatırlatıcısı tetiklendiğinde bildirim gönderir ve bir sonraki
 * cumartesi için kendini yeniden kurar. Bildirimdeki "Evet, tara" düğmesi
 * uygulamayı açıp taramayı başlatır.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context)
        // Bir sonraki cumartesi için yeniden kur.
        ReminderScheduler.schedule(context)
    }

    private fun showNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Haftalık hatırlatıcı",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Cumartesi cuma görsellerini temizleme hatırlatması" }
            )
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val scanIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_AUTO_SCAN, true)
        }
        val openPending = PendingIntent.getActivity(
            context, 1, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val scanPending = PendingIntent.getActivity(
            context, 2, scanIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Cuma geçti 🕌")
            .setContentText("Gelen Hayırlı Cumalar görsellerini temizleyelim mi?")
            .setSmallIcon(R.drawable.ic_scan)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, "Evet, tara", scanPending)
            .build()

        try {
            nm.notify(NOTIF_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS reddedilmişse sessizce geç.
        }
    }

    private companion object {
        const val CHANNEL_ID = "weekly_reminder"
        const val NOTIF_ID = 2001
    }
}
