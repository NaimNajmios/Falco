package com.najmi.falco.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.najmi.falco.MainActivity
import com.najmi.falco.R
import com.najmi.falco.domain.model.Verdict
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FalcoNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FALCO Verifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for claim verification progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgress(claimId: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_falco)
            .setContentTitle("Verifying claim")
            .setContentText(message)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        notificationManager.notify(claimId.hashCode(), notification)
    }

    fun updateProgress(claimId: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_falco)
            .setContentTitle("Verifying claim")
            .setContentText(message)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        notificationManager.notify(claimId.hashCode(), notification)
    }

    fun showVerdictReady(claimId: String, verdict: Verdict) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_CLAIM_ID, claimId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            claimId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_falco)
            .setContentTitle("Verdict Ready")
            .setContentText("${verdict.lean.name}: ${verdict.summary.take(50)}...")
            .setStyle(NotificationCompat.BigTextStyle().bigText(verdict.summary))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(claimId.hashCode(), notification)
    }

    fun showError(claimId: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_falco)
            .setContentTitle("Verification Failed")
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(claimId.hashCode(), notification)
    }

    fun cancelNotification(claimId: String) {
        notificationManager.cancel(claimId.hashCode())
    }

    companion object {
        const val CHANNEL_ID = "falco_verifications"
        const val EXTRA_CLAIM_ID = "extra_claim_id"
    }
}
