package com.najmi.falco.ui.hypothesis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.domain.usecase.VerifyClaimUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HypothesisViewModel @Inject constructor(
    private val verifyClaimUseCase: VerifyClaimUseCase
) : ViewModel() {

    val verificationState: StateFlow<VerificationState> = verifyClaimUseCase.verificationState

    private val _claimText = MutableStateFlow("")
    val claimText: String get() = _claimText.value

    private val _currentVerdict = MutableStateFlow<Verdict?>(null)
    val currentVerdict: Verdict? get() = _currentVerdict.value

    fun onTextChanged(text: String) {
        _claimText.value = text
    }

    fun verify(text: String) {
        viewModelScope.launch {
            verifyClaimUseCase.execute(text).collect { state ->
                if (state is VerificationState.Success) {
                    _currentVerdict.value = state.verdict
                }
            }
        }
    }

    fun reset() {
        verifyClaimUseCase.reset()
        _claimText.value = ""
        _currentVerdict.value = null
    }
}
