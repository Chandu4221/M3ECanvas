package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Origin of an asset in the project asset system (Section 30).
 */
@Serializable
enum class AssetSourceType {
    LOCAL,
    REMOTE,
    BUNDLED,
    VECTOR,
    FONT
}

/**
 * Content scaling mode for rendering images.
 */
@Serializable
enum class ContentScaleType {
    FIT,
    CROP,
    FILL_BOUNDS,
    INSIDE,
    FILL_WIDTH,
    FILL_HEIGHT,
    NONE
}

/**
 * Alignment for positioning content within an asset's bounding box.
 */
@Serializable
enum class AssetAlignment {
    TOP_START,
    TOP_CENTER,
    TOP_END,
    CENTER_START,
    CENTER,
    CENTER_END,
    BOTTOM_START,
    BOTTOM_CENTER,
    BOTTOM_END
}

typealias AssetAlignmentType = AssetAlignment

/**
 * Geometric dimensions of an asset in pixels or points.
 */
@Serializable
data class AssetDimensions(
    val width: Float,
    val height: Float
)

/**
 * Normalized or absolute crop rectangle (left, top, right, bottom).
 */
@Serializable
data class AssetCrop(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
)

/**
 * Strongly-typed asset reference fulfilling Section 30 of the specification:
 *
 * asset ID
 * source
 * mime type
 * dimensions
 * metadata
 * crop
 * scale
 * alignment
 */
@Serializable
data class AssetReference(
    val assetId: String,
    val source: AssetSourceType = AssetSourceType.BUNDLED,
    val uri: String = "",
    val mimeType: String = "image/png",
    val dimensions: AssetDimensions? = null,
    val metadata: Map<String, String> = emptyMap(),
    val crop: AssetCrop? = null,
    val scale: ContentScaleType = ContentScaleType.FIT,
    val alignment: AssetAlignment = AssetAlignment.CENTER
)
