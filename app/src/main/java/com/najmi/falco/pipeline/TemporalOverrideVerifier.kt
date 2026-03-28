package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.ClaimType
import com.najmi.falco.domain.model.FreshnessFlag
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.Verdict
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TemporalOverrideResult(
    val shouldOverride: Boolean,
    val overrideReason: String,
    val overrideVerdict: Stance?,
    val confidencePenalty: Float,
    val temporalWarning: String?
)

@Singleton
class TemporalOverrideVerifier @Inject constructor() {

    companion object {
        private const val DAYS_180 = 180L
        private const val DAYS_365 = 365L
        private const val THRESHOLD_OLD_SOURCES = 0.6f
        private const val THRESHOLD_VERY_OLD_SOURCES = 0.5f
    }

    fun evaluate(
        verdict: Verdict,
        claimType: ClaimType,
        paperFreshness: List<FreshnessFlag>
    ): TemporalOverrideResult {
        val now = LocalDate.now()
        
        val freshPapers = paperFreshness.count { it == FreshnessFlag.FRESH }
        val stalePapers = paperFreshness.count { it == FreshnessFlag.STALE }
        val veryOldPapers = paperFreshness.count { it == FreshnessFlag.VERY_OLD }
        val totalPapers = paperFreshness.size

        if (totalPapers == 0) {
            return TemporalOverrideResult(
                shouldOverride = false,
                overrideReason = "No papers to evaluate",
                overrideVerdict = null,
                confidencePenalty = 0f,
                temporalWarning = null
            )
        }

        val oldSourceRatio = (stalePapers + veryOldPapers).toFloat() / totalPapers
        val veryOldRatio = veryOldPapers.toFloat() / totalPapers

        val isCurrentEventClaim = claimType == ClaimType.CURRENT_EVENT || 
            claimType == ClaimType.STATISTICAL

        var shouldOverride = false
        var overrideReason = ""
        var confidencePenalty = 0f
        var temporalWarning: String? = null

        if (isCurrentEventClaim && oldSourceRatio >= THRESHOLD_OLD_SOURCES) {
            shouldOverride = true
            overrideReason = "Current event claim supported by outdated sources (>60% older than 180 days)"
            confidencePenalty = 0.25f
            
            temporalWarning = if (veryOldRatio >= THRESHOLD_VERY_OLD_SOURCES) {
                "⚠️ MISLEADING: This claim implies a current event but all/most sources are over 1 year old (zombie hoax)"
            } else {
                "⚠️ CAUTION: This claim references current information but relies heavily on outdated sources"
            }
        }

        if (isCurrentEventClaim && veryOldRatio >= THRESHOLD_VERY_OLD_SOURCES) {
            shouldOverride = true
            overrideReason = "Claim implies current event but >50% sources are >365 days old"
            confidencePenalty = 0.40f
            
            temporalWarning = "🚨 MISLEADING: This claim suggests current information but all sources are over 1 year old. This is a 'zombie hoax' - old information being presented as current."
        }

        val hasMixedTemporalSignals = freshPapers > 0 && veryOldPapers > 0
        if (hasMixedTemporalSignals && oldSourceRatio >= 0.4f) {
            confidencePenalty += 0.15f
            temporalWarning = if (temporalWarning != null) {
                "$temporalWarning Additionally, evidence has mixed freshness - some recent, some outdated."
            } else {
                "⚠️ CAUTION: Evidence has mixed temporal signals - some recent sources but also outdated ones."
            }
        }

        return TemporalOverrideResult(
            shouldOverride = shouldOverride,
            overrideReason = overrideReason,
            overrideVerdict = if (shouldOverride) Stance.OPPOSES else null,
            confidencePenalty = confidencePenalty.coerceIn(0f, 0.5f),
            temporalWarning = temporalWarning
        )
    }

    fun analyzePaperTemporalProfile(
        paperDate: LocalDate?,
        claimType: ClaimType
    ): FreshnessFlag {
        if (paperDate == null) return FreshnessFlag.UNKNOWN

        val daysOld = ChronoUnit.DAYS.between(paperDate, LocalDate.now())

        return when {
            daysOld <= 90 -> FreshnessFlag.FRESH
            daysOld <= DAYS_180 -> FreshnessFlag.RECENT
            daysOld <= DAYS_365 -> FreshnessFlag.STALE
            else -> FreshnessFlag.VERY_OLD
        }
    }
}
