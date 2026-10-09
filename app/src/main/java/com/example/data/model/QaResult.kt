package com.example.data.model

data class QaCheck(
    val title: String,
    val description: String,
    val passed: Boolean
)

data class QaResult(
    val checks: List<QaCheck>,
    val characterNameSuppressed: Boolean,
    val safetyVerified: Boolean,
    val flowReady: Boolean
) {
    val allPassed: Boolean get() = checks.all { it.passed }
    val passedCount: Int get() = checks.count { it.passed }
    val totalCount: Int get() = checks.size
}
