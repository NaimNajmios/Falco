package com.najmi.falco.ui.hypothesis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.domain.repository.IVerdictRepository
import com.najmi.falco.domain.repository.RecentClaim
import com.najmi.falco.domain.usecase.VerifyClaimUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HypothesisViewModel @Inject constructor(
    private val verifyClaimUseCase: VerifyClaimUseCase,
    private val verdictRepository: IVerdictRepository
) : ViewModel() {

    val verificationState: StateFlow<VerificationState> = verifyClaimUseCase.verificationState

    private val _claimText = MutableStateFlow("")
    val claimText: StateFlow<String> = _claimText.asStateFlow()

    private val _currentClaimId = MutableStateFlow("")
    val currentClaimId: String get() = _currentClaimId.value

    private val _currentVerdict = MutableStateFlow<Verdict?>(null)
    val currentVerdict: Verdict? get() = _currentVerdict.value

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: String? get() = _errorMessage.value

    private var verificationJob: Job? = null

    val recentClaims: StateFlow<List<RecentClaim>> = verdictRepository.getRecentClaims()
        .let { flow ->
            val stateFlow = MutableStateFlow<List<RecentClaim>>(emptyList())
            viewModelScope.launch {
                flow.collect { stateFlow.value = it }
            }
            stateFlow.asStateFlow()
        }

    fun onTextChanged(text: String) {
        _claimText.value = text
    }

    fun verify(text: String) {
        _currentClaimId.value = UUID.randomUUID().toString().take(8).uppercase()
        _errorMessage.value = null
        verificationJob = viewModelScope.launch {
            verifyClaimUseCase.execute(text).collect { state ->
                when (state) {
                    is VerificationState.Success -> {
                        _currentVerdict.value = state.verdict
                        _currentClaimId.value = state.verdict.claimId.take(8).uppercase()
                    }
                    is VerificationState.Error -> {
                        _currentVerdict.value = null
                        _errorMessage.value = state.message
                    }
                    is VerificationState.InProgress -> {
                        _errorMessage.value = null
                    }
                    is VerificationState.Idle -> {}
                }
            }
        }
    }

    fun haltVerification() {
        verificationJob?.cancel()
        verifyClaimUseCase.reset()
        _errorMessage.value = "Verification cancelled by user"
    }

    fun cancelVerification() = haltVerification()

    fun reset() {
        verifyClaimUseCase.reset()
        _claimText.value = ""
        _currentVerdict.value = null
        _errorMessage.value = null
        _currentClaimId.value = ""
    }
}
