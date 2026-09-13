package dev.chandradsl.m3ecanvas.editor.service

import dev.chandradsl.m3ecanvas.domain.model.*

/**
 * Service responsible for all node tree mutations, layout modifications,
 * and hierarchy invariant validations.
 */
class DocumentEditorService {

    /**
     * Adds a new top-level component of [type] at [position].
     * Returns the updated project and the created node, or null if rejected.
     */
    fun addNode(
        project: M3EProject,
        type: ComponentType,
        position: CanvasPosition,
        defaultSize: CanvasSize
    ): Pair<M3EProject, CanvasNode?> {
        val hasScaffold = project.nodes.any { it.type == ComponentType.SCAFFOLD }
        if (!type.isAllowedAtRoot(hasScaffold)) return Pair(project, null)

        val existingCount = project.nodes.count { it.type == type }
        val name = "${type.displayName} ${existingCount + 1}"
        val targetPosition = if (type == ComponentType.SCAFFOLD || type.isOverlay()) CanvasPosition.Zero else position
        val node = CanvasNode(
            type = type,
            name = name,
            position = targetPosition,
            size = defaultSize
        )

        return Pair(project.withNode(node), node)
    }

    /**
     * Finds the nearest legal container for [forType], starting from [startingFromNodeId]
     * (or the currently selected node) and walking up ancestor containers.
     */
    fun findValidTargetContainer(
        project: M3EProject,
        forType: ComponentType,
        startingFromNodeId: String? = null
    ): CanvasNode? {
        val rootScaffold = project.nodes.firstOrNull { it.type == ComponentType.SCAFFOLD }

        if (rootScaffold != null && forType.canonicalSlot() != SlotRole.CONTENT) {
            return if (rootScaffold.type.canAcceptChild(forType)) rootScaffold else null
        }

        var current = if (startingFromNodeId != null) project.findNode(startingFromNodeId) else null

        if (current != null && current.isContainer && current.type.canAcceptChild(forType)) {
            return current
        }

        var ancestor = if (current != null) findParent(project, current.id) else null
        while (ancestor != null) {
            if (ancestor.isContainer && ancestor.type.canAcceptChild(forType)) {
                return ancestor
            }
            ancestor = findParent(project, ancestor.id)
        }

        if (rootScaffold != null && forType != ComponentType.SCAFFOLD) {
            val contentContainer = rootScaffold.children.firstOrNull {
                it.slot == SlotRole.CONTENT && it.isContainer && it.type.canAcceptChild(forType)
            }
            if (contentContainer != null) return contentContainer
            if (rootScaffold.type.canAcceptChild(forType)) return rootScaffold
        }

        return project.nodes.firstOrNull { it.isContainer && it.type.canAcceptChild(forType) }
    }

    /**
     * Adds a new child of [type] inside the container with [containerId].
     */
    fun addChildToContainer(
        project: M3EProject,
        containerId: String,
        type: ComponentType,
        defaultSize: CanvasSize
    ): Pair<M3EProject, CanvasNode?> {
        val container = project.findNode(containerId) ?: return Pair(project, null)
        if (!container.isContainer) return Pair(project, null)
        if (!container.type.canAcceptChild(type)) return Pair(project, null)

        val childCount = container.children.count { it.type == type }
        val name = "${type.displayName} ${childCount + 1}"

        val supported = container.type.supportedSlots()
        val targetSlot = if (supported.isNotEmpty()) {
            val allowed = type.allowedSlotsIn(container.type)
            val occupiedSlots = container.children.mapNotNull { it.slot }.toSet()
            allowed.firstOrNull { it.isMultiOccupant || it !in occupiedSlots }
                ?: type.canonicalSlotFor(container.type)
        } else {
            null
        }

        val child = CanvasNode(
            type = type,
            name = name,
            position = CanvasPosition.Zero,
            size = defaultSize,
            slot = targetSlot
        )

        val updatedContainer = if (targetSlot != null && !targetSlot.isMultiOccupant) {
            container.withChildInSlot(child = child, slotRole = targetSlot)
        } else {
            container.withChild(child = child)
        }

        return Pair(project.updateNode(updatedContainer), child)
    }

    /**
     * Sets the slot role for [nodeId], replacing single-occupant occupants if necessary.
     */
    fun setNodeSlot(project: M3EProject, nodeId: String, slotRole: SlotRole?): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        if (node.slot == slotRole) return project

        val parent = findParent(project, nodeId)
        return if (parent != null && slotRole != null) {
            if (!node.type.allowedSlotsIn(parent.type).contains(slotRole)) return project
            val cleanedChildren = if (!slotRole.isMultiOccupant) {
                parent.children.filterNot { it.id != nodeId && it.slot == slotRole }
            } else {
                parent.children
            }
            val updatedChildren = cleanedChildren.map {
                if (it.id == nodeId) it.copy(slot = slotRole) else it
            }
            val updatedParent = parent.copy(children = updatedChildren)
            project.updateNode(updatedParent)
        } else {
            project.updateNode(node.copy(slot = slotRole))
        }
    }

    /**
     * Removes the node with [nodeId] from anywhere in the tree.
     */
    fun removeNode(project: M3EProject, nodeId: String): M3EProject {
        return project.removeNode(nodeId)
    }

    /**
     * Deletes all nodes identified by [nodeIds], excluding locked nodes.
     */
    fun deleteNodes(project: M3EProject, nodeIds: Set<String>): M3EProject {
        var current = project
        for (id in nodeIds) {
            val node = current.findNode(id)
            if (node != null && node.isLocked) continue
            current = current.removeNode(id)
        }
        return current
    }

    /**
     * Nudges selected top-level nodes by [dx] and [dy].
     * Nodes locked in slots or with isLocked == true are skipped.
     */
    fun nudgeNodes(project: M3EProject, nodeIds: Set<String>, dx: Float, dy: Float): Pair<M3EProject, Boolean> {
        var current = project
        var movedAny = false
        for (id in nodeIds) {
            val isTopLevel = current.nodes.any { it.id == id }
            if (!isTopLevel) continue
            val node = current.findNode(id) ?: continue
            if (node.isLockedInSlot || node.isLocked) continue
            current = current.updateNode(node.movedBy(dx = dx, dy = dy))
            movedAny = true
        }
        return Pair(current, movedAny)
    }

    /**
     * Toggles visibility of the node identified by [nodeId].
     */
    fun toggleNodeVisibility(project: M3EProject, nodeId: String): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        return project.updateNode(node.withVisibility(!node.isVisible))
    }

    /**
     * Toggles lock state of the node identified by [nodeId].
     */
    fun toggleNodeLock(project: M3EProject, nodeId: String): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        return project.updateNode(node.withLocked(!node.isLocked))
    }

    fun updateNodeProperty(project: M3EProject, nodeId: String, property: ComponentProperty): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        val updatedNode = node.withProperty(property)
        return project.updateNode(updatedNode)
    }

    fun renameNode(project: M3EProject, nodeId: String, name: String): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        return project.updateNode(node.copy(name = name))
    }

    fun updateLayoutConfig(project: M3EProject, nodeId: String, config: LayoutConfig): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        return project.updateNode(node.copy(layoutConfig = config))
    }

    fun addModifier(project: M3EProject, nodeId: String, spec: ModifierSpec): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        return project.updateNode(node.withModifier(spec))
    }

    fun removeModifier(project: M3EProject, nodeId: String, specId: String): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        return project.updateNode(node.withoutModifier(specId))
    }

    fun updateModifier(project: M3EProject, nodeId: String, spec: ModifierSpec): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        return project.updateNode(node.updateModifier(spec))
    }

    fun moveModifier(project: M3EProject, nodeId: String, specId: String, delta: Int): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        val updated = node.moveModifier(specId, delta)
        return if (updated != node) project.updateNode(updated) else project
    }

    fun reorderNode(project: M3EProject, nodeId: String, delta: Int): M3EProject {
        val parent = findParent(project, nodeId)
        return if (parent == null) {
            val nodes = project.nodes
            val index = nodes.indexOfFirst { it.id == nodeId }
            val target = index + delta
            if (index < 0 || target < 0 || target >= nodes.size) return project
            val reordered = nodes.toMutableList().apply {
                val item = removeAt(index)
                add(target, item)
            }
            project.copy(nodes = reordered)
        } else {
            val children = parent.children
            val index = children.indexOfFirst { it.id == nodeId }
            val target = index + delta
            if (index < 0 || target < 0 || target >= children.size) return project
            val reordered = children.toMutableList().apply {
                val item = removeAt(index)
                add(target, item)
            }
            val updatedParent = parent.copy(children = reordered)
            project.updateNode(updatedParent)
        }
    }

    fun findParent(project: M3EProject, nodeId: String): CanvasNode? {
        for (root in project.nodes) {
            val found = findParentInternal(root, nodeId)
            if (found != null) return found
        }
        return null
    }

    private fun findParentInternal(node: CanvasNode, targetId: String): CanvasNode? {
        for (child in node.children) {
            if (child.id == targetId) return node
            val deeper = findParentInternal(child, targetId)
            if (deeper != null) return deeper
        }
        return null
    }

    fun validateHierarchy(project: M3EProject): List<dev.chandradsl.m3ecanvas.editor.state.HierarchyViolation> {
        val violations = mutableListOf<dev.chandradsl.m3ecanvas.editor.state.HierarchyViolation>()

        if (project.nodes.count { it.type == ComponentType.SCAFFOLD } > 1) {
            violations.add(
                dev.chandradsl.m3ecanvas.editor.state.HierarchyViolation(
                    nodeId = "",
                    nodeName = "Scaffold",
                    nodeType = ComponentType.SCAFFOLD,
                    parentId = null,
                    parentType = null,
                    description = "Multiple Scaffold instances found at project root"
                )
            )
        }

        fun checkNode(node: CanvasNode, parent: CanvasNode?) {
            if (parent != null) {
                if (!parent.type.canAcceptChild(node.type)) {
                    violations.add(
                        dev.chandradsl.m3ecanvas.editor.state.HierarchyViolation(
                            nodeId = node.id,
                            nodeName = node.name,
                            nodeType = node.type,
                            parentId = parent.id,
                            parentType = parent.type,
                            description = "${parent.type.displayName} cannot contain ${node.type.displayName}"
                        )
                    )
                }
                if (node.slot != null && !node.type.allowedSlotsIn(parent.type).contains(node.slot)) {
                    violations.add(
                        dev.chandradsl.m3ecanvas.editor.state.HierarchyViolation(
                            nodeId = node.id,
                            nodeName = node.name,
                            nodeType = node.type,
                            parentId = parent.id,
                            parentType = parent.type,
                            description = "Slot ${node.slot.displayName} is not allowed for ${node.type.displayName} in ${parent.type.displayName}"
                        )
                    )
                }
            } else {
                val hasScaffold = project.nodes.any { it.type == ComponentType.SCAFFOLD }
                if (!node.type.isAllowedAtRoot(hasScaffold = hasScaffold && node.type != ComponentType.SCAFFOLD)) {
                    violations.add(
                        dev.chandradsl.m3ecanvas.editor.state.HierarchyViolation(
                            nodeId = node.id,
                            nodeName = node.name,
                            nodeType = node.type,
                            parentId = null,
                            parentType = null,
                            description = "${node.type.displayName} is not allowed as a root component"
                        )
                    )
                }
            }

            val seenSingleOccupantSlots = mutableSetOf<SlotRole>()
            for (child in node.children) {
                if (child.slot != null && !child.slot.isMultiOccupant) {
                    if (child.slot in seenSingleOccupantSlots) {
                        violations.add(
                            dev.chandradsl.m3ecanvas.editor.state.HierarchyViolation(
                                nodeId = child.id,
                                nodeName = child.name,
                                nodeType = child.type,
                                parentId = node.id,
                                parentType = node.type,
                                description = "Duplicate single-occupant slot ${child.slot.displayName} in ${node.type.displayName}"
                            )
                        )
                    } else {
                        seenSingleOccupantSlots.add(child.slot)
                    }
                }
                checkNode(child, node)
            }
        }

        for (root in project.nodes) {
            checkNode(root, null)
        }

        return violations
    }

    /**
     * Updates the canvas position of the node identified by [nodeId].
     */
    fun updateNodePosition(project: M3EProject, nodeId: String, newPosition: CanvasPosition): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        return project.updateNode(node.copy(position = newPosition))
    }

    /**
     * Aligns multiple nodes along the specified [alignment] axis.
     * Computes the collective bounding box and updates positions for top-level nodes,
     * or adjusts modifier alignment for children in Row, Column, or Box containers.
     */
    fun alignNodes(
        project: M3EProject,
        nodeIds: Set<String>,
        alignment: AlignmentType
    ): M3EProject {
        if (nodeIds.size < 2) return project
        val selectedNodes = nodeIds.mapNotNull { project.findNode(it) }.filterNot { it.isLocked }
        if (selectedNodes.size < 2) return project

        // Check if all selected nodes share the same parent container
        val parents = selectedNodes.map { findParent(project, it.id) }
        val commonParent = if (parents.all { it != null && it.id == parents.first()?.id }) parents.first() else null

        if (commonParent != null) {
            when (commonParent.type) {
                ComponentType.COLUMN, ComponentType.LAZY_COLUMN -> {
                    // Column aligns children horizontally via Modifier.align
                    val target = when (alignment) {
                        AlignmentType.LEFT -> AlignTarget.START
                        AlignmentType.CENTER_HORIZONTALLY -> AlignTarget.CENTER_HORIZONTALLY
                        AlignmentType.RIGHT -> AlignTarget.END
                        else -> null
                    }
                    if (target != null) {
                        var currentProject = project
                        for (node in selectedNodes) {
                            val updated = withAlignmentModifier(node, target)
                            currentProject = currentProject.updateNode(updated)
                        }
                        return currentProject
                    }
                }
                ComponentType.ROW, ComponentType.LAZY_ROW -> {
                    // Row aligns children vertically via Modifier.align
                    val target = when (alignment) {
                        AlignmentType.TOP -> AlignTarget.TOP
                        AlignmentType.CENTER_VERTICALLY -> AlignTarget.CENTER_VERTICALLY
                        AlignmentType.BOTTOM -> AlignTarget.BOTTOM
                        else -> null
                    }
                    if (target != null) {
                        var currentProject = project
                        for (node in selectedNodes) {
                            val updated = withAlignmentModifier(node, target)
                            currentProject = currentProject.updateNode(updated)
                        }
                        return currentProject
                    }
                }
                ComponentType.BOX, ComponentType.BOX_WITH_CONSTRAINTS -> {
                    // Box supports 2D alignment
                    val target = when (alignment) {
                        AlignmentType.LEFT -> AlignTarget.CENTER_START
                        AlignmentType.CENTER_HORIZONTALLY -> AlignTarget.CENTER
                        AlignmentType.RIGHT -> AlignTarget.CENTER_END
                        AlignmentType.TOP -> AlignTarget.TOP_CENTER
                        AlignmentType.CENTER_VERTICALLY -> AlignTarget.CENTER
                        AlignmentType.BOTTOM -> AlignTarget.BOTTOM_CENTER
                    }
                    var currentProject = project
                    for (node in selectedNodes) {
                        val updated = withAlignmentModifier(node, target)
                        currentProject = currentProject.updateNode(updated)
                    }
                    return currentProject
                }
                else -> { /* Fallback to freeform position update */ }
            }
        }

        // Top-level / freeform nodes: bounding box alignment
        val minX = selectedNodes.minOf { it.position.x }
        val maxX = selectedNodes.maxOf { it.position.x + it.size.width }
        val minY = selectedNodes.minOf { it.position.y }
        val maxY = selectedNodes.maxOf { it.position.y + it.size.height }
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f

        var currentProject = project
        for (node in selectedNodes) {
            val newPosition = when (alignment) {
                AlignmentType.LEFT -> node.position.copy(x = minX)
                AlignmentType.CENTER_HORIZONTALLY -> node.position.copy(x = centerX - node.size.width / 2f)
                AlignmentType.RIGHT -> node.position.copy(x = maxX - node.size.width)
                AlignmentType.TOP -> node.position.copy(y = minY)
                AlignmentType.CENTER_VERTICALLY -> node.position.copy(y = centerY - node.size.height / 2f)
                AlignmentType.BOTTOM -> node.position.copy(y = maxY - node.size.height)
            }
            if (newPosition != node.position) {
                currentProject = updateNodePosition(currentProject, node.id, newPosition)
            }
        }
        return currentProject
    }

    /**
     * Distributes 3 or more nodes with equal spacing between them along the specified [distribution] axis.
     */
    fun distributeNodes(
        project: M3EProject,
        nodeIds: Set<String>,
        distribution: DistributionType
    ): M3EProject {
        if (nodeIds.size < 3) return project
        val selectedNodes = nodeIds.mapNotNull { project.findNode(it) }.filterNot { it.isLocked }
        if (selectedNodes.size < 3) return project

        val parents = selectedNodes.map { findParent(project, it.id) }
        val commonParent = if (parents.all { it != null && it.id == parents.first()?.id }) parents.first() else null

        if (commonParent != null) {
            when {
                (commonParent.type == ComponentType.COLUMN || commonParent.type == ComponentType.LAZY_COLUMN) && distribution == DistributionType.VERTICALLY -> {
                    // In a column, distribute vertically by reordering selected children in spatial order
                    val sorted = selectedNodes.sortedBy { it.position.y }
                    val newChildren = commonParent.children.toMutableList()
                    val selectedIndices = sorted.map { node -> newChildren.indexOfFirst { it.id == node.id } }.sorted()
                    for (i in sorted.indices) {
                        newChildren[selectedIndices[i]] = sorted[i]
                    }
                    return project.updateNode(commonParent.copy(children = newChildren))
                }
                (commonParent.type == ComponentType.ROW || commonParent.type == ComponentType.LAZY_ROW) && distribution == DistributionType.HORIZONTALLY -> {
                    // In a row, distribute horizontally by reordering selected children in spatial order
                    val sorted = selectedNodes.sortedBy { it.position.x }
                    val newChildren = commonParent.children.toMutableList()
                    val selectedIndices = sorted.map { node -> newChildren.indexOfFirst { it.id == node.id } }.sorted()
                    for (i in sorted.indices) {
                        newChildren[selectedIndices[i]] = sorted[i]
                    }
                    return project.updateNode(commonParent.copy(children = newChildren))
                }
                else -> { /* Fall through to canvas position distribution */ }
            }
        }

        var currentProject = project
        when (distribution) {
            DistributionType.HORIZONTALLY -> {
                val sorted = selectedNodes.sortedBy { it.position.x }
                val first = sorted.first()
                val last = sorted.last()
                val totalSpan = (last.position.x + last.size.width) - first.position.x
                val totalItemWidth = sorted.sumOf { it.size.width.toDouble() }.toFloat()
                val availableSpace = totalSpan - totalItemWidth
                if (availableSpace > 0f) {
                    val gap = availableSpace / (sorted.size - 1)
                    var currentX = first.position.x
                    for (node in sorted) {
                        if (node.position.x != currentX) {
                            currentProject = updateNodePosition(currentProject, node.id, node.position.copy(x = currentX))
                        }
                        currentX += node.size.width + gap
                    }
                }
            }
            DistributionType.VERTICALLY -> {
                val sorted = selectedNodes.sortedBy { it.position.y }
                val first = sorted.first()
                val last = sorted.last()
                val totalSpan = (last.position.y + last.size.height) - first.position.y
                val totalItemHeight = sorted.sumOf { it.size.height.toDouble() }.toFloat()
                val availableSpace = totalSpan - totalItemHeight
                if (availableSpace > 0f) {
                    val gap = availableSpace / (sorted.size - 1)
                    var currentY = first.position.y
                    for (node in sorted) {
                        if (node.position.y != currentY) {
                            currentProject = updateNodePosition(currentProject, node.id, node.position.copy(y = currentY))
                        }
                        currentY += node.size.height + gap
                    }
                }
            }
        }
        return currentProject
    }

    private fun withAlignmentModifier(node: CanvasNode, alignTarget: AlignTarget): CanvasNode {
        val existing = node.modifiers.filterIsInstance<ModifierSpec.Align>().firstOrNull()
        return if (existing != null) {
            node.updateModifier(existing.copy(alignment = alignTarget))
        } else {
            node.withModifier(ModifierSpec.Align(alignment = alignTarget))
        }
    }

    /**
     * Groups [nodeIds] into a single container of [containerType] (default [ComponentType.BOX]).
     *
     * Invariants:
     * - At least 1 node must be provided.
     * - Cannot group SCAFFOLD or Overlay nodes.
     * - Locked nodes are skipped / cannot be grouped.
     * - All selected nodes must share the same parent scope.
     *
     * Returns Pair(updatedProject, newContainerId).
     */
    fun groupNodes(
        project: M3EProject,
        nodeIds: Set<String>,
        containerType: ComponentType = ComponentType.BOX
    ): Pair<M3EProject, String?> {
        val validNodes = nodeIds.mapNotNull { project.findNode(it) }
            .filterNot { it.isLocked || it.type == ComponentType.SCAFFOLD || it.type.isOverlay() }
        if (validNodes.isEmpty()) return Pair(project, null)

        val firstParent = findParent(project, validNodes.first().id)
        val allSameParent = validNodes.all { findParent(project, it.id) == firstParent }
        if (!allSameParent) return Pair(project, null)

        if (!validNodes.all { containerType.canAcceptChild(it.type) }) {
            return Pair(project, null)
        }

        val existingGroupCount = project.allNodes().count { it.type == containerType && it.name.startsWith("Group") }
        val groupName = "Group ${existingGroupCount + 1}"

        if (firstParent == null) {
            // Top-level nodes: calculate bounding box
            val minX = validNodes.minOf { it.position.x }
            val minY = validNodes.minOf { it.position.y }
            val maxX = validNodes.maxOf { it.position.x + it.size.width }
            val maxY = validNodes.maxOf { it.position.y + it.size.height }

            val groupPosition = CanvasPosition(minX, minY)
            val groupSize = CanvasSize(maxOf(1f, maxX - minX), maxOf(1f, maxY - minY))

            val adjustedChildren = validNodes.map { node ->
                node.copy(
                    position = CanvasPosition(node.position.x - minX, node.position.y - minY)
                )
            }

            val groupNode = CanvasNode(
                type = containerType,
                name = groupName,
                position = groupPosition,
                size = groupSize,
                children = adjustedChildren
            )

            val validNodeIds = validNodes.map { it.id }.toSet()
            val firstIndex = project.nodes.indexOfFirst { it.id in validNodeIds }.coerceAtLeast(0)
            val remainingNodes = project.nodes.filterNot { it.id in validNodeIds }.toMutableList()
            val insertIndex = firstIndex.coerceAtMost(remainingNodes.size)
            remainingNodes.add(insertIndex, groupNode)

            return Pair(project.copy(nodes = remainingNodes), groupNode.id)
        } else {
            // Nested siblings inside firstParent:
            val validNodeIds = validNodes.map { it.id }.toSet()
            val parentChildren = firstParent.children
            val firstIndex = parentChildren.indexOfFirst { it.id in validNodeIds }.coerceAtLeast(0)

            val groupNode = CanvasNode(
                type = containerType,
                name = groupName,
                position = CanvasPosition.Zero,
                size = CanvasSize(100f, 100f),
                children = validNodes
            )

            val remainingChildren = parentChildren.filterNot { it.id in validNodeIds }.toMutableList()
            val insertIndex = firstIndex.coerceAtMost(remainingChildren.size)
            remainingChildren.add(insertIndex, groupNode)

            val updatedParent = firstParent.copy(children = remainingChildren)
            return Pair(project.updateNode(updatedParent), groupNode.id)
        }
    }

    /**
     * Dissolves [containerId], releasing its children into the container's parent
     * (or canvas root) while preserving visual positions.
     *
     * Invariants:
     * - Cannot ungroup SCAFFOLD or non-container nodes.
     * - Locked containers cannot be ungrouped.
     * - Empty containers cannot be ungrouped.
     *
     * Returns Pair(updatedProject, releasedChildrenIds).
     */
    fun ungroupNode(
        project: M3EProject,
        containerId: String
    ): Pair<M3EProject, List<String>> {
        val container = project.findNode(containerId) ?: return Pair(project, emptyList())
        if (container.isLocked || !container.isContainer || container.type == ComponentType.SCAFFOLD || container.children.isEmpty()) {
            return Pair(project, emptyList())
        }

        val parent = findParent(project, containerId)
        val releasedIds = container.children.map { it.id }

        if (parent == null) {
            // Top-level container: project children positions to canvas coordinates
            val projectedChildren = container.children.map { child ->
                child.copy(
                    position = CanvasPosition(
                        x = container.position.x + child.position.x,
                        y = container.position.y + child.position.y
                    )
                )
            }

            val containerIndex = project.nodes.indexOfFirst { it.id == containerId }
            val mutableNodes = project.nodes.toMutableList()
            if (containerIndex >= 0) {
                mutableNodes.removeAt(containerIndex)
                mutableNodes.addAll(containerIndex, projectedChildren)
            } else {
                mutableNodes.removeAll { it.id == containerId }
                mutableNodes.addAll(projectedChildren)
            }

            return Pair(project.copy(nodes = mutableNodes), releasedIds)
        } else {
            // Nested container: promote children into parent
            val containerIndex = parent.children.indexOfFirst { it.id == containerId }
            val mutableChildren = parent.children.toMutableList()
            if (containerIndex >= 0) {
                mutableChildren.removeAt(containerIndex)
                mutableChildren.addAll(containerIndex, container.children)
            } else {
                mutableChildren.removeAll { it.id == containerId }
                mutableChildren.addAll(container.children)
            }

            val updatedParent = parent.copy(children = mutableChildren)
            return Pair(project.updateNode(updatedParent), releasedIds)
        }
    }

    /**
     * Checks if [ancestorId] is an ancestor of [targetId] in the node hierarchy.
     */
    fun isAncestorOf(project: M3EProject, ancestorId: String, targetId: String): Boolean {
        if (ancestorId == targetId) return true
        val ancestorNode = project.findNode(ancestorId) ?: return false
        return ancestorNode.findNode(targetId) != null
    }

    /**
     * Reparents [nodeId] to [newParentId] (or moves to root if [newParentId] is null)
     * at optional [targetIndex].
     *
     * Guards:
     * - Locked nodes cannot be reparented.
     * - Prevents cycles: [nodeId] cannot be an ancestor of [newParentId].
     * - Checks [newParent.type.canAcceptChild(node.type)].
     */
    fun reparentNode(
        project: M3EProject,
        nodeId: String,
        newParentId: String?,
        targetIndex: Int? = null
    ): M3EProject {
        val node = project.findNode(nodeId) ?: return project
        if (node.isLocked || node.type == ComponentType.SCAFFOLD || node.type.isOverlay()) return project

        val currentParent = findParent(project, nodeId)
        if (currentParent?.id == newParentId && targetIndex == null) return project

        // Cycle prevention
        if (newParentId != null) {
            if (isAncestorOf(project, ancestorId = nodeId, targetId = newParentId)) {
                return project // Cycle detected!
            }
            val newParent = project.findNode(newParentId) ?: return project
            if (!newParent.isContainer || !newParent.type.canAcceptChild(node.type)) {
                return project // Target cannot accept this child type!
            }
        }

        // 1. Remove node from its current location
        val projectWithoutNode = project.removeNode(nodeId)

        // 2. Insert into new location
        return if (newParentId == null) {
            val mutableNodes = projectWithoutNode.nodes.toMutableList()
            val idx = targetIndex?.coerceIn(0, mutableNodes.size) ?: mutableNodes.size
            mutableNodes.add(idx, node.copy(slot = null))
            projectWithoutNode.copy(nodes = mutableNodes)
        } else {
            val targetParent = projectWithoutNode.findNode(newParentId) ?: return project
            val mutableChildren = targetParent.children.toMutableList()
            val idx = targetIndex?.coerceIn(0, mutableChildren.size) ?: mutableChildren.size
            mutableChildren.add(idx, node)
            val updatedParent = targetParent.copy(children = mutableChildren)
            projectWithoutNode.updateNode(updatedParent)
        }
    }
}

/**
 * Visual alignment operations across multiple selected components.
 */
enum class AlignmentType(val displayName: String) {
    LEFT("Align Left"),
    CENTER_HORIZONTALLY("Align Center Horizontally"),
    RIGHT("Align Right"),
    TOP("Align Top"),
    CENTER_VERTICALLY("Align Center Vertically"),
    BOTTOM("Align Bottom")
}

/**
 * Equal spacing distribution operations across multiple selected components.
 */
enum class DistributionType(val displayName: String) {
    HORIZONTALLY("Distribute Horizontally"),
    VERTICALLY("Distribute Vertically")
}
