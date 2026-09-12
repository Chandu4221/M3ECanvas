package dev.chandradsl.m3ecanvas.editor.component

import androidx.compose.ui.graphics.vector.ImageVector
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.palette.icon

/**
 * Metadata definition for a component type in the M3E editor.
 * Consolidates layout sizing, available variants, slot mapping, and categorization
 * into a single source of truth.
 */
data class ComponentDefinition(
    val type: ComponentType,
    val displayName: String = type.displayName,
    val category: ComponentCategory = type.category,
    val isContainer: Boolean = type.isContainer,
    val canonicalSlot: SlotRole = type.canonicalSlot(),
    val availableVariants: List<MaterialVariant> = emptyList(),
    val defaultSize: (deviceWidth: Float, deviceHeight: Float) -> CanvasSize
) {
    fun icon(): ImageVector = type.icon()
}

private val DEFAULT_DEFINITIONS: Map<ComponentType, ComponentDefinition> by lazy {
    buildDefaultDefinitions()
}

private fun buildDefaultDefinitions(): Map<ComponentType, ComponentDefinition> {
    val list = listOf(
        ComponentDefinition(
            type = ComponentType.SCAFFOLD,
            defaultSize = { w, h -> CanvasSize(w, h) }
        ),
        ComponentDefinition(
            type = ComponentType.TOP_APP_BAR,
            defaultSize = { w, _ -> CanvasSize(w, 64f) }
        ),
        ComponentDefinition(
            type = ComponentType.NAVIGATION_BAR,
            defaultSize = { w, _ -> CanvasSize(w, 80f) }
        ),
        ComponentDefinition(
            type = ComponentType.NAVIGATION_RAIL,
            defaultSize = { _, h -> CanvasSize(80f, h) }
        ),
        ComponentDefinition(
            type = ComponentType.NAVIGATION_DRAWER,
            defaultSize = { _, h -> CanvasSize(300f, h) }
        ),
        ComponentDefinition(
            type = ComponentType.TABS,
            defaultSize = { w, _ -> CanvasSize(w, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.SEARCH,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.FAB,
            availableVariants = MaterialVariant.FloatingActionButton.entries,
            defaultSize = { _, _ -> CanvasSize(56f, 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.EXTENDED_FAB,
            availableVariants = MaterialVariant.FloatingActionButton.entries,
            defaultSize = { _, _ -> CanvasSize(140f, 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.BUTTON,
            availableVariants = MaterialVariant.Button.entries,
            defaultSize = { _, _ -> CanvasSize(120f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.ICON_BUTTON,
            availableVariants = MaterialVariant.IconButton.entries,
            defaultSize = { _, _ -> CanvasSize(48f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.SEGMENTED_BUTTON,
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(220f), 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.SPLIT_BUTTON,
            defaultSize = { _, _ -> CanvasSize(140f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.BUTTON_GROUP,
            defaultSize = { _, _ -> CanvasSize(260f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.TOGGLE_BUTTON,
            availableVariants = MaterialVariant.ToggleButton.entries,
            defaultSize = { _, _ -> CanvasSize(120f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.CARD,
            availableVariants = MaterialVariant.Card.entries,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.ELEVATED_CARD,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.OUTLINED_CARD,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.LIST_ITEM,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 72f) }
        ),
        ComponentDefinition(
            type = ComponentType.MODAL_BOTTOM_SHEET,
            defaultSize = { w, _ -> CanvasSize(w, 220f) }
        ),
        ComponentDefinition(
            type = ComponentType.ALERT_DIALOG,
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(260f), 200f) }
        ),
        ComponentDefinition(
            type = ComponentType.BASIC_ALERT_DIALOG,
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(260f), 200f) }
        ),
        ComponentDefinition(
            type = ComponentType.SURFACE,
            availableVariants = MaterialVariant.Surface.entries,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.SNACKBAR,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.SNACKBAR_HOST,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.BADGE,
            defaultSize = { _, _ -> CanvasSize(48f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.BADGED_BOX,
            defaultSize = { _, _ -> CanvasSize(48f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.NAVIGATION_BAR_ITEM,
            defaultSize = { _, _ -> CanvasSize(64f, 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.NAVIGATION_RAIL_ITEM,
            defaultSize = { _, _ -> CanvasSize(56f, 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.TAB,
            defaultSize = { _, _ -> CanvasSize(90f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.TOOLTIP,
            defaultSize = { _, _ -> CanvasSize(160f, 36f) }
        ),
        ComponentDefinition(
            type = ComponentType.PROGRESS_INDICATOR,
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(200f), 8f) }
        ),
        ComponentDefinition(
            type = ComponentType.LOADING_INDICATOR,
            defaultSize = { _, _ -> CanvasSize(48f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.CHECKBOX,
            defaultSize = { _, _ -> CanvasSize(180f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.RADIO_BUTTON,
            defaultSize = { _, _ -> CanvasSize(180f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.SWITCH,
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(180f), 44f) }
        ),
        ComponentDefinition(
            type = ComponentType.SLIDER,
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(200f), 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.RANGE_SLIDER,
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(200f), 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.CHIPS,
            availableVariants = MaterialVariant.Chip.entries,
            defaultSize = { _, _ -> CanvasSize(100f, 36f) }
        ),
        ComponentDefinition(
            type = ComponentType.DATE_PICKER,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(280f), 320f) }
        ),
        ComponentDefinition(
            type = ComponentType.TIME_PICKER,
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(260f), 180f) }
        ),
        ComponentDefinition(
            type = ComponentType.MENUS,
            defaultSize = { _, _ -> CanvasSize(180f, 140f) }
        ),
        ComponentDefinition(
            type = ComponentType.DROPDOWN_MENU_ITEM,
            defaultSize = { _, _ -> CanvasSize(160f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.TEXT_FIELD,
            availableVariants = MaterialVariant.TextField.entries,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.TEXT,
            defaultSize = { _, _ -> CanvasSize(120f, 24f) }
        ),
        ComponentDefinition(
            type = ComponentType.ICON,
            defaultSize = { _, _ -> CanvasSize(24f, 24f) }
        ),
        ComponentDefinition(
            type = ComponentType.IMAGE,
            defaultSize = { _, _ -> CanvasSize(120f, 120f) }
        ),
        ComponentDefinition(
            type = ComponentType.HORIZONTAL_DIVIDER,
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 1f) }
        ),
        ComponentDefinition(
            type = ComponentType.VERTICAL_DIVIDER,
            defaultSize = { _, _ -> CanvasSize(1f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.SPACER,
            defaultSize = { _, _ -> CanvasSize(16f, 16f) }
        ),
        ComponentDefinition(
            type = ComponentType.BOTTOM_APP_BAR,
            defaultSize = { w, _ -> CanvasSize(w, 80f) }
        ),
        ComponentDefinition(
            type = ComponentType.BOTTOM_SHEET_SCAFFOLD,
            defaultSize = { w, h -> CanvasSize(w, h) }
        ),
        ComponentDefinition(
            type = ComponentType.COLUMN,
            defaultSize = { w, h -> CanvasSize(w, (h - 160f).coerceAtLeast(200f)) }
        ),
        ComponentDefinition(
            type = ComponentType.ROW,
            defaultSize = { w, _ -> CanvasSize(w, 120f) }
        ),
        ComponentDefinition(
            type = ComponentType.BOX,
            defaultSize = { w, _ -> CanvasSize(w, 300f) }
        ),
        ComponentDefinition(
            type = ComponentType.BOX_WITH_CONSTRAINTS,
            defaultSize = { w, _ -> CanvasSize(w, 300f) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_COLUMN,
            defaultSize = { w, h -> CanvasSize(w, (h - 160f).coerceAtLeast(200f)) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_ROW,
            defaultSize = { w, _ -> CanvasSize(w, 120f) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_VERTICAL_GRID,
            defaultSize = { w, h -> CanvasSize(w, (h - 160f).coerceAtLeast(200f)) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_HORIZONTAL_GRID,
            defaultSize = { w, _ -> CanvasSize(w, 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_VERTICAL_STAGGERED_GRID,
            defaultSize = { w, h -> CanvasSize(w, (h - 160f).coerceAtLeast(200f)) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_HORIZONTAL_STAGGERED_GRID,
            defaultSize = { w, _ -> CanvasSize(w, 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.HORIZONTAL_PAGER,
            defaultSize = { w, _ -> CanvasSize(w, 200f) }
        ),
        ComponentDefinition(
            type = ComponentType.CAROUSEL,
            availableVariants = MaterialVariant.Carousel.entries,
            defaultSize = { w, _ -> CanvasSize(w, 200f) }
        ),
        ComponentDefinition(
            type = ComponentType.FLOW_ROW,
            defaultSize = { w, _ -> CanvasSize(w, 120f) }
        ),
        ComponentDefinition(
            type = ComponentType.FLOW_COLUMN,
            defaultSize = { w, h -> CanvasSize(w, (h - 160f).coerceAtLeast(200f)) }
        )
    )
    return list.associateBy { it.type }
}

/**
 * Service providing component metadata, default dimensions, and variants.
 * Can be instantiated and injected for testing without touching the UI or file system.
 */
open class ComponentRegistry(
    definitions: Map<ComponentType, ComponentDefinition> = DEFAULT_DEFINITIONS
) {
    private val _definitions: MutableMap<ComponentType, ComponentDefinition> = definitions.toMutableMap()
    val registeredDefinitions: Map<ComponentType, ComponentDefinition>
        get() = _definitions

    open fun registerComponent(definition: ComponentDefinition) {
        _definitions[definition.type] = definition
    }

    open fun get(type: ComponentType): ComponentDefinition? = _definitions[type]

    open fun getOrThrow(type: ComponentType): ComponentDefinition =
        _definitions[type] ?: error("No ComponentDefinition registered for $type")

    open fun defaultSizeFor(
        type: ComponentType,
        deviceWidth: Float = 412f,
        deviceHeight: Float = 915f
    ): CanvasSize {
        val def = _definitions[type] ?: return CanvasSize(120f, 40f)
        return def.defaultSize(deviceWidth, deviceHeight)
    }

    open fun variantsFor(type: ComponentType): List<MaterialVariant> {
        return _definitions[type]?.availableVariants ?: emptyList()
    }

    open fun byCategory(category: ComponentCategory): List<ComponentDefinition> {
        return _definitions.values.filter { it.category == category }
    }

    open fun all(): List<ComponentDefinition> = _definitions.values.toList()

    companion object {
        val default: ComponentRegistry by lazy { ComponentRegistry(DEFAULT_DEFINITIONS) }

        fun defaultDefinitions(): Map<ComponentType, ComponentDefinition> = DEFAULT_DEFINITIONS

        fun get(type: ComponentType): ComponentDefinition? = default.get(type)
        fun getOrThrow(type: ComponentType): ComponentDefinition = default.getOrThrow(type)
        fun defaultSizeFor(
            type: ComponentType,
            deviceWidth: Float = 412f,
            deviceHeight: Float = 915f
        ): CanvasSize = default.defaultSizeFor(type, deviceWidth, deviceHeight)

        fun variantsFor(type: ComponentType): List<MaterialVariant> = default.variantsFor(type)
        fun byCategory(category: ComponentCategory): List<ComponentDefinition> = default.byCategory(category)
        fun all(): List<ComponentDefinition> = default.all()
    }
}
