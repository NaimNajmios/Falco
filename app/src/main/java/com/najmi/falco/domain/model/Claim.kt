package com.najmi.falco.domain.model

import java.util.UUID

data class Claim(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: ClaimType,
    val submittedAt: Long = System.currentTimeMillis()
)
