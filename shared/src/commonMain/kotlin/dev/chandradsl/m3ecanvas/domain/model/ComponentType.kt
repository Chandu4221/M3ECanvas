package dev.chandradsl.m3ecanvas.domain.model

/**
 * Categories used to group components in the palette.
 * These mirror the official Material 3 component categories.
 */
enum class ComponentCategory(val displayName: String) {
    ACTION(displayName = "Action"),
    CONTAINMENT(displayName = "Containment"),
    COMMUNICATION(displayName = "Communication"),
    NAVIGATION(displayName = "Navigation"),
    SELECTION(displayName = "Selection"),
    TEXT_INPUT(displayName = "Text input")
}

/**
 * All Material 3 components available in the M3E Canvas.
 * Each type maps to a real Material 3 composable that will be rendered on the canvas.
 */
enum class ComponentType(
    val displayName: String,
    val category: ComponentCategory
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
    CARD(displayName = "Card", category = ComponentCategory.CONTAINMENT),
    LISTS(displayName = "Lists", category = ComponentCategory.CONTAINMENT),
    SHEETS(displayName = "Sheets", category = ComponentCategory.CONTAINMENT),
    DIALOG(displayName = "Dialog", category = ComponentCategory.CONTAINMENT),

    // Communication
    SNACKBAR(displayName = "Snackbar", category = ComponentCategory.COMMUNICATION),
    BADGE(displayName = "Badge", category = ComponentCategory.COMMUNICATION),
    TOOLTIP(displayName = "Tooltip", category = ComponentCategory.COMMUNICATION),
    PROGRESS_INDICATOR(displayName = "Progress Indicator", category = ComponentCategory.COMMUNICATION),
    LOADING_INDICATOR(displayName = "Loading Indicator", category = ComponentCategory.COMMUNICATION),

    // Navigation
    NAVIGATION_BAR(displayName = "Navigation Bar", category = ComponentCategory.NAVIGATION),
    NAVIGATION_RAIL(displayName = "Navigation Rail", category = ComponentCategory.NAVIGATION),
    NAVIGATION_DRAWER(displayName = "Navigation Drawer", category = ComponentCategory.NAVIGATION),
    TOP_APP_BAR(displayName = "Top App Bar", category = ComponentCategory.NAVIGATION),
    TABS(displayName = "Tabs", category = ComponentCategory.NAVIGATION),
    SEARCH(displayName = "Search", category = ComponentCategory.NAVIGATION),

    // Selection
    CHECKBOX(displayName = "Checkbox", category = ComponentCategory.SELECTION),
    RADIO_BUTTON(displayName = "Radio Button", category = ComponentCategory.SELECTION),
    SWITCH(displayName = "Switch", category = ComponentCategory.SELECTION),
    SLIDER(displayName = "Slider", category = ComponentCategory.SELECTION),
    CHIPS(displayName = "Chips", category = ComponentCategory.SELECTION),
    DATE_PICKER(displayName = "Date Picker", category = ComponentCategory.SELECTION),
    TIME_PICKER(displayName = "Time Picker", category = ComponentCategory.SELECTION),
    MENUS(displayName = "Menus", category = ComponentCategory.SELECTION),

    // Text input
    TEXT_FIELD(displayName = "Text Field", category = ComponentCategory.TEXT_INPUT)
}