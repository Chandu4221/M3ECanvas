package dev.chandradsl.m3ecanvas.domain.model

/**
 * Represents a single editable property of a component.
 *
 * A sealed hierarchy is used so every property is strongly typed.
 * This lets the properties panel, renderer, and code generator handle
 * each kind safely without casting or guessing.
 */
sealed interface ComponentProperty {

    /** The identifier of this property, for example "text" or "enabled". */
    val key: String

    /** A plain text value, such as a button label. */
    data class Text(
        override val key: String,
        val value: String
    ) : ComponentProperty

    /** A true/false value, such as "enabled" or "checked". */
    data class BooleanFlag(
        override val key: String,
        val value: Boolean
    ) : ComponentProperty

    /** A numeric value, such as elevation or corner radius. */
    data class Numeric(
        override val key: String,
        val value: Float
    ) : ComponentProperty

    /** A color stored as a hex string, for example "#FF6200EE". */
    data class ColorHex(
        override val key: String,
        val value: String
    ) : ComponentProperty

    /** A component variant, such as Filled or Outlined. */
    data class Variant(
        override val key: String,
        val value: MaterialVariant
    ) : ComponentProperty

    /** An icon reference by name. */
    data class Icon(
        override val key: String,
        val iconName: String
    ) : ComponentProperty
}