package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
 * Each type maps 1:1 to a real Material 3 or Compose composable rendered on the canvas.
 *
 * [isContainer] marks types that can hold child nodes, enabling hierarchical layouts.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
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
    TOGGLE_BUTTON(displayName = "Toggle Button", category = ComponentCategory.ACTION),

    // Containment
    CARD(displayName = "Card", category = ComponentCategory.CONTAINMENT, isContainer = true),
    ELEVATED_CARD(displayName = "Elevated Card", category = ComponentCategory.CONTAINMENT, isContainer = true),
    OUTLINED_CARD(displayName = "Outlined Card", category = ComponentCategory.CONTAINMENT, isContainer = true),
    @JsonNames("LISTS")
    LIST_ITEM(displayName = "ListItem", category = ComponentCategory.CONTAINMENT, isContainer = true),
    @JsonNames("SHEETS")
    MODAL_BOTTOM_SHEET(displayName = "Modal Bottom Sheet", category = ComponentCategory.CONTAINMENT, isContainer = true),
    @JsonNames("DIALOG")
    ALERT_DIALOG(displayName = "Alert Dialog", category = ComponentCategory.CONTAINMENT, isContainer = true),
    BASIC_ALERT_DIALOG(displayName = "Basic Alert Dialog", category = ComponentCategory.CONTAINMENT, isContainer = true),
    SURFACE(displayName = "Surface", category = ComponentCategory.CONTAINMENT, isContainer = true),

    // Communication
    SNACKBAR(displayName = "Snackbar", category = ComponentCategory.COMMUNICATION),
    SNACKBAR_HOST(displayName = "Snackbar Host", category = ComponentCategory.COMMUNICATION),
    BADGE(displayName = "Badge", category = ComponentCategory.COMMUNICATION),
    BADGED_BOX(displayName = "Badged Box", category = ComponentCategory.COMMUNICATION, isContainer = true),
    TOOLTIP(displayName = "Tooltip", category = ComponentCategory.COMMUNICATION),
    PROGRESS_INDICATOR(displayName = "Progress Indicator", category = ComponentCategory.COMMUNICATION),
    LOADING_INDICATOR(displayName = "Loading Indicator", category = ComponentCategory.COMMUNICATION),

    // Navigation
    TOP_APP_BAR(displayName = "Top App Bar", category = ComponentCategory.NAVIGATION, isContainer = true),
    NAVIGATION_BAR(displayName = "Navigation Bar", category = ComponentCategory.NAVIGATION, isContainer = true),
    NAVIGATION_BAR_ITEM(displayName = "Navigation Bar Item", category = ComponentCategory.NAVIGATION),
    BOTTOM_APP_BAR(displayName = "Bottom App Bar", category = ComponentCategory.NAVIGATION, isContainer = true),
    NAVIGATION_RAIL(displayName = "Navigation Rail", category = ComponentCategory.NAVIGATION, isContainer = true),
    NAVIGATION_RAIL_ITEM(displayName = "Navigation Rail Item", category = ComponentCategory.NAVIGATION),
    NAVIGATION_DRAWER(displayName = "Navigation Drawer", category = ComponentCategory.NAVIGATION, isContainer = true),
    TABS(displayName = "Tabs", category = ComponentCategory.NAVIGATION, isContainer = true),
    TAB(displayName = "Tab", category = ComponentCategory.NAVIGATION),
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
    MENUS(displayName = "Menus", category = ComponentCategory.SELECTION, isContainer = true),
    DROPDOWN_MENU_ITEM(displayName = "Dropdown Menu Item", category = ComponentCategory.SELECTION),

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
    BOTTOM_SHEET_SCAFFOLD(displayName = "Bottom Sheet Scaffold", category = ComponentCategory.LAYOUT, isContainer = true),
    COLUMN(displayName = "Column", category = ComponentCategory.LAYOUT, isContainer = true),
    ROW(displayName = "Row", category = ComponentCategory.LAYOUT, isContainer = true),
    BOX(displayName = "Box", category = ComponentCategory.LAYOUT, isContainer = true),
    BOX_WITH_CONSTRAINTS(displayName = "BoxWithConstraints", category = ComponentCategory.LAYOUT, isContainer = true),
    LAZY_COLUMN(displayName = "Lazy Column", category = ComponentCategory.LAYOUT, isContainer = true),
    LAZY_ROW(displayName = "Lazy Row", category = ComponentCategory.LAYOUT, isContainer = true),
    LAZY_VERTICAL_GRID(displayName = "Lazy Vertical Grid", category = ComponentCategory.LAYOUT, isContainer = true),
    LAZY_HORIZONTAL_GRID(displayName = "Lazy Horizontal Grid", category = ComponentCategory.LAYOUT, isContainer = true),
    LAZY_VERTICAL_STAGGERED_GRID(displayName = "Lazy Vertical Staggered Grid", category = ComponentCategory.LAYOUT, isContainer = true),
    LAZY_HORIZONTAL_STAGGERED_GRID(displayName = "Lazy Horizontal Staggered Grid", category = ComponentCategory.LAYOUT, isContainer = true),
    HORIZONTAL_PAGER(displayName = "Horizontal Pager", category = ComponentCategory.LAYOUT, isContainer = true),
    CAROUSEL(displayName = "Carousel", category = ComponentCategory.LAYOUT, isContainer = true),
    FLOW_ROW(displayName = "Flow Row", category = ComponentCategory.LAYOUT, isContainer = true),
    FLOW_COLUMN(displayName = "Flow Column", category = ComponentCategory.LAYOUT, isContainer = true),
    SPACER(displayName = "Spacer", category = ComponentCategory.LAYOUT);

    /**
     * Determines whether this component is a vertically scrolling lazy layout.
     */
    fun isVerticalLazy(): Boolean = when (this) {
        LAZY_COLUMN, LAZY_VERTICAL_GRID, LAZY_VERTICAL_STAGGERED_GRID -> true
        else -> false
    }

    /**
     * Determines whether this component is a horizontally scrolling lazy layout.
     */
    fun isHorizontalLazy(): Boolean = when (this) {
        LAZY_ROW, LAZY_HORIZONTAL_GRID, LAZY_HORIZONTAL_STAGGERED_GRID, HORIZONTAL_PAGER, CAROUSEL -> true
        else -> false
    }

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
            SNACKBAR, SNACKBAR_HOST -> SlotRole.SNACKBAR
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
            BOTTOM_SHEET_SCAFFOLD -> listOf(
                SlotRole.SHEET_CONTENT,
                SlotRole.TOP_BAR,
                SlotRole.FAB,
                SlotRole.SNACKBAR,
                SlotRole.CONTENT
            )
            TOP_APP_BAR -> listOf(
                SlotRole.NAVIGATION_ICON,
                SlotRole.TITLE,
                SlotRole.ACTIONS
            )
            LIST_ITEM -> listOf(
                SlotRole.LEADING,
                SlotRole.HEADLINE,
                SlotRole.SUPPORTING,
                SlotRole.TRAILING,
                SlotRole.OVERLINE
            )
            ALERT_DIALOG -> listOf(
                SlotRole.ICON,
                SlotRole.TITLE,
                SlotRole.CONTENT,
                SlotRole.CONFIRM_BUTTON,
                SlotRole.DISMISS_BUTTON
            )
            BADGED_BOX -> listOf(
                SlotRole.BADGE,
                SlotRole.CONTENT
            )
            NAVIGATION_RAIL -> listOf(
                SlotRole.HEADER,
                SlotRole.CONTENT
            )
            BOTTOM_APP_BAR -> listOf(
                SlotRole.ACTIONS,
                SlotRole.FAB
            )
            TEXT_FIELD -> listOf(
                SlotRole.LEADING,
                SlotRole.TRAILING
            )
            CARD, ELEVATED_CARD, OUTLINED_CARD, MODAL_BOTTOM_SHEET, BASIC_ALERT_DIALOG, SURFACE,
            COLUMN, ROW, BOX, BOX_WITH_CONSTRAINTS,
            LAZY_COLUMN, LAZY_ROW, LAZY_VERTICAL_GRID, LAZY_HORIZONTAL_GRID,
            LAZY_VERTICAL_STAGGERED_GRID, LAZY_HORIZONTAL_STAGGERED_GRID,
            HORIZONTAL_PAGER, CAROUSEL,
            FLOW_ROW, FLOW_COLUMN,
            NAVIGATION_BAR, NAVIGATION_DRAWER, TABS, MENUS -> listOf(SlotRole.CONTENT)
            else -> emptyList()
        }
    }

    /**
     * Determines the canonical slot when this component is placed inside [parentType].
     */
    fun canonicalSlotFor(parentType: ComponentType): SlotRole {
        return when (parentType) {
            SCAFFOLD, BOTTOM_SHEET_SCAFFOLD -> canonicalSlot()
            TOP_APP_BAR -> when (this) {
                TEXT -> SlotRole.TITLE
                ICON, ICON_BUTTON -> SlotRole.ACTIONS
                else -> SlotRole.ACTIONS
            }
            LIST_ITEM -> when (this) {
                ICON, IMAGE -> SlotRole.LEADING
                CHECKBOX, SWITCH, RADIO_BUTTON, ICON_BUTTON, BADGE -> SlotRole.TRAILING
                TEXT -> SlotRole.HEADLINE
                else -> SlotRole.HEADLINE
            }
            ALERT_DIALOG -> when (this) {
                ICON, IMAGE -> SlotRole.ICON
                TEXT -> SlotRole.CONTENT
                BUTTON, ICON_BUTTON, TOGGLE_BUTTON -> SlotRole.CONFIRM_BUTTON
                else -> SlotRole.CONTENT
            }
            BADGED_BOX -> when (this) {
                BADGE -> SlotRole.BADGE
                else -> SlotRole.CONTENT
            }
            BOTTOM_APP_BAR -> when (this) {
                FAB, EXTENDED_FAB -> SlotRole.FAB
                else -> SlotRole.ACTIONS
            }
            NAVIGATION_RAIL -> when (this) {
                FAB, EXTENDED_FAB, ICON, IMAGE -> SlotRole.HEADER
                else -> SlotRole.CONTENT
            }
            TEXT_FIELD -> when (this) {
                ICON, ICON_BUTTON -> SlotRole.LEADING
                else -> SlotRole.CONTENT
            }
            else -> SlotRole.CONTENT
        }
    }

    /**
     * Returns true if this container component type can legally accept [childType]
     * according to Material 3 guidelines and Compose layout constraints.
     */
    fun canAcceptChild(childType: ComponentType): Boolean {
        if (!this.isContainer) return false

        // Cannot nest screen-level scaffolds
        if (childType == SCAFFOLD || childType == BOTTOM_SHEET_SCAFFOLD) return false

        // Overlay dialogs cannot be in-flow children
        if (childType == ALERT_DIALOG || childType == BASIC_ALERT_DIALOG) return false

        // Scaffold-specific structural bars & snackbars belong only to Scaffolds
        when (childType) {
            TOP_APP_BAR, BOTTOM_APP_BAR, NAVIGATION_BAR, NAVIGATION_RAIL, NAVIGATION_DRAWER, SNACKBAR, SNACKBAR_HOST -> {
                return this == SCAFFOLD || this == BOTTOM_SHEET_SCAFFOLD
            }
            else -> Unit
        }

        // Compose runtime constraints: prevent nested same-axis lazy layouts (infinite dimension crash)
        if (this.isVerticalLazy() && childType.isVerticalLazy()) return false
        if (this.isHorizontalLazy() && childType.isHorizontalLazy()) return false

        return when (this) {
            SCAFFOLD, BOTTOM_SHEET_SCAFFOLD -> true
            TOP_APP_BAR -> when (childType) {
                TEXT, ICON, ICON_BUTTON, IMAGE, ROW -> true
                else -> false
            }
            BADGED_BOX -> true
            NAVIGATION_BAR -> when (childType) {
                NAVIGATION_BAR_ITEM, ICON, TEXT, BADGE -> true
                else -> true
            }
            NAVIGATION_RAIL -> when (childType) {
                NAVIGATION_RAIL_ITEM, FAB, EXTENDED_FAB, ICON, IMAGE -> true
                else -> true
            }
            TABS -> when (childType) {
                TAB, TEXT, ICON -> true
                else -> true
            }
            MENUS -> when (childType) {
                DROPDOWN_MENU_ITEM, HORIZONTAL_DIVIDER -> true
                else -> true
            }
            LIST_ITEM, CARD, ELEVATED_CARD, OUTLINED_CARD, SURFACE, MODAL_BOTTOM_SHEET, BASIC_ALERT_DIALOG, ALERT_DIALOG,
            COLUMN, ROW, BOX, BOX_WITH_CONSTRAINTS,
            LAZY_COLUMN, LAZY_ROW, LAZY_VERTICAL_GRID, LAZY_HORIZONTAL_GRID,
            LAZY_VERTICAL_STAGGERED_GRID, LAZY_HORIZONTAL_STAGGERED_GRID,
            HORIZONTAL_PAGER, CAROUSEL,
            FLOW_ROW, FLOW_COLUMN -> true
            else -> false
        }
    }

    /**
     * Returns true if this component can be placed as a child of [parentType].
     */
    fun canBeChildOf(parentType: ComponentType): Boolean {
        return parentType.canAcceptChild(this)
    }

    /**
     * Determines whether this component type can exist as a top-level root node in the project.
     * When [hasScaffold] is true, another Scaffold cannot be added at root.
     */
    fun isAllowedAtRoot(hasScaffold: Boolean): Boolean {
        return if (hasScaffold) {
            this != SCAFFOLD && this != BOTTOM_SHEET_SCAFFOLD
        } else {
            true
        }
    }

    /**
     * Returns the valid slots this component type is semantically allowed to occupy inside [parentType].
     * If this returns a list with 1 or 0 elements, no slot selection dropdown should be shown.
     */
    fun allowedSlotsIn(parentType: ComponentType): List<SlotRole> {
        if (!parentType.canAcceptChild(this)) return emptyList()

        return when (parentType) {
            SCAFFOLD -> when (this) {
                TOP_APP_BAR -> listOf(SlotRole.TOP_BAR)
                NAVIGATION_BAR, BOTTOM_APP_BAR -> listOf(SlotRole.BOTTOM_BAR)
                NAVIGATION_RAIL -> listOf(SlotRole.RAIL)
                NAVIGATION_DRAWER -> listOf(SlotRole.DRAWER)
                FAB, EXTENDED_FAB -> listOf(SlotRole.FAB)
                SNACKBAR, SNACKBAR_HOST -> listOf(SlotRole.SNACKBAR)
                else -> listOf(SlotRole.CONTENT)
            }
            BOTTOM_SHEET_SCAFFOLD -> when (this) {
                TOP_APP_BAR -> listOf(SlotRole.TOP_BAR)
                FAB, EXTENDED_FAB -> listOf(SlotRole.FAB)
                SNACKBAR, SNACKBAR_HOST -> listOf(SlotRole.SNACKBAR)
                else -> listOf(SlotRole.CONTENT, SlotRole.SHEET_CONTENT)
            }
            TOP_APP_BAR -> when (this) {
                ICON, ICON_BUTTON -> listOf(SlotRole.ACTIONS, SlotRole.NAVIGATION_ICON)
                TEXT -> listOf(SlotRole.TITLE)
                else -> listOf(SlotRole.ACTIONS)
            }
            LIST_ITEM -> when (this) {
                ICON, IMAGE -> listOf(
                    SlotRole.LEADING,
                    SlotRole.TRAILING,
                    SlotRole.HEADLINE,
                    SlotRole.SUPPORTING,
                    SlotRole.OVERLINE
                )
                CHECKBOX, SWITCH, RADIO_BUTTON, ICON_BUTTON, BADGE -> listOf(
                    SlotRole.TRAILING,
                    SlotRole.LEADING,
                    SlotRole.HEADLINE,
                    SlotRole.SUPPORTING,
                    SlotRole.OVERLINE
                )
                TEXT -> listOf(
                    SlotRole.HEADLINE,
                    SlotRole.SUPPORTING,
                    SlotRole.OVERLINE,
                    SlotRole.TRAILING,
                    SlotRole.LEADING
                )
                else -> listOf(
                    SlotRole.HEADLINE,
                    SlotRole.SUPPORTING,
                    SlotRole.LEADING,
                    SlotRole.TRAILING,
                    SlotRole.OVERLINE
                )
            }
            ALERT_DIALOG -> when (this) {
                BUTTON, ICON_BUTTON, TOGGLE_BUTTON -> listOf(SlotRole.CONFIRM_BUTTON, SlotRole.DISMISS_BUTTON)
                ICON, IMAGE -> listOf(SlotRole.ICON)
                TEXT -> listOf(SlotRole.TITLE, SlotRole.CONTENT, SlotRole.SUPPORTING)
                else -> listOf(SlotRole.CONTENT)
            }
            BADGED_BOX -> when (this) {
                BADGE -> listOf(SlotRole.BADGE)
                else -> listOf(SlotRole.CONTENT)
            }
            NAVIGATION_RAIL -> when (this) {
                FAB, EXTENDED_FAB, ICON, IMAGE -> listOf(SlotRole.HEADER, SlotRole.CONTENT)
                else -> listOf(SlotRole.CONTENT)
            }
            BOTTOM_APP_BAR -> when (this) {
                FAB, EXTENDED_FAB -> listOf(SlotRole.FAB, SlotRole.ACTIONS)
                else -> listOf(SlotRole.ACTIONS)
            }
            TEXT_FIELD -> listOf(SlotRole.LEADING, SlotRole.TRAILING)
            else -> listOf(SlotRole.CONTENT)
        }
    }
}