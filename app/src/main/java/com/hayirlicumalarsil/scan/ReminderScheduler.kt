package com.hayirlicumalarsil.scan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Haftalık "cumartesi hatırlatıcısı" zamanlayıcısı. Perşembe/cuma geçtikten sonra
 * cumartesi ~10:00'da kullanıcıya "Hayırlı Cumalar görsellerini temizleyelim mi?"
 * bildirimi gönderilir. Tam-alarm izni gerektirmemek için setAndAllowWhileIdle
 * kullanılır ve her tetiklenmede bir sonraki cumartesi yeniden kurulur.
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 4242
    const val HOUR_OF_DAY = 10

    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val triggerAt = nextSaturday()
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } catch (e: SecurityException) {
            // Bazı OEM'lerde kısıtlıysa sessizce geç.
        }
    }

    private fun nextSaturday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, HOUR_OF_DAY)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Bir sonraki cumartesiye ilerle (bugün cumartesi ve saat geçmişse haftaya).
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY ||
            cal.timeInMillis <= System.currentTimeMillis()
        ) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
