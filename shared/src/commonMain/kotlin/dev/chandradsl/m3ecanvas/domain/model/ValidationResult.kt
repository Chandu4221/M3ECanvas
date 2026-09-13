// ValidationResult.kt
package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the result of a validation operation, such as checking slot constraints
 * or responsive rule definitions.
 */
@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val messages: List<String> = emptyList()
)
