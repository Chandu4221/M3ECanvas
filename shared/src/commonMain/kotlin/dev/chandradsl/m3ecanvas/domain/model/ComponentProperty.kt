package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a single editable property of a component.
 *
 * A sealed hierarchy is used so every property is strongly typed.
 * This lets the properties panel, renderer, and code generator handle
 * each kind safely without casting or guessing.
 */
@Serializable
sealed interface ComponentProperty {

    /** The identifier of this property, for example "text" or "enabled". */
    val key: String

    /** A plain text value, such as a button label. */
    @Serializable
    data class Text(
        override val key: String,
        val value: String
    ) : ComponentProperty

    /** A true/false value, such as "enabled" or "checked". */
    @Serializable
    data class BooleanFlag(
        override val key: String,
        val value: Boolean
    ) : ComponentProperty

    /** A numeric value, such as elevation or corner radius. */
    @Serializable
    data class Numeric(
        override val key: String,
        val value: Float
    ) : ComponentProperty

    /** A color stored as a hex string, for example "#FF6200EE". */
    @Serializable
    data class ColorHex(
        override val key: String,
        val value: String
    ) : ComponentProperty

    /** A component variant, such as Filled or Outlined. */
    @Serializable
    data class Variant(
        override val key: String,
        val value: MaterialVariant
    ) : ComponentProperty

    /** An icon reference adhering to Section 31. */
    @Serializable
    data class Icon(
        override val key: String,
        val iconName: String = "Favorite",
        val iconRef: IconReference = IconReference.material(iconName)
    ) : ComponentProperty {
        constructor(key: String, reference: IconReference) : this(
            key = key,
            iconName = reference.name,
            iconRef = reference
        )
    }

    /** An asset reference adhering to Section 30. */
    @Serializable
    data class Asset(
        override val key: String,
        val assetId: String = "",
        val assetRef: AssetReference = AssetReference(assetId = assetId)
    ) : ComponentProperty {
        constructor(key: String, reference: AssetReference) : this(
            key = key,
            assetId = reference.assetId,
            assetRef = reference
        )
    }
}