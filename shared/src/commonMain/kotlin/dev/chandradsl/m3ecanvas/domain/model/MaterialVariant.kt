package dev.chandradsl.m3ecanvas.domain.model

/**
 * Represents the visual variant of a Material 3 component.
 *
 * Each component family has its own set of variants, modeled as a nested enum.
 * All implement this shared interface so they can be stored in
 * [ComponentProperty.Variant] while remaining strongly typed.
 */
sealed interface MaterialVariant {

    /** The human-readable label shown in the properties panel. */
    val displayName: String

    /** Variants for Button. */
    enum class Button(override val displayName: String) : MaterialVariant {
        FILLED(displayName = "Filled"),
        TONAL(displayName = "Tonal"),
        OUTLINED(displayName = "Outlined"),
        ELEVATED(displayName = "Elevated"),
        TEXT(displayName = "Text")
    }

    /** Variants for Card. */
    enum class Card(override val displayName: String) : MaterialVariant {
        FILLED(displayName = "Filled"),
        ELEVATED(displayName = "Elevated"),
        OUTLINED(displayName = "Outlined")
    }

    /** Variants for Floating Action Button. */
    enum class FloatingActionButton(override val displayName: String) : MaterialVariant {
        SURFACE(displayName = "Surface"),
        SECONDARY(displayName = "Secondary"),
        TERTIARY(displayName = "Tertiary")
    }

    /** Variants for Icon Button. */
    enum class IconButton(override val displayName: String) : MaterialVariant {
        STANDARD(displayName = "Standard"),
        FILLED(displayName = "Filled"),
        TONAL(displayName = "Tonal"),
        OUTLINED(displayName = "Outlined")
    }

    /** Variants for Chip. */
    enum class Chip(override val displayName: String) : MaterialVariant {
        ASSIST(displayName = "Assist"),
        FILTER(displayName = "Filter"),
        INPUT(displayName = "Input"),
        SUGGESTION(displayName = "Suggestion")
    }
}