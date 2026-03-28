package com.najmi.falco.domain.usecase

import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.pipeline.FalcoOrchestrator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerifyClaimUseCase @Inject constructor(
    private val orchestrator: FalcoOrchestrator
) {
    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState.asStateFlow()

    fun execute(claimText: String): Flow<VerificationState> =
        orchestrator.verify(claimText).onEach { state ->
            _verificationState.value = state
        }

    fun reset() {
        _verificationState.value = VerificationState.Idle
    }
}
