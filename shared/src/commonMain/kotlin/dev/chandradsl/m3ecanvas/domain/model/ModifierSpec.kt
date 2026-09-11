@file:OptIn(ExperimentalUuidApi::class)

package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Target alignment for the [ModifierSpec.Align] modifier.
 */
@Serializable
enum class AlignTarget(val displayName: String) {
    TOP_START(displayName = "Top Start"),
    TOP_CENTER(displayName = "Top Center"),
    TOP_END(displayName = "Top End"),
    CENTER_START(displayName = "Center Start"),
    CENTER(displayName = "Center"),
    CENTER_END(displayName = "Center End"),
    BOTTOM_START(displayName = "Bottom Start"),
    BOTTOM_CENTER(displayName = "Bottom Center"),
    BOTTOM_END(displayName = "Bottom End"),
    START(displayName = "Start (Horiz)"),
    CENTER_HORIZONTALLY(displayName = "Center Horizontally"),
    END(displayName = "End (Horiz)"),
    TOP(displayName = "Top (Vert)"),
    CENTER_VERTICALLY(displayName = "Center Vertically"),
    BOTTOM(displayName = "Bottom (Vert)")
}

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
        val all: Float = 8f,
        val horizontal: Float = 0f,
        val vertical: Float = 0f
    ) : ModifierSpec

    @Serializable
    data class Background(
        override val id: String = Uuid.random().toString(),
        val colorHex: String = "#FF6200EE",
        val cornerRadius: Float = 0f
    ) : ModifierSpec

    @Serializable
    data class Border(
        override val id: String = Uuid.random().toString(),
        val width: Float = 2f,
        val colorHex: String = "#FF3700B3",
        val cornerRadius: Float = 4f
    ) : ModifierSpec

    @Serializable
    data class Clip(
        override val id: String = Uuid.random().toString(),
        val cornerRadius: Float = 8f
    ) : ModifierSpec

    @Serializable
    data class Shadow(
        override val id: String = Uuid.random().toString(),
        val elevation: Float = 4f,
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
    data class FillMaxSize(
        override val id: String = Uuid.random().toString(),
        val fraction: Float = 1f
    ) : ModifierSpec

    @Serializable
    data class FillMaxWidth(
        override val id: String = Uuid.random().toString()
    ) : ModifierSpec

    @Serializable
    data class FillMaxHeight(
        override val id: String = Uuid.random().toString()
    ) : ModifierSpec

    @Serializable
    data class WrapContentSize(
        override val id: String = Uuid.random().toString()
    ) : ModifierSpec

    @Serializable
    data class Width(
        override val id: String = Uuid.random().toString(),
        val dp: Float = 120f
    ) : ModifierSpec

    @Serializable
    data class Height(
        override val id: String = Uuid.random().toString(),
        val dp: Float = 120f
    ) : ModifierSpec

    @Serializable
    data class Size(
        override val id: String = Uuid.random().toString(),
        val width: Float = 100f,
        val height: Float = 100f
    ) : ModifierSpec

    @Serializable
    data class AspectRatio(
        override val id: String = Uuid.random().toString(),
        val ratio: Float = 1f
    ) : ModifierSpec

    @Serializable
    data class Offset(
        override val id: String = Uuid.random().toString(),
        val x: Float = 0f,
        val y: Float = 0f
    ) : ModifierSpec

    @Serializable
    data class Weight(
        override val id: String = Uuid.random().toString(),
        val weight: Float = 1f,
        val fill: Boolean = true
    ) : ModifierSpec

    @Serializable
    data class Align(
        override val id: String = Uuid.random().toString(),
        val alignment: AlignTarget = AlignTarget.CENTER
    ) : ModifierSpec

    @Serializable
    data class ZIndex(
        override val id: String = Uuid.random().toString(),
        val value: Float = 1f
    ) : ModifierSpec

    @Serializable
    data class Clickable(
        override val id: String = Uuid.random().toString(),
        val enabled: Boolean = true
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
    SHADOW(displayName = "Shadow"),
    ALPHA(displayName = "Alpha"),
    ROTATION(displayName = "Rotation"),
    SCALE(displayName = "Scale"),
    FILL_MAX_SIZE(displayName = "Fill Max Size"),
    FILL_MAX_WIDTH(displayName = "Fill Max Width"),
    FILL_MAX_HEIGHT(displayName = "Fill Max Height"),
    WRAP_CONTENT_SIZE(displayName = "Wrap Content Size"),
    WIDTH(displayName = "Width"),
    HEIGHT(displayName = "Height"),
    SIZE(displayName = "Size"),
    ASPECT_RATIO(displayName = "Aspect Ratio"),
    OFFSET(displayName = "Offset"),
    WEIGHT(displayName = "Weight (Column/Row)"),
    ALIGN(displayName = "Align (Container Child)"),
    Z_INDEX(displayName = "Z-Index"),
    CLICKABLE(displayName = "Clickable")
}

/** Creates a default-valued [ModifierSpec] for this kind. */
fun ModifierKind.create(): ModifierSpec {
    return when (this) {
        ModifierKind.PADDING -> ModifierSpec.Padding()
        ModifierKind.BACKGROUND -> ModifierSpec.Background()
        ModifierKind.BORDER -> ModifierSpec.Border()
        ModifierKind.CLIP -> ModifierSpec.Clip()
        ModifierKind.SHADOW -> ModifierSpec.Shadow()
        ModifierKind.ALPHA -> ModifierSpec.Alpha()
        ModifierKind.ROTATION -> ModifierSpec.Rotation()
        ModifierKind.SCALE -> ModifierSpec.Scale()
        ModifierKind.FILL_MAX_SIZE -> ModifierSpec.FillMaxSize()
        ModifierKind.FILL_MAX_WIDTH -> ModifierSpec.FillMaxWidth()
        ModifierKind.FILL_MAX_HEIGHT -> ModifierSpec.FillMaxHeight()
        ModifierKind.WRAP_CONTENT_SIZE -> ModifierSpec.WrapContentSize()
        ModifierKind.WIDTH -> ModifierSpec.Width()
        ModifierKind.HEIGHT -> ModifierSpec.Height()
        ModifierKind.SIZE -> ModifierSpec.Size()
        ModifierKind.ASPECT_RATIO -> ModifierSpec.AspectRatio()
        ModifierKind.OFFSET -> ModifierSpec.Offset()
        ModifierKind.WEIGHT -> ModifierSpec.Weight()
        ModifierKind.ALIGN -> ModifierSpec.Align()
        ModifierKind.Z_INDEX -> ModifierSpec.ZIndex()
        ModifierKind.CLICKABLE -> ModifierSpec.Clickable()
    }
}

/** Human-readable row label including the key parameter values. */
fun ModifierSpec.label(): String {
    return when (this) {
        is ModifierSpec.Padding -> if (horizontal > 0f || vertical > 0f) "Padding h:${horizontal.toInt()} v:${vertical.toInt()} dp" else "Padding ${all.toInt()} dp"
        is ModifierSpec.Background -> if (cornerRadius > 0f) "Background $colorHex (${cornerRadius.toInt()}dp)" else "Background $colorHex"
        is ModifierSpec.Border -> "Border ${width.toInt()} dp ($colorHex)"
        is ModifierSpec.Clip -> "Clip ${cornerRadius.toInt()} dp"
        is ModifierSpec.Shadow -> "Shadow ${elevation.toInt()} dp"
        is ModifierSpec.Alpha -> "Alpha $value"
        is ModifierSpec.Rotation -> "Rotate ${degrees.toInt()} deg"
        is ModifierSpec.Scale -> "Scale $value"
        is ModifierSpec.FillMaxSize -> "Fill Max Size"
        is ModifierSpec.FillMaxWidth -> "Fill Max Width"
        is ModifierSpec.FillMaxHeight -> "Fill Max Height"
        is ModifierSpec.WrapContentSize -> "Wrap Content Size"
        is ModifierSpec.Width -> "Width ${dp.toInt()} dp"
        is ModifierSpec.Height -> "Height ${dp.toInt()} dp"
        is ModifierSpec.Size -> "Size ${width.toInt()}×${height.toInt()} dp"
        is ModifierSpec.AspectRatio -> "Aspect Ratio $ratio"
        is ModifierSpec.Offset -> "Offset (${x.toInt()}, ${y.toInt()}) dp"
        is ModifierSpec.Weight -> "Weight $weight"
        is ModifierSpec.Align -> "Align ${alignment.displayName}"
        is ModifierSpec.ZIndex -> "Z-Index $value"
        is ModifierSpec.Clickable -> "Clickable"
    }
}

/** Creates a copy of this [ModifierSpec] with a newly generated UUID. */
fun ModifierSpec.withNewId(): ModifierSpec = when (this) {
    is ModifierSpec.Padding -> copy(id = Uuid.random().toString())
    is ModifierSpec.Background -> copy(id = Uuid.random().toString())
    is ModifierSpec.Border -> copy(id = Uuid.random().toString())
    is ModifierSpec.Clip -> copy(id = Uuid.random().toString())
    is ModifierSpec.Shadow -> copy(id = Uuid.random().toString())
    is ModifierSpec.Alpha -> copy(id = Uuid.random().toString())
    is ModifierSpec.Rotation -> copy(id = Uuid.random().toString())
    is ModifierSpec.Scale -> copy(id = Uuid.random().toString())
    is ModifierSpec.FillMaxSize -> copy(id = Uuid.random().toString())
    is ModifierSpec.FillMaxWidth -> copy(id = Uuid.random().toString())
    is ModifierSpec.FillMaxHeight -> copy(id = Uuid.random().toString())
    is ModifierSpec.WrapContentSize -> copy(id = Uuid.random().toString())
    is ModifierSpec.Width -> copy(id = Uuid.random().toString())
    is ModifierSpec.Height -> copy(id = Uuid.random().toString())
    is ModifierSpec.Size -> copy(id = Uuid.random().toString())
    is ModifierSpec.AspectRatio -> copy(id = Uuid.random().toString())
    is ModifierSpec.Offset -> copy(id = Uuid.random().toString())
    is ModifierSpec.Weight -> copy(id = Uuid.random().toString())
    is ModifierSpec.Align -> copy(id = Uuid.random().toString())
    is ModifierSpec.ZIndex -> copy(id = Uuid.random().toString())
    is ModifierSpec.Clickable -> copy(id = Uuid.random().toString())
}