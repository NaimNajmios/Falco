package com.najmi.falco.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.najmi.falco.MainActivity
import com.najmi.falco.R
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.model.VerificationStage
import com.najmi.falco.pipeline.FalcoOrchestrator
import com.najmi.falco.work.FalcoNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VerificationForegroundService : Service() {

    @Inject
    lateinit var orchestrator: FalcoOrchestrator

    @Inject
    lateinit var notificationManager: FalcoNotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var verificationJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val claimText = intent?.getStringExtra(EXTRA_CLAIM_TEXT)
        val claimId = intent?.getStringExtra(EXTRA_CLAIM_ID)

        if (claimText == null || claimId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createInitialNotification())
        
        runVerification(claimId, claimText)

        return START_STICKY
    }

    private fun runVerification(claimId: String, claimText: String) {
        verificationJob = serviceScope.launch {
            orchestrator.verify(claimText).collectLatest { state ->
                when (state) {
                    is VerificationState.InProgress -> {
                        notificationManager.showProgress(
                            claimId = claimId,
                            stage = state.stage,
                            processedCount = state.processedCount,
                            totalCount = state.totalCount,
                            message = state.message
                        )
                    }
                    is VerificationState.Success -> {
                        notificationManager.showVerdictReady(claimId, state.verdict)
                        stopSelf()
                    }
                    is VerificationState.Error -> {
                        notificationManager.showError(claimId, state.message)
                        stopSelf()
                    }
                    is VerificationState.Idle -> { /* Ignore */ }
                }
            }
        }
    }

    private fun createInitialNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_falco)
            .setContentTitle("Starting verification...")
            .setContentText("Preparing to verify your claim")
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        verificationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "falco_verifications"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_CLAIM_TEXT = "extra_claim_text"
        const val EXTRA_CLAIM_ID = "extra_claim_id"
    }
}
