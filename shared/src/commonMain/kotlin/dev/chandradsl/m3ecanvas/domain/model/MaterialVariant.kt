package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Material 3 variant families for components that ship multiple styles.
 *
 * Serialized as a plain string like "Button.OUTLINED" via
 * [MaterialVariantSerializer], because enum subclasses of a sealed interface
 * cannot use kotlinx JSON polymorphic object encoding.
 */
@Serializable(with = MaterialVariantSerializer::class)
sealed interface MaterialVariant {

    val displayName: String

    enum class Button(override val displayName: String) : MaterialVariant {
        FILLED(displayName = "Filled"),
        TONAL(displayName = "Tonal"),
        OUTLINED(displayName = "Outlined"),
        ELEVATED(displayName = "Elevated"),
        TEXT(displayName = "Text")
    }

    enum class Card(override val displayName: String) : MaterialVariant {
        FILLED(displayName = "Filled"),
        ELEVATED(displayName = "Elevated"),
        OUTLINED(displayName = "Outlined")
    }

    enum class FloatingActionButton(override val displayName: String) : MaterialVariant {
        SURFACE(displayName = "Surface"),
        SECONDARY(displayName = "Secondary"),
        TERTIARY(displayName = "Tertiary")
    }

    enum class IconButton(override val displayName: String) : MaterialVariant {
        STANDARD(displayName = "Standard"),
        FILLED(displayName = "Filled"),
        TONAL(displayName = "Tonal"),
        OUTLINED(displayName = "Outlined")
    }

    enum class Chip(override val displayName: String) : MaterialVariant {
        ASSIST(displayName = "Assist"),
        FILTER(displayName = "Filter"),
        INPUT(displayName = "Input"),
        SUGGESTION(displayName = "Suggestion")
    }

    enum class TextField(override val displayName: String) : MaterialVariant {
        OUTLINED(displayName = "Outlined"),
        FILLED(displayName = "Filled")
    }

    enum class Surface(override val displayName: String) : MaterialVariant {
        ROUNDED(displayName = "Rounded"),
        RECTANGLE(displayName = "Rectangle"),
        CIRCLE(displayName = "Circle")
    }

    enum class ToggleButton(override val displayName: String) : MaterialVariant {
        FILLED(displayName = "Filled"),
        ELEVATED(displayName = "Elevated"),
        TONAL(displayName = "Tonal"),
        OUTLINED(displayName = "Outlined")
    }

    enum class FabSize(override val displayName: String) : MaterialVariant {
        SMALL(displayName = "Small"),
        MEDIUM(displayName = "Medium"),
        LARGE(displayName = "Large")
    }

    enum class Carousel(override val displayName: String) : MaterialVariant {
        MULTI_BROWSE(displayName = "Multi-browse"),
        UNCONTAINED(displayName = "Uncontained"),
        CENTERED_HERO(displayName = "Centered Hero")
    }

    enum class TopAppBar(override val displayName: String) : MaterialVariant {
        SMALL(displayName = "Small"),
        CENTER_ALIGNED(displayName = "Center Aligned"),
        MEDIUM(displayName = "Medium"),
        LARGE(displayName = "Large")
    }
}

/** Stable wire format: "Family.NAME", e.g. "Button.OUTLINED". */
fun MaterialVariant.toSerialString(): String {
    return when (this) {
        is MaterialVariant.Button -> "Button.$name"
        is MaterialVariant.Card -> "Card.$name"
        is MaterialVariant.FloatingActionButton -> "FloatingActionButton.$name"
        is MaterialVariant.IconButton -> "IconButton.$name"
        is MaterialVariant.Chip -> "Chip.$name"
        is MaterialVariant.TextField -> "TextField.$name"
        is MaterialVariant.Surface -> "Surface.$name"
        is MaterialVariant.ToggleButton -> "ToggleButton.$name"
        is MaterialVariant.FabSize -> "FabSize.$name"
        is MaterialVariant.Carousel -> "Carousel.$name"
        is MaterialVariant.TopAppBar -> "TopAppBar.$name"
    }
}

/** Parses the wire format back into a [MaterialVariant]. */
fun materialVariantFromSerialString(value: String): MaterialVariant {
    val parts = value.split('.', limit = 2)
    if (parts.size != 2) {
        throw SerializationException(message = "Unknown MaterialVariant: $value")
    }
    return when (parts[0]) {
        "Button" -> MaterialVariant.Button.valueOf(value = parts[1])
        "Card" -> MaterialVariant.Card.valueOf(value = parts[1])
        "FloatingActionButton" -> MaterialVariant.FloatingActionButton.valueOf(value = parts[1])
        "IconButton" -> MaterialVariant.IconButton.valueOf(value = parts[1])
        "Chip" -> MaterialVariant.Chip.valueOf(value = parts[1])
        "TextField" -> MaterialVariant.TextField.valueOf(value = parts[1])
        "Surface" -> MaterialVariant.Surface.valueOf(value = parts[1])
        "ToggleButton" -> MaterialVariant.ToggleButton.valueOf(value = parts[1])
        "FabSize" -> MaterialVariant.FabSize.valueOf(value = parts[1])
        "Carousel" -> MaterialVariant.Carousel.valueOf(value = parts[1])
        "TopAppBar" -> MaterialVariant.TopAppBar.valueOf(value = parts[1])
        else -> throw SerializationException(message = "Unknown MaterialVariant family: ${parts[0]}")
    }
}

object MaterialVariantSerializer : KSerializer<MaterialVariant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "MaterialVariant",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: MaterialVariant) {
        encoder.encodeString(value = value.toSerialString())
    }

    override fun deserialize(decoder: Decoder): MaterialVariant {
        return materialVariantFromSerialString(value = decoder.decodeString())
    }
}