package com.najmi.falco.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.pipeline.FalcoOrchestrator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class VerificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val orchestrator: FalcoOrchestrator,
    private val notificationManager: FalcoNotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val claimText = inputData.getString(KEY_CLAIM) ?: return Result.failure()
        val claimId = inputData.getString(KEY_CLAIM_ID) ?: return Result.failure()

        return try {
            orchestrator.verify(claimText).collect { state ->
                when (state) {
                    is VerificationState.InProgress -> {
                        notificationManager.updateProgress(claimId, state.message)
                    }
                    is VerificationState.Success -> {
                        notificationManager.showVerdictReady(claimId, state.verdict)
                    }
                    is VerificationState.Error -> {
                        notificationManager.showError(claimId, state.message)
                    }
                    else -> {}
                }
            }
            Result.success()
        } catch (e: Exception) {
            notificationManager.showError(claimId, e.message ?: "Unknown error")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_CLAIM = "claim"
        const val KEY_CLAIM_ID = "claim_id"
        const val WORK_TAG = "falco_verification"
    }
}
