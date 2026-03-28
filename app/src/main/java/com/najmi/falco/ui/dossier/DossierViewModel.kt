package com.najmi.falco.ui.dossier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.domain.repository.IVerdictRepository
import com.najmi.falco.domain.repository.RecentClaim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DossierUiState(
    val isLoading: Boolean = true,
    val recentClaims: List<RecentClaim> = emptyList(),
    val selectedVerdict: Verdict? = null,
    val isLoadingDetail: Boolean = false
)

@HiltViewModel
class DossierViewModel @Inject constructor(
    private val verdictRepository: IVerdictRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DossierUiState())
    val uiState: StateFlow<DossierUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            verdictRepository.getRecentClaims().collect { claims ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    recentClaims = claims
                )
            }
        }
    }

    fun selectVerdict(claimId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDetail = true)
            val verdict = verdictRepository.getByClaimId(claimId)
            _uiState.value = _uiState.value.copy(
                selectedVerdict = verdict,
                isLoadingDetail = false
            )
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedVerdict = null)
    }

    fun deleteClaim(id: String) {
        viewModelScope.launch {
            verdictRepository.deleteClaim(id)
        }
    }
}
