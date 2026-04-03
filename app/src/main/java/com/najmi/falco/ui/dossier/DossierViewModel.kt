package com.najmi.falco.ui.dossier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.domain.repository.IVerdictRepository
import com.najmi.falco.domain.repository.RecentClaim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun filterAndSort(claims: List<RecentClaim>, query: String, sortOrder: SortOrder, showFavoritesOnly: Boolean): List<RecentClaim> {
    var filtered = if (query.isBlank()) claims
    else claims.filter { it.text.contains(query, ignoreCase = true) }
    
    if (showFavoritesOnly) {
        filtered = filtered.filter { it.isFavorite }
    }
    
    return when (sortOrder) {
        SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.submittedAt }
        SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.submittedAt }
        SortOrder.HIGHEST_CONFIDENCE -> filtered.sortedByDescending { it.confidence ?: 0f }
    }
}

data class DossierUiState(
    val isLoading: Boolean = true,
    val recentClaims: List<RecentClaim> = emptyList(),
    val filteredClaims: List<RecentClaim> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val showFavoritesOnly: Boolean = false,
    val selectedVerdict: Verdict? = null,
    val isLoadingDetail: Boolean = false
)

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    HIGHEST_CONFIDENCE
}

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
                val state = _uiState.value
                val filtered = filterAndSort(claims, state.searchQuery, state.sortOrder, state.showFavoritesOnly)
                _uiState.value = state.copy(
                    isLoading = false,
                    recentClaims = claims,
                    filteredClaims = filtered
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            searchQuery = query,
            filteredClaims = filterAndSort(state.recentClaims, query, state.sortOrder, state.showFavoritesOnly)
        )
    }

    fun setSortOrder(order: SortOrder) {
        val state = _uiState.value
        _uiState.value = state.copy(
            sortOrder = order,
            filteredClaims = filterAndSort(state.recentClaims, state.searchQuery, order, state.showFavoritesOnly)
        )
    }

    fun setShowFavoritesOnly(showFavoritesOnly: Boolean) {
        val state = _uiState.value
        _uiState.value = state.copy(
            showFavoritesOnly = showFavoritesOnly,
            filteredClaims = filterAndSort(state.recentClaims, state.searchQuery, state.sortOrder, showFavoritesOnly)
        )
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
            lastDeletedClaim = verdictRepository.getByClaimId(id)?.let { v ->
                RecentClaim(
                    id = v.claimId,
                    text = v.claim,
                    type = "EMPIRICAL",
                    submittedAt = v.completedAt,
                    lean = v.lean.name,
                    confidence = v.confidence,
                    supportingCount = v.supportingCount,
                    opposingCount = v.opposingCount,
                    neutralCount = v.neutralCount
                )
            }
            verdictRepository.deleteClaim(id)
        }
    }

    fun undoDelete() {
        lastDeletedClaim?.let { claim ->
            viewModelScope.launch {
                val verdict = verdictRepository.getById(claim.id)
                verdict?.let { verdictRepository.save(it) }
                lastDeletedClaim = null
            }
        }
    }

    fun exportVerdicts(): Flow<List<Verdict>> {
        return verdictRepository.exportAllVerdicts()
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            verdictRepository.toggleFavorite(id)
        }
    }

    private var lastDeletedClaim: RecentClaim? = null
}
