package com.najmi.falco.ui.pipeline

import androidx.lifecycle.ViewModel
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.usecase.VerifyClaimUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PipelineViewModel @Inject constructor(
    verifyClaimUseCase: VerifyClaimUseCase
) : ViewModel() {
    val verificationState: StateFlow<VerificationState> = verifyClaimUseCase.verificationState
}
