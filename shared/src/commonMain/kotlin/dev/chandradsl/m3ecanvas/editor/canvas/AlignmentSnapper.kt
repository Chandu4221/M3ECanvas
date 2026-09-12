package dev.chandradsl.m3ecanvas.editor.canvas

import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.editor.state.AlignmentGuide
import dev.chandradsl.m3ecanvas.editor.state.GuideOrientation
import kotlin.math.abs

/**
 * Result of computing alignment snapping for a node being moved.
 */
data class SnapResult(
    val snappedPosition: CanvasPosition,
    val guides: List<AlignmentGuide>
)

/**
 * Computes magnetic alignment snapping and visual guides for nodes being dragged on the canvas.
 * Snaps to sibling node edges/centers, canvas screen center, and standard margins.
 */
class AlignmentSnapper(
    val snapThreshold: Float = 6f
) {

    fun computeSnap(
        node: CanvasNode,
        candidatePos: CanvasPosition,
        otherNodes: List<CanvasNode>,
        deviceWidth: Float,
        deviceHeight: Float,
        enabled: Boolean = true
    ): SnapResult {
        if (!enabled) {
            return SnapResult(snappedPosition = candidatePos, guides = emptyList())
        }

        var snappedX = candidatePos.x
        var snappedY = candidatePos.y
        val guides = mutableListOf<AlignmentGuide>()

        var bestDiffX = snapThreshold + 1f
        var bestDiffY = snapThreshold + 1f

        val nodeWidth = node.size.width
        val nodeHeight = node.size.height
        val nodeCenterX = candidatePos.x + nodeWidth / 2f
        val nodeCenterY = candidatePos.y + nodeHeight / 2f

        // 1. Snap to Canvas Center
        val screenCenterX = deviceWidth / 2f
        val diffScreenCenterX = abs(nodeCenterX - screenCenterX)
        if (diffScreenCenterX <= snapThreshold && diffScreenCenterX < bestDiffX) {
            bestDiffX = diffScreenCenterX
            snappedX = screenCenterX - nodeWidth / 2f
            guides.removeAll { it.orientation == GuideOrientation.VERTICAL }
            guides.add(AlignmentGuide(GuideOrientation.VERTICAL, screenCenterX, "Center"))
        }

        val screenCenterY = deviceHeight / 2f
        val diffScreenCenterY = abs(nodeCenterY - screenCenterY)
        if (diffScreenCenterY <= snapThreshold && diffScreenCenterY < bestDiffY) {
            bestDiffY = diffScreenCenterY
            snappedY = screenCenterY - nodeHeight / 2f
            guides.removeAll { it.orientation == GuideOrientation.HORIZONTAL }
            guides.add(AlignmentGuide(GuideOrientation.HORIZONTAL, screenCenterY, "Center"))
        }

        // 2. Snap to Canvas Margins (16dp standard Material margin)
        val leftMargin = 16f
        val diffLeftMargin = abs(candidatePos.x - leftMargin)
        if (diffLeftMargin <= snapThreshold && diffLeftMargin < bestDiffX) {
            bestDiffX = diffLeftMargin
            snappedX = leftMargin
            guides.removeAll { it.orientation == GuideOrientation.VERTICAL }
            guides.add(AlignmentGuide(GuideOrientation.VERTICAL, leftMargin, "Margin"))
        }

        val rightMargin = deviceWidth - 16f - nodeWidth
        val diffRightMargin = abs(candidatePos.x - rightMargin)
        if (diffRightMargin <= snapThreshold && diffRightMargin < bestDiffX) {
            bestDiffX = diffRightMargin
            snappedX = rightMargin
            guides.removeAll { it.orientation == GuideOrientation.VERTICAL }
            guides.add(AlignmentGuide(GuideOrientation.VERTICAL, deviceWidth - 16f, "Margin"))
        }

        // 3. Snap against other nodes
        for (other in otherNodes) {
            val otherX = other.position.x
            val otherY = other.position.y
            val otherWidth = other.size.width
            val otherHeight = other.size.height
            val otherCenterX = otherX + otherWidth / 2f
            val otherCenterY = otherY + otherHeight / 2f

            // Horizontal snaps (affecting X coordinate)
            // Left to Left
            val diffL2L = abs(candidatePos.x - otherX)
            if (diffL2L <= snapThreshold && diffL2L < bestDiffX) {
                bestDiffX = diffL2L
                snappedX = otherX
                guides.removeAll { it.orientation == GuideOrientation.VERTICAL }
                guides.add(AlignmentGuide(GuideOrientation.VERTICAL, otherX))
            }
            // Left to Right
            val diffL2R = abs(candidatePos.x - (otherX + otherWidth))
            if (diffL2R <= snapThreshold && diffL2R < bestDiffX) {
                bestDiffX = diffL2R
                snappedX = otherX + otherWidth
                guides.removeAll { it.orientation == GuideOrientation.VERTICAL }
                guides.add(AlignmentGuide(GuideOrientation.VERTICAL, otherX + otherWidth))
            }
            // Right to Left
            val diffR2L = abs((candidatePos.x + nodeWidth) - otherX)
            if (diffR2L <= snapThreshold && diffR2L < bestDiffX) {
                bestDiffX = diffR2L
                snappedX = otherX - nodeWidth
                guides.removeAll { it.orientation == GuideOrientation.VERTICAL }
                guides.add(AlignmentGuide(GuideOrientation.VERTICAL, otherX))
            }
            // Right to Right
            val diffR2R = abs((candidatePos.x + nodeWidth) - (otherX + otherWidth))
            if (diffR2R <= snapThreshold && diffR2R < bestDiffX) {
                bestDiffX = diffR2R
                snappedX = otherX + otherWidth - nodeWidth
                guides.removeAll { it.orientation == GuideOrientation.VERTICAL }
                guides.add(AlignmentGuide(GuideOrientation.VERTICAL, otherX + otherWidth))
            }
            // Center to Center X
            val diffC2C_X = abs(nodeCenterX - otherCenterX)
            if (diffC2C_X <= snapThreshold && diffC2C_X < bestDiffX) {
                bestDiffX = diffC2C_X
                snappedX = otherCenterX - nodeWidth / 2f
                guides.removeAll { it.orientation == GuideOrientation.VERTICAL }
                guides.add(AlignmentGuide(GuideOrientation.VERTICAL, otherCenterX))
            }

            // Vertical snaps (affecting Y coordinate)
            // Top to Top
            val diffT2T = abs(candidatePos.y - otherY)
            if (diffT2T <= snapThreshold && diffT2T < bestDiffY) {
                bestDiffY = diffT2T
                snappedY = otherY
                guides.removeAll { it.orientation == GuideOrientation.HORIZONTAL }
                guides.add(AlignmentGuide(GuideOrientation.HORIZONTAL, otherY))
            }
            // Top to Bottom
            val diffT2B = abs(candidatePos.y - (otherY + otherHeight))
            if (diffT2B <= snapThreshold && diffT2B < bestDiffY) {
                bestDiffY = diffT2B
                snappedY = otherY + otherHeight
                guides.removeAll { it.orientation == GuideOrientation.HORIZONTAL }
                guides.add(AlignmentGuide(GuideOrientation.HORIZONTAL, otherY + otherHeight))
            }
            // Bottom to Top
            val diffB2T = abs((candidatePos.y + nodeHeight) - otherY)
            if (diffB2T <= snapThreshold && diffB2T < bestDiffY) {
                bestDiffY = diffB2T
                snappedY = otherY - nodeHeight
                guides.removeAll { it.orientation == GuideOrientation.HORIZONTAL }
                guides.add(AlignmentGuide(GuideOrientation.HORIZONTAL, otherY))
            }
            // Bottom to Bottom
            val diffB2B = abs((candidatePos.y + nodeHeight) - (otherY + otherHeight))
            if (diffB2B <= snapThreshold && diffB2B < bestDiffY) {
                bestDiffY = diffB2B
                snappedY = otherY + otherHeight - nodeHeight
                guides.removeAll { it.orientation == GuideOrientation.HORIZONTAL }
                guides.add(AlignmentGuide(GuideOrientation.HORIZONTAL, otherY + otherHeight))
            }
            // Center to Center Y
            val diffC2C_Y = abs(nodeCenterY - otherCenterY)
            if (diffC2C_Y <= snapThreshold && diffC2C_Y < bestDiffY) {
                bestDiffY = diffC2C_Y
                snappedY = otherCenterY - nodeHeight / 2f
                guides.removeAll { it.orientation == GuideOrientation.HORIZONTAL }
                guides.add(AlignmentGuide(GuideOrientation.HORIZONTAL, otherCenterY))
            }
        }

        return SnapResult(
            snappedPosition = CanvasPosition(snappedX, snappedY),
            guides = guides
        )
    }

    companion object {
        val default = AlignmentSnapper()
    }
}
