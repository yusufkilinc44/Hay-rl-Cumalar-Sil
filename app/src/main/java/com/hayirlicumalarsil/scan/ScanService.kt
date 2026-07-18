package com.hayirlicumalarsil.scan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hayirlicumalarsil.MainActivity
import com.hayirlicumalarsil.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Taramayı foreground'da tutan ince sarmalayıcı. Asıl iş [ScanEngine]'de.
 * Amaç: uzun süren tarama sırasında OS'un işlemi öldürme ihtimalini düşürmek.
 * Durum [ScanState.Scanning]'ten çıkınca servis kendini durdurur.
 */
class ScanService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(0, 0, 0), type)

        ScanEngine.start(applicationContext)

        // Durumu bildirime yansıt ve tarama bitince kendini durdur.
        lifecycleScope.launch {
            ScanEngine.state.collectLatest { state ->
                when (state) {
                    is ScanState.Scanning -> updateNotification(state)
                    else -> {
                        ServiceCompat.stopForeground(this@ScanService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }

        // OS öldürürse bare Service'i null intent ile diriltme; devam modeli
        // "tekrar TARA'ya bas" (skip-cache sayesinde hızlı).
        return START_NOT_STICKY
    }

    /** Android 15+ per-tip FGS zaman aşımı savunması. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        ScanEngine.cancel()
        stopSelf()
    }

    private fun updateNotification(state: ScanState.Scanning) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(state.scanned, state.total, state.found, state.skipped))
    }

    private fun buildNotification(scanned: Int, total: Int, found: Int, skipped: Int = 0) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(
                if (total > 0) "Taranıyor: $scanned / $total görsel"
                else "Görseller taranıyor"
            )
            .setContentText(
                when {
                    total <= 0 -> "Görseller listeleniyor…"
                    skipped > 0 -> "$found aday • $skipped görsel atlandı (önceden tarandı)"
                    else -> "$found cuma görseli adayı bulundu"
                }
            )
            .setSmallIcon(R.drawable.ic_scan)
            .setOngoing(true)
            .setProgress(total, scanned, total == 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent())
            .build()

    /** Bildirime dokununca uygulamayı öne getirir. */
    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tarama",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Cuma görseli tarama ilerlemesi" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private companion object {
        const val CHANNEL_ID = "scan_progress"
        const val NOTIF_ID = 1001
    }
}
