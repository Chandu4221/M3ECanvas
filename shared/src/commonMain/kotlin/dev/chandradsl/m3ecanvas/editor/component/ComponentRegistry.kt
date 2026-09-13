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
    val description: String = "",
    val keyParameters: List<String> = emptyList(),
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
            description = "Top-level layout structure implementing Material Design layout patterns.",
            keyParameters = listOf("topBar", "bottomBar", "floatingActionButton", "snackbarHost"),
            defaultSize = { w, h -> CanvasSize(w, h) }
        ),
        ComponentDefinition(
            type = ComponentType.TOP_APP_BAR,
            description = "Displays information and actions relating to the current screen.",
            keyParameters = listOf("title", "navigationIcon", "actions", "variant"),
            defaultSize = { w, _ -> CanvasSize(w, 64f) }
        ),
        ComponentDefinition(
            type = ComponentType.NAVIGATION_BAR,
            description = "Bottom navigation bar providing access to primary destinations.",
            keyParameters = listOf("destinations", "selectedIndex", "containerColor"),
            defaultSize = { w, _ -> CanvasSize(w, 80f) }
        ),
        ComponentDefinition(
            type = ComponentType.NAVIGATION_RAIL,
            description = "Side navigation rail for medium and expanded screens.",
            keyParameters = listOf("destinations", "header", "selectedIndex"),
            defaultSize = { _, h -> CanvasSize(80f, h) }
        ),
        ComponentDefinition(
            type = ComponentType.NAVIGATION_DRAWER,
            description = "Navigation drawer providing access to destinations on larger displays.",
            keyParameters = listOf("gesturesEnabled", "scrimColor"),
            defaultSize = { _, h -> CanvasSize(300f, h) }
        ),
        ComponentDefinition(
            type = ComponentType.TABS,
            description = "Organizes content across different screens or data sets.",
            keyParameters = listOf("selectedTabIndex", "edgePadding"),
            defaultSize = { w, _ -> CanvasSize(w, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.SEARCH,
            description = "Material 3 search bar for query entry and suggestions.",
            keyParameters = listOf("query", "placeholder", "active", "leadingIcon"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.FAB,
            availableVariants = MaterialVariant.FloatingActionButton.entries,
            description = "Floating action button representing the primary action of a screen.",
            keyParameters = listOf("icon", "variant"),
            defaultSize = { _, _ -> CanvasSize(56f, 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.EXTENDED_FAB,
            availableVariants = MaterialVariant.FloatingActionButton.entries,
            description = "Floating action button with an icon and text label.",
            keyParameters = listOf("text", "icon", "expanded"),
            defaultSize = { _, _ -> CanvasSize(140f, 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.BUTTON,
            availableVariants = MaterialVariant.Button.entries,
            description = "Material 3 Button for triggering actions.",
            keyParameters = listOf("text", "variant", "enabled", "icon"),
            defaultSize = { _, _ -> CanvasSize(120f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.ICON_BUTTON,
            availableVariants = MaterialVariant.IconButton.entries,
            description = "Compact clickable button containing a single icon.",
            keyParameters = listOf("icon", "variant", "enabled"),
            defaultSize = { _, _ -> CanvasSize(48f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.SEGMENTED_BUTTON,
            description = "Row of segmented buttons for selecting single or multiple options.",
            keyParameters = listOf("segments", "selectedIndices"),
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(220f), 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.SPLIT_BUTTON,
            description = "Two-part button with primary action and dropdown indicator.",
            keyParameters = listOf("text", "leadingIcon"),
            defaultSize = { _, _ -> CanvasSize(140f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.BUTTON_GROUP,
            description = "Grouped layout container for related action buttons.",
            keyParameters = listOf("spacing"),
            defaultSize = { _, _ -> CanvasSize(260f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.TOGGLE_BUTTON,
            availableVariants = MaterialVariant.ToggleButton.entries,
            description = "Button that toggles between selected and unselected states.",
            keyParameters = listOf("selected", "enabled"),
            defaultSize = { _, _ -> CanvasSize(120f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.CARD,
            availableVariants = MaterialVariant.Card.entries,
            description = "Container for content and actions on a single subject.",
            keyParameters = listOf("variant", "elevation", "shape"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.ELEVATED_CARD,
            description = "Card with elevation shadow for visual separation.",
            keyParameters = listOf("elevation", "shape"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.OUTLINED_CARD,
            description = "Card with an outline border and clean background.",
            keyParameters = listOf("border", "shape"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.LIST_ITEM,
            description = "Standard list row with headline, supporting text, and icon slots.",
            keyParameters = listOf("headlineText", "supportingText", "leadingContent", "trailingContent"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 72f) }
        ),
        ComponentDefinition(
            type = ComponentType.MODAL_BOTTOM_SHEET,
            description = "Sheet sliding from screen bottom covering underlying content.",
            keyParameters = listOf("dragHandle", "scrimColor"),
            defaultSize = { w, _ -> CanvasSize(w, 220f) }
        ),
        ComponentDefinition(
            type = ComponentType.ALERT_DIALOG,
            description = "Modal dialog interrupting user flow for critical actions.",
            keyParameters = listOf("title", "text", "confirmButton", "dismissButton"),
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(260f), 200f) }
        ),
        ComponentDefinition(
            type = ComponentType.BASIC_ALERT_DIALOG,
            description = "Customizable dialog container without default button slots.",
            keyParameters = listOf("content"),
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(260f), 200f) }
        ),
        ComponentDefinition(
            type = ComponentType.SURFACE,
            availableVariants = MaterialVariant.Surface.entries,
            description = "Fundamental surface container with color and elevation.",
            keyParameters = listOf("color", "shape", "shadowElevation"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.SNACKBAR,
            description = "Brief floating message at the bottom of the screen.",
            keyParameters = listOf("message", "actionLabel"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.SNACKBAR_HOST,
            description = "Host container coordinating transient snackbar messages.",
            keyParameters = listOf("hostState"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.BADGE,
            description = "Small notification badge for icon or tab affordances.",
            keyParameters = listOf("text", "containerColor"),
            defaultSize = { _, _ -> CanvasSize(48f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.BADGED_BOX,
            description = "Container positioning a badge over its content element.",
            keyParameters = listOf("badge"),
            defaultSize = { _, _ -> CanvasSize(48f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.NAVIGATION_BAR_ITEM,
            description = "Single item destination inside a navigation bar.",
            keyParameters = listOf("label", "icon", "selected"),
            defaultSize = { _, _ -> CanvasSize(64f, 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.NAVIGATION_RAIL_ITEM,
            description = "Single item destination inside a navigation rail.",
            keyParameters = listOf("label", "icon", "selected"),
            defaultSize = { _, _ -> CanvasSize(56f, 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.TAB,
            description = "Single selectable tab inside a tab row.",
            keyParameters = listOf("text", "icon", "selected"),
            defaultSize = { _, _ -> CanvasSize(90f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.TOOLTIP,
            description = "Informative text label shown upon hovering or pressing.",
            keyParameters = listOf("tooltipText"),
            defaultSize = { _, _ -> CanvasSize(160f, 36f) }
        ),
        ComponentDefinition(
            type = ComponentType.PROGRESS_INDICATOR,
            description = "Visual indicator expressing operation progress.",
            keyParameters = listOf("progress", "color"),
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(200f), 8f) }
        ),
        ComponentDefinition(
            type = ComponentType.LOADING_INDICATOR,
            description = "Circular spinner expressing ongoing activity.",
            keyParameters = listOf("color"),
            defaultSize = { _, _ -> CanvasSize(48f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.CHECKBOX,
            description = "Selection control allowing multiple item selection.",
            keyParameters = listOf("checked", "enabled"),
            defaultSize = { _, _ -> CanvasSize(180f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.RADIO_BUTTON,
            description = "Selection control allowing single mutually-exclusive option.",
            keyParameters = listOf("selected", "enabled"),
            defaultSize = { _, _ -> CanvasSize(180f, 40f) }
        ),
        ComponentDefinition(
            type = ComponentType.SWITCH,
            description = "Toggle control between checked and unchecked state.",
            keyParameters = listOf("checked", "enabled"),
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(180f), 44f) }
        ),
        ComponentDefinition(
            type = ComponentType.SLIDER,
            description = "Continuous range selection control with optional steps.",
            keyParameters = listOf("value", "valueRange", "steps"),
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(200f), 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.RANGE_SLIDER,
            description = "Dual-thumb slider for selecting a range between two points.",
            keyParameters = listOf("valueRange"),
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(200f), 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.CHIPS,
            availableVariants = MaterialVariant.Chip.entries,
            description = "Compact pill representing input, attribute, or action.",
            keyParameters = listOf("label", "leadingIcon", "selected"),
            defaultSize = { _, _ -> CanvasSize(100f, 36f) }
        ),
        ComponentDefinition(
            type = ComponentType.DATE_PICKER,
            description = "Calendar interface for picking a single date.",
            keyParameters = listOf("title", "headline"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(280f), 320f) }
        ),
        ComponentDefinition(
            type = ComponentType.TIME_PICKER,
            description = "Clock dial interface for selecting time.",
            keyParameters = listOf("is24Hour"),
            defaultSize = { w, _ -> CanvasSize((w - 48f).coerceAtLeast(260f), 180f) }
        ),
        ComponentDefinition(
            type = ComponentType.MENUS,
            description = "Dropdown menu displaying a transient list of choices.",
            keyParameters = listOf("expanded"),
            defaultSize = { _, _ -> CanvasSize(180f, 140f) }
        ),
        ComponentDefinition(
            type = ComponentType.DROPDOWN_MENU_ITEM,
            description = "Single item row inside a dropdown menu.",
            keyParameters = listOf("text", "leadingIcon"),
            defaultSize = { _, _ -> CanvasSize(160f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.TEXT_FIELD,
            availableVariants = MaterialVariant.TextField.entries,
            description = "Text input field with label and placeholder support.",
            keyParameters = listOf("value", "label", "placeholder", "isError"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 56f) }
        ),
        ComponentDefinition(
            type = ComponentType.TEXT,
            description = "Basic typography text element with style and color.",
            keyParameters = listOf("text", "style", "color"),
            defaultSize = { _, _ -> CanvasSize(120f, 24f) }
        ),
        ComponentDefinition(
            type = ComponentType.ICON,
            description = "Material Design vector icon display.",
            keyParameters = listOf("icon", "tint"),
            defaultSize = { _, _ -> CanvasSize(24f, 24f) }
        ),
        ComponentDefinition(
            type = ComponentType.IMAGE,
            description = "Raster or vector image display with content scale.",
            keyParameters = listOf("contentScale"),
            defaultSize = { _, _ -> CanvasSize(120f, 120f) }
        ),
        ComponentDefinition(
            type = ComponentType.HORIZONTAL_DIVIDER,
            description = "Thin visual line grouping and separating content horizontally.",
            keyParameters = listOf("thickness", "color"),
            defaultSize = { w, _ -> CanvasSize((w - 32f).coerceAtLeast(200f), 1f) }
        ),
        ComponentDefinition(
            type = ComponentType.VERTICAL_DIVIDER,
            description = "Thin visual line separating content vertically.",
            keyParameters = listOf("thickness", "color"),
            defaultSize = { _, _ -> CanvasSize(1f, 48f) }
        ),
        ComponentDefinition(
            type = ComponentType.SPACER,
            description = "Empty space separator for flexible layout distancing.",
            keyParameters = listOf("width", "height"),
            defaultSize = { _, _ -> CanvasSize(16f, 16f) }
        ),
        ComponentDefinition(
            type = ComponentType.BOTTOM_APP_BAR,
            description = "Bottom app bar displaying navigation and action icons.",
            keyParameters = listOf("actions", "floatingActionButton"),
            defaultSize = { w, _ -> CanvasSize(w, 80f) }
        ),
        ComponentDefinition(
            type = ComponentType.BOTTOM_SHEET_SCAFFOLD,
            description = "Scaffold containing a persistent bottom sheet surface.",
            keyParameters = listOf("sheetContent"),
            defaultSize = { w, h -> CanvasSize(w, h) }
        ),
        ComponentDefinition(
            type = ComponentType.COLUMN,
            description = "Vertical stack layout placing child elements in order.",
            keyParameters = listOf("verticalArrangement", "horizontalAlignment"),
            defaultSize = { w, h -> CanvasSize(w, (h - 160f).coerceAtLeast(200f)) }
        ),
        ComponentDefinition(
            type = ComponentType.ROW,
            description = "Horizontal stack layout placing child elements side-by-side.",
            keyParameters = listOf("horizontalArrangement", "verticalAlignment"),
            defaultSize = { w, _ -> CanvasSize(w, 120f) }
        ),
        ComponentDefinition(
            type = ComponentType.BOX,
            description = "Stack layout positioning children on top of each other.",
            keyParameters = listOf("contentAlignment"),
            defaultSize = { w, _ -> CanvasSize(w, 300f) }
        ),
        ComponentDefinition(
            type = ComponentType.BOX_WITH_CONSTRAINTS,
            description = "Box layout exposing parent width and height constraints.",
            keyParameters = listOf("contentAlignment"),
            defaultSize = { w, _ -> CanvasSize(w, 300f) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_COLUMN,
            description = "Scrollable vertical list rendering visible items efficiently.",
            keyParameters = listOf("verticalArrangement"),
            defaultSize = { w, h -> CanvasSize(w, (h - 160f).coerceAtLeast(200f)) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_ROW,
            description = "Scrollable horizontal list rendering visible items efficiently.",
            keyParameters = listOf("horizontalArrangement"),
            defaultSize = { w, _ -> CanvasSize(w, 120f) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_VERTICAL_GRID,
            description = "Scrollable vertical grid layout with fixed or adaptive columns.",
            keyParameters = listOf("columns"),
            defaultSize = { w, h -> CanvasSize(w, (h - 160f).coerceAtLeast(200f)) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_HORIZONTAL_GRID,
            description = "Scrollable horizontal grid layout with fixed or adaptive rows.",
            keyParameters = listOf("rows"),
            defaultSize = { w, _ -> CanvasSize(w, 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_VERTICAL_STAGGERED_GRID,
            description = "Vertical grid with staggered variable-height items.",
            keyParameters = listOf("columns"),
            defaultSize = { w, h -> CanvasSize(w, (h - 160f).coerceAtLeast(200f)) }
        ),
        ComponentDefinition(
            type = ComponentType.LAZY_HORIZONTAL_STAGGERED_GRID,
            description = "Horizontal grid with staggered variable-width items.",
            keyParameters = listOf("rows"),
            defaultSize = { w, _ -> CanvasSize(w, 160f) }
        ),
        ComponentDefinition(
            type = ComponentType.HORIZONTAL_PAGER,
            description = "Swipeable horizontal page carousel.",
            keyParameters = listOf("pageCount"),
            defaultSize = { w, _ -> CanvasSize(w, 200f) }
        ),
        ComponentDefinition(
            type = ComponentType.CAROUSEL,
            availableVariants = MaterialVariant.Carousel.entries,
            description = "Multi-browse or hero carousel displaying items sequentially.",
            keyParameters = listOf("variant"),
            defaultSize = { w, _ -> CanvasSize(w, 200f) }
        ),
        ComponentDefinition(
            type = ComponentType.FLOW_ROW,
            description = "Horizontal layout wrapping items to the next line when space runs out.",
            keyParameters = listOf("horizontalArrangement"),
            defaultSize = { w, _ -> CanvasSize(w, 120f) }
        ),
        ComponentDefinition(
            type = ComponentType.FLOW_COLUMN,
            description = "Vertical layout wrapping items to the next column when space runs out.",
            keyParameters = listOf("verticalArrangement"),
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
