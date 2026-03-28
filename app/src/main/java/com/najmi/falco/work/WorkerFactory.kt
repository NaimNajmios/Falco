package com.najmi.falco.work

import android.content.Context
import androidx.work.WorkerParameters
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@AssistedFactory
interface WorkerFactory {
    fun create(
        context: Context,
        workerParams: WorkerParameters
    ): VerificationWorker
}
