package dev.chandradsl.m3ecanvas.editor.asset

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.chandradsl.m3ecanvas.domain.model.AssetAlignment
import dev.chandradsl.m3ecanvas.domain.model.AssetDimensions
import dev.chandradsl.m3ecanvas.domain.model.AssetReference
import dev.chandradsl.m3ecanvas.domain.model.AssetSourceType
import dev.chandradsl.m3ecanvas.domain.model.ContentScaleType

/**
 * Port interface for managing and querying assets (Section 30).
 */
interface AssetRepository {
    fun getAsset(assetId: String): AssetReference?
    fun listAssets(): List<AssetReference>
    fun registerAsset(asset: AssetReference)
    fun removeAsset(assetId: String)
}

/**
 * In-memory and project-backed asset repository with built-in bundled sample assets.
 */
open class DefaultAssetRepository(
    initialAssets: List<AssetReference> = BUNDLED_SAMPLE_ASSETS
) : AssetRepository {

    private val assetMap = initialAssets.associateBy { it.assetId }.toMutableMap()

    override fun getAsset(assetId: String): AssetReference? {
        return assetMap[assetId]
    }

    override fun listAssets(): List<AssetReference> {
        return assetMap.values.toList()
    }

    override fun registerAsset(asset: AssetReference) {
        assetMap[asset.assetId] = asset
    }

    override fun removeAsset(assetId: String) {
        assetMap.remove(assetId)
    }

    companion object {
        val BUNDLED_SAMPLE_ASSETS = listOf(
            AssetReference(
                assetId = "sample_hero_landscape",
                source = AssetSourceType.BUNDLED,
                mimeType = "image/png",
                dimensions = AssetDimensions(width = 1200f, height = 600f),
                scale = ContentScaleType.CROP,
                alignment = AssetAlignment.CENTER,
                metadata = mapOf("title" to "Mountain Sunset Hero", "category" to "Landscape")
            ),
            AssetReference(
                assetId = "sample_avatar_user",
                source = AssetSourceType.BUNDLED,
                mimeType = "image/png",
                dimensions = AssetDimensions(width = 400f, height = 400f),
                scale = ContentScaleType.FIT,
                alignment = AssetAlignment.CENTER,
                metadata = mapOf("title" to "User Portrait Avatar", "category" to "People")
            ),
            AssetReference(
                assetId = "sample_product_card",
                source = AssetSourceType.BUNDLED,
                mimeType = "image/png",
                dimensions = AssetDimensions(width = 800f, height = 600f),
                scale = ContentScaleType.CROP,
                alignment = AssetAlignment.CENTER,
                metadata = mapOf("title" to "Abstract Tech Architecture", "category" to "Technology")
            ),
            AssetReference(
                assetId = "sample_abstract_gradient",
                source = AssetSourceType.BUNDLED,
                mimeType = "image/png",
                dimensions = AssetDimensions(width = 1000f, height = 1000f),
                scale = ContentScaleType.FILL_BOUNDS,
                alignment = AssetAlignment.CENTER,
                metadata = mapOf("title" to "Vibrant Aurora Gradient", "category" to "Abstract")
            )
        )

        val default: DefaultAssetRepository by lazy { DefaultAssetRepository() }
    }
}

/**
 * Draws rich vector artwork for bundled sample assets without relying on placeholder Surfaces.
 * Fulfills Section 30: "Do not represent images with placeholder Surfaces."
 */
fun DrawScope.drawSampleAsset(assetId: String) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    when (assetId) {
        "sample_avatar_user" -> {
            // Gradient circular background
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF6750A4), Color(0xFF4F378B))
                )
            )
            // Head circle
            val headRadius = (minOf(w, h) * 0.22f)
            val headCenter = Offset(w * 0.5f, h * 0.38f)
            drawCircle(
                color = Color(0xFFFFD8E4),
                radius = headRadius,
                center = headCenter
            )
            // Body shoulders arc
            val bodyPath = Path().apply {
                val bodyTop = h * 0.58f
                moveTo(w * 0.15f, h)
                cubicTo(
                    w * 0.2f, bodyTop,
                    w * 0.8f, bodyTop,
                    w * 0.85f, h
                )
                close()
            }
            drawPath(bodyPath, Color(0xFFEADDFF))
        }

        "sample_product_card" -> {
            // Deep tech gradient background
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                )
            )
            // Grid lines
            val step = minOf(w, h) / 8f
            var x = step
            while (x < w) {
                drawLine(
                    color = Color(0x2238BDF8),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = step
            while (y < h) {
                drawLine(
                    color = Color(0x2238BDF8),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += step
            }
            // Glowing geometric cube/hexagon in center
            val centerX = w * 0.5f
            val centerY = h * 0.5f
            val rad = minOf(w, h) * 0.25f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x8838BDF8), Color(0x0038BDF8)),
                    center = Offset(centerX, centerY),
                    radius = rad * 1.5f
                ),
                radius = rad * 1.5f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = rad * 0.5f,
                center = Offset(centerX, centerY)
            )
        }

        "sample_abstract_gradient" -> {
            // Multi-stop flowing aurora gradient
            drawRect(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFEC4899),
                        Color(0xFF8B5CF6),
                        Color(0xFF3B82F6),
                        Color(0xFF10B981),
                        Color(0xFFF59E0B),
                        Color(0xFFEC4899)
                    ),
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            )
        }

        else -> {
            // Default "sample_hero_landscape" - stylized sunset and mountain ridge
            // Sky gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF311B92),
                        Color(0xFF7B1FA2),
                        Color(0xFFE91E63),
                        Color(0xFFFF9800)
                    )
                )
            )
            // Sun
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFEE58), Color(0xFFFF9800), Color(0x00FF9800)),
                    center = Offset(w * 0.65f, h * 0.45f),
                    radius = minOf(w, h) * 0.35f
                ),
                radius = minOf(w, h) * 0.35f,
                center = Offset(w * 0.65f, h * 0.45f)
            )
            // Back mountain ridge
            val backMountain = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.72f)
                lineTo(w * 0.28f, h * 0.45f)
                lineTo(w * 0.55f, h * 0.68f)
                lineTo(w * 0.85f, h * 0.38f)
                lineTo(w, h * 0.58f)
                lineTo(w, h)
                close()
            }
            drawPath(backMountain, Color(0xBB311B92))

            // Fore mountain ridge
            val foreMountain = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.82f)
                lineTo(w * 0.42f, h * 0.55f)
                lineTo(w * 0.7f, h * 0.78f)
                lineTo(w, h * 0.62f)
                lineTo(w, h)
                close()
            }
            drawPath(foreMountain, Color(0xFF1A0A40))
        }
    }
}
