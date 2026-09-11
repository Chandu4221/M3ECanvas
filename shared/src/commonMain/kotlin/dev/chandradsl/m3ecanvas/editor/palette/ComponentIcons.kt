package dev.chandradsl.m3ecanvas.editor.palette

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chandradsl.m3ecanvas.domain.model.ComponentType

/**
 * Maps each [ComponentType] to a representative Material Outlined icon
 * for display in the component palette.
 *
 * Outlined icons are used to maintain a lighter visual weight in the editor chrome,
 * matching the convention established for the layers panel.
 */
fun ComponentType.icon(): ImageVector {
    return when (this) {
        // Action
        ComponentType.BUTTON -> Icons.Outlined.SmartButton
        ComponentType.ICON_BUTTON -> Icons.Outlined.TouchApp
        ComponentType.FAB -> Icons.Outlined.AddCircle
        ComponentType.EXTENDED_FAB -> Icons.Outlined.AddBox
        ComponentType.SEGMENTED_BUTTON -> Icons.AutoMirrored.Outlined.Segment
        ComponentType.SPLIT_BUTTON -> Icons.Outlined.ViewDay
        ComponentType.BUTTON_GROUP -> Icons.Outlined.GridView

        // Containment
        ComponentType.CARD -> Icons.Outlined.CreditCard
        ComponentType.LISTS -> Icons.AutoMirrored.Outlined.List
        ComponentType.SHEETS -> Icons.Outlined.ViewArray
        ComponentType.DIALOG -> Icons.Outlined.ChatBubbleOutline

        // Communication
        ComponentType.SNACKBAR -> Icons.AutoMirrored.Outlined.Message
        ComponentType.BADGE -> Icons.Outlined.Circle
        ComponentType.TOOLTIP -> Icons.Outlined.Info
        ComponentType.PROGRESS_INDICATOR -> Icons.Outlined.LinearScale
        ComponentType.LOADING_INDICATOR -> Icons.Outlined.Sync

        // Navigation
        ComponentType.NAVIGATION_BAR -> Icons.Outlined.Navigation
        ComponentType.NAVIGATION_RAIL -> Icons.AutoMirrored.Outlined.ViewSidebar
        ComponentType.NAVIGATION_DRAWER -> Icons.Outlined.Menu
        ComponentType.TOP_APP_BAR -> Icons.Outlined.WebAsset
        ComponentType.TABS -> Icons.Outlined.Tab
        ComponentType.SEARCH -> Icons.Outlined.Search

        // Selection
        ComponentType.CHECKBOX -> Icons.Outlined.CheckBoxOutlineBlank
        ComponentType.RADIO_BUTTON -> Icons.Outlined.RadioButtonUnchecked
        ComponentType.SWITCH -> Icons.Outlined.ToggleOn
        ComponentType.SLIDER -> Icons.Outlined.Tune
        ComponentType.CHIPS -> Icons.AutoMirrored.Outlined.Label
        ComponentType.DATE_PICKER -> Icons.Outlined.CalendarMonth
        ComponentType.TIME_PICKER -> Icons.Outlined.Schedule
        ComponentType.MENUS -> Icons.Outlined.MoreVert

        // Text input
        ComponentType.TEXT_FIELD -> Icons.Outlined.TextFields

        // Layout
        ComponentType.SCAFFOLD -> Icons.Outlined.Dashboard
        ComponentType.COLUMN -> Icons.Outlined.ViewColumn
        ComponentType.ROW -> Icons.Outlined.ViewStream
        ComponentType.BOX -> Icons.Outlined.CropSquare
        ComponentType.LAZY_COLUMN -> Icons.AutoMirrored.Outlined.ListAlt
        ComponentType.LAZY_ROW -> Icons.Outlined.ViewCarousel
    }
}