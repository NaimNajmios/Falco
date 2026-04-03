package com.najmi.falco.ui.hypothesis

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.falco.data.local.DebugLogBuffer
import com.najmi.falco.data.local.DebugLogEntry
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.domain.repository.IVerdictRepository
import com.najmi.falco.domain.repository.RecentClaim
import com.najmi.falco.domain.usecase.VerifyClaimUseCase
import com.najmi.falco.service.VerificationForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val verdictRepository: IVerdictRepository,
    @ApplicationContext private val context: Context
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
    private var serviceIntent: Intent? = null

    val recentClaims: StateFlow<List<RecentClaim>> = verdictRepository.getRecentClaims()
        .let { flow ->
            val stateFlow = MutableStateFlow<List<RecentClaim>>(emptyList())
            viewModelScope.launch {
                flow.collect { stateFlow.value = it }
            }
            stateFlow.asStateFlow()
        }
    
    val debugLogEntries: StateFlow<List<DebugLogEntry>> = DebugLogBuffer.entries
    val stageTimings: StateFlow<List<Pair<String, Long>>> = DebugLogBuffer.stageTimings

    fun onTextChanged(text: String) {
        _claimText.value = text
    }

    fun setClaimText(text: String) {
        _claimText.value = text
    }

    fun verify(text: String) {
        val claimId = UUID.randomUUID().toString().take(8).uppercase()
        _currentClaimId.value = claimId
        _errorMessage.value = null
        
        serviceIntent = Intent(context, VerificationForegroundService::class.java).apply {
            putExtra(VerificationForegroundService.EXTRA_CLAIM_TEXT, text)
            putExtra(VerificationForegroundService.EXTRA_CLAIM_ID, claimId)
        }
        context.startForegroundService(serviceIntent)
        
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
        
        serviceIntent?.let { intent ->
            context.stopService(intent)
            serviceIntent = null
        }
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
