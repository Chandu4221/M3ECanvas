@file:OptIn(ExperimentalUuidApi::class)

package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A single entry in a node's modifier chain.
 *
 * Modifiers are stored as data so they can be edited, reordered, serialized,
 * and later emitted as generated code. The order of this list in
 * [CanvasNode.modifiers] equals the order of the Compose modifier chain,
 * which is semantically significant.
 */
@Serializable
sealed interface ModifierSpec {

    /** Stable identity so rows can be reordered and deleted safely. */
    val id: String

    @Serializable
    data class Padding(
        override val id: String = Uuid.random().toString(),
        val all: Float = 8f
    ) : ModifierSpec

    @Serializable
    data class Background(
        override val id: String = Uuid.random().toString(),
        val colorHex: String = "#FF6200EE"
    ) : ModifierSpec

    @Serializable
    data class Border(
        override val id: String = Uuid.random().toString(),
        val width: Float = 2f,
        val colorHex: String = "#FF3700B3"
    ) : ModifierSpec

    @Serializable
    data class Clip(
        override val id: String = Uuid.random().toString(),
        val cornerRadius: Float = 8f
    ) : ModifierSpec

    @Serializable
    data class Alpha(
        override val id: String = Uuid.random().toString(),
        val value: Float = 0.5f
    ) : ModifierSpec

    @Serializable
    data class Rotation(
        override val id: String = Uuid.random().toString(),
        val degrees: Float = 45f
    ) : ModifierSpec

    @Serializable
    data class Scale(
        override val id: String = Uuid.random().toString(),
        val value: Float = 1.2f
    ) : ModifierSpec

    @Serializable
    data class FillMaxWidth(
        override val id: String = Uuid.random().toString()
    ) : ModifierSpec

    @Serializable
    data class FillMaxHeight(
        override val id: String = Uuid.random().toString()
    ) : ModifierSpec
}

/**
 * Catalog of modifier kinds the inspector can add. Kept separate from
 * [ModifierSpec] so the panel can list options without instances.
 */
enum class ModifierKind(val displayName: String) {
    PADDING(displayName = "Padding"),
    BACKGROUND(displayName = "Background"),
    BORDER(displayName = "Border"),
    CLIP(displayName = "Clip"),
    ALPHA(displayName = "Alpha"),
    ROTATION(displayName = "Rotation"),
    SCALE(displayName = "Scale"),
    FILL_MAX_WIDTH(displayName = "Fill Max Width"),
    FILL_MAX_HEIGHT(displayName = "Fill Max Height")
}

/** Creates a default-valued [ModifierSpec] for this kind. */
fun ModifierKind.create(): ModifierSpec {
    return when (this) {
        ModifierKind.PADDING -> ModifierSpec.Padding()
        ModifierKind.BACKGROUND -> ModifierSpec.Background()
        ModifierKind.BORDER -> ModifierSpec.Border()
        ModifierKind.CLIP -> ModifierSpec.Clip()
        ModifierKind.ALPHA -> ModifierSpec.Alpha()
        ModifierKind.ROTATION -> ModifierSpec.Rotation()
        ModifierKind.SCALE -> ModifierSpec.Scale()
        ModifierKind.FILL_MAX_WIDTH -> ModifierSpec.FillMaxWidth()
        ModifierKind.FILL_MAX_HEIGHT -> ModifierSpec.FillMaxHeight()
    }
}

/** Human-readable row label including the key parameter values. */
fun ModifierSpec.label(): String {
    return when (this) {
        is ModifierSpec.Padding -> "Padding ${all.toInt()} dp"
        is ModifierSpec.Background -> "Background $colorHex"
        is ModifierSpec.Border -> "Border ${width.toInt()} dp"
        is ModifierSpec.Clip -> "Clip ${cornerRadius.toInt()} dp"
        is ModifierSpec.Alpha -> "Alpha $value"
        is ModifierSpec.Rotation -> "Rotate ${degrees.toInt()} deg"
        is ModifierSpec.Scale -> "Scale $value"
        is ModifierSpec.FillMaxWidth -> "Fill Max Width"
        is ModifierSpec.FillMaxHeight -> "Fill Max Height"
    }
}