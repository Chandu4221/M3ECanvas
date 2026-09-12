package dev.chandradsl.m3ecanvas.domain.model

/**
 * Categories used to group components in the palette.
 * These mirror the official Material 3 component categories,
 * plus a Layout category for Compose container composables.
 */
enum class ComponentCategory(val displayName: String) {
    ACTION(displayName = "Action"),
    CONTAINMENT(displayName = "Containment"),
    COMMUNICATION(displayName = "Communication"),
    NAVIGATION(displayName = "Navigation"),
    SELECTION(displayName = "Selection"),
    TEXT_INPUT(displayName = "Text input"),
    TYPOGRAPHY(displayName = "Typography"),
    GRAPHICS(displayName = "Graphics"),
    LAYOUT(displayName = "Layout")
}

/**
 * All Material 3 and Foundation components available in the M3E Canvas.
 * Each type maps to a real Material 3 or Compose composable rendered on the canvas.
 *
 * [isContainer] marks types that can hold child nodes, enabling hierarchical layouts.
 */
enum class ComponentType(
    val displayName: String,
    val category: ComponentCategory,
    val isContainer: Boolean = false
) {
    // Action
    BUTTON(displayName = "Button", category = ComponentCategory.ACTION),
    ICON_BUTTON(displayName = "Icon Button", category = ComponentCategory.ACTION),
    FAB(displayName = "Floating Action Button", category = ComponentCategory.ACTION),
    EXTENDED_FAB(displayName = "Extended FAB", category = ComponentCategory.ACTION),
    SEGMENTED_BUTTON(displayName = "Segmented Button", category = ComponentCategory.ACTION),
    SPLIT_BUTTON(displayName = "Split Button", category = ComponentCategory.ACTION),
    BUTTON_GROUP(displayName = "Button Group", category = ComponentCategory.ACTION),

    // Containment
    CARD(displayName = "Card", category = ComponentCategory.CONTAINMENT, isContainer = true),
    LISTS(displayName = "Lists", category = ComponentCategory.CONTAINMENT, isContainer = true),
    SHEETS(displayName = "Sheets", category = ComponentCategory.CONTAINMENT, isContainer = true),
    DIALOG(displayName = "Dialog", category = ComponentCategory.CONTAINMENT, isContainer = true),
    SURFACE(displayName = "Surface", category = ComponentCategory.CONTAINMENT, isContainer = true),

    // Communication
    SNACKBAR(displayName = "Snackbar", category = ComponentCategory.COMMUNICATION),
    BADGE(displayName = "Badge", category = ComponentCategory.COMMUNICATION),
    TOOLTIP(displayName = "Tooltip", category = ComponentCategory.COMMUNICATION),
    PROGRESS_INDICATOR(displayName = "Progress Indicator", category = ComponentCategory.COMMUNICATION),
    LOADING_INDICATOR(displayName = "Loading Indicator", category = ComponentCategory.COMMUNICATION),

    // Navigation
    TOP_APP_BAR(displayName = "Top App Bar", category = ComponentCategory.NAVIGATION, isContainer = true),
    NAVIGATION_BAR(displayName = "Navigation Bar", category = ComponentCategory.NAVIGATION),
    BOTTOM_APP_BAR(displayName = "Bottom App Bar", category = ComponentCategory.NAVIGATION),
    NAVIGATION_RAIL(displayName = "Navigation Rail", category = ComponentCategory.NAVIGATION),
    NAVIGATION_DRAWER(displayName = "Navigation Drawer", category = ComponentCategory.NAVIGATION),
    TABS(displayName = "Tabs", category = ComponentCategory.NAVIGATION),
    SEARCH(displayName = "Search", category = ComponentCategory.NAVIGATION),

    // Selection
    CHECKBOX(displayName = "Checkbox", category = ComponentCategory.SELECTION),
    RADIO_BUTTON(displayName = "Radio Button", category = ComponentCategory.SELECTION),
    SWITCH(displayName = "Switch", category = ComponentCategory.SELECTION),
    SLIDER(displayName = "Slider", category = ComponentCategory.SELECTION),
    RANGE_SLIDER(displayName = "Range Slider", category = ComponentCategory.SELECTION),
    CHIPS(displayName = "Chips", category = ComponentCategory.SELECTION),
    DATE_PICKER(displayName = "Date Picker", category = ComponentCategory.SELECTION),
    TIME_PICKER(displayName = "Time Picker", category = ComponentCategory.SELECTION),
    MENUS(displayName = "Menus", category = ComponentCategory.SELECTION),

    // Text input
    TEXT_FIELD(displayName = "Text Field", category = ComponentCategory.TEXT_INPUT),

    // Typography
    TEXT(displayName = "Text", category = ComponentCategory.TYPOGRAPHY),

    // Graphics
    ICON(displayName = "Icon", category = ComponentCategory.GRAPHICS),
    IMAGE(displayName = "Image", category = ComponentCategory.GRAPHICS),
    HORIZONTAL_DIVIDER(displayName = "Horizontal Divider", category = ComponentCategory.GRAPHICS),
    VERTICAL_DIVIDER(displayName = "Vertical Divider", category = ComponentCategory.GRAPHICS),

    // Layout containers
    SCAFFOLD(displayName = "Scaffold", category = ComponentCategory.LAYOUT, isContainer = true),
    COLUMN(displayName = "Column", category = ComponentCategory.LAYOUT, isContainer = true),
    ROW(displayName = "Row", category = ComponentCategory.LAYOUT, isContainer = true),
    BOX(displayName = "Box", category = ComponentCategory.LAYOUT, isContainer = true),
    LAZY_COLUMN(displayName = "Lazy Column", category = ComponentCategory.LAYOUT, isContainer = true),
    LAZY_ROW(displayName = "Lazy Row", category = ComponentCategory.LAYOUT, isContainer = true),
    LAZY_VERTICAL_GRID(displayName = "Lazy Vertical Grid", category = ComponentCategory.LAYOUT, isContainer = true),
    FLOW_ROW(displayName = "Flow Row", category = ComponentCategory.LAYOUT, isContainer = true),
    FLOW_COLUMN(displayName = "Flow Column", category = ComponentCategory.LAYOUT, isContainer = true),
    SPACER(displayName = "Spacer", category = ComponentCategory.LAYOUT);

    /**
     * Determines the canonical structural slot role this component type binds to
     * when added to a Scaffold.
     */
    fun canonicalSlot(): SlotRole {
        return when (this) {
            TOP_APP_BAR -> SlotRole.TOP_BAR
            NAVIGATION_BAR, BOTTOM_APP_BAR -> SlotRole.BOTTOM_BAR
            NAVIGATION_RAIL -> SlotRole.RAIL
            NAVIGATION_DRAWER -> SlotRole.DRAWER
            FAB, EXTENDED_FAB -> SlotRole.FAB
            SNACKBAR -> SlotRole.SNACKBAR
            else -> SlotRole.CONTENT
        }
    }

    /**
     * Returns the list of named slots supported by this component type.
     */
    fun supportedSlots(): List<SlotRole> {
        return when (this) {
            SCAFFOLD -> listOf(
                SlotRole.TOP_BAR,
                SlotRole.BOTTOM_BAR,
                SlotRole.RAIL,
                SlotRole.DRAWER,
                SlotRole.FAB,
                SlotRole.SNACKBAR,
                SlotRole.CONTENT
            )
            TOP_APP_BAR -> listOf(
                SlotRole.NAVIGATION_ICON,
                SlotRole.TITLE,
                SlotRole.ACTIONS
            )
            LISTS -> listOf(
                SlotRole.LEADING,
                SlotRole.HEADLINE,
                SlotRole.SUPPORTING,
                SlotRole.TRAILING,
                SlotRole.OVERLINE
            )
            DIALOG -> listOf(
                SlotRole.ICON,
                SlotRole.TITLE,
                SlotRole.CONTENT,
                SlotRole.CONFIRM_BUTTON,
                SlotRole.DISMISS_BUTTON
            )
            TEXT_FIELD -> listOf(
                SlotRole.LEADING,
                SlotRole.TRAILING
            )
            CARD, SHEETS, SURFACE,
            COLUMN, ROW, BOX,
            LAZY_COLUMN, LAZY_ROW, LAZY_VERTICAL_GRID,
            FLOW_ROW, FLOW_COLUMN -> listOf(SlotRole.CONTENT)
            else -> emptyList()
        }
    }

    /**
     * Determines the canonical slot when this component is placed inside [parentType].
     */
    fun canonicalSlotFor(parentType: ComponentType): SlotRole {
        return when (parentType) {
            SCAFFOLD -> canonicalSlot()
            TOP_APP_BAR -> when (this) {
                TEXT -> SlotRole.TITLE
                ICON, ICON_BUTTON -> SlotRole.ACTIONS
                else -> SlotRole.ACTIONS
            }
            LISTS -> when (this) {
                ICON, IMAGE -> SlotRole.LEADING
                CHECKBOX, SWITCH, RADIO_BUTTON, ICON_BUTTON -> SlotRole.TRAILING
                TEXT -> SlotRole.HEADLINE
                else -> SlotRole.CONTENT
            }
            DIALOG -> when (this) {
                ICON, IMAGE -> SlotRole.ICON
                TEXT -> SlotRole.CONTENT
                BUTTON -> SlotRole.CONFIRM_BUTTON
                else -> SlotRole.CONTENT
            }
            TEXT_FIELD -> when (this) {
                ICON, ICON_BUTTON -> SlotRole.LEADING
                else -> SlotRole.CONTENT
            }
            else -> SlotRole.CONTENT
        }
    }
}