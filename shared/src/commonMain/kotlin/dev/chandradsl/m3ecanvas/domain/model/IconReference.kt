package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Origin source of an icon reference.
 */
@Serializable
enum class IconSource {
    MATERIAL,
    CUSTOM_VECTOR,
    BUNDLED,
    REMOTE
}

/**
 * Design family the icon belongs to.
 */
@Serializable
enum class IconFamily {
    MATERIAL_DESIGN,
    MATERIAL_SYMBOLS,
    CUSTOM
}

/**
 * Visual style variant of an icon.
 */
@Serializable
enum class IconStyle {
    OUTLINED,
    FILLED,
    ROUNDED,
    SHARP,
    TWO_TONE
}

/**
 * Strongly typed icon reference adhering to Section 31 of the specification:
 *
 * IconReference
 *  ├── source
 *  ├── family
 *  ├── name
 *  ├── style
 *  └── metadata
 *
 * The renderer and code generator resolve this reference rather than hardcoding
 * raw Compose symbol strings in documents.
 */
@Serializable
data class IconReference(
    val name: String,
    val source: IconSource = IconSource.MATERIAL,
    val family: IconFamily = IconFamily.MATERIAL_DESIGN,
    val style: IconStyle = IconStyle.OUTLINED,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun material(
            name: String,
            style: IconStyle = IconStyle.OUTLINED,
            metadata: Map<String, String> = emptyMap()
        ): IconReference {
            return IconReference(
                name = name,
                source = IconSource.MATERIAL,
                family = IconFamily.MATERIAL_DESIGN,
                style = style,
                metadata = metadata
            )
        }
    }
}
