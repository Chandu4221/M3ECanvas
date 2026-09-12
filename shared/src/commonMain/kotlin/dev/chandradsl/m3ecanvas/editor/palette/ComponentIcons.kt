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
        ComponentType.TOGGLE_BUTTON -> Icons.Outlined.ToggleOn

        // Containment
        ComponentType.CARD -> Icons.Outlined.CreditCard
        ComponentType.ELEVATED_CARD -> Icons.Outlined.CreditCard
        ComponentType.OUTLINED_CARD -> Icons.Outlined.CreditCard
        ComponentType.LIST_ITEM -> Icons.AutoMirrored.Outlined.List
        ComponentType.MODAL_BOTTOM_SHEET -> Icons.Outlined.ViewArray
        ComponentType.ALERT_DIALOG -> Icons.Outlined.ChatBubbleOutline
        ComponentType.BASIC_ALERT_DIALOG -> Icons.Outlined.ChatBubbleOutline
        ComponentType.SURFACE -> Icons.Outlined.Layers

        // Communication
        ComponentType.SNACKBAR -> Icons.AutoMirrored.Outlined.Message
        ComponentType.SNACKBAR_HOST -> Icons.AutoMirrored.Outlined.Message
        ComponentType.BADGE -> Icons.Outlined.Circle
        ComponentType.BADGED_BOX -> Icons.Outlined.MarkChatUnread
        ComponentType.TOOLTIP -> Icons.Outlined.Info
        ComponentType.PROGRESS_INDICATOR -> Icons.Outlined.LinearScale
        ComponentType.LOADING_INDICATOR -> Icons.Outlined.Sync

        // Navigation
        ComponentType.NAVIGATION_BAR -> Icons.Outlined.Navigation
        ComponentType.NAVIGATION_BAR_ITEM -> Icons.Outlined.Tab
        ComponentType.BOTTOM_APP_BAR -> Icons.Outlined.CallToAction
        ComponentType.NAVIGATION_RAIL -> Icons.AutoMirrored.Outlined.ViewSidebar
        ComponentType.NAVIGATION_RAIL_ITEM -> Icons.Outlined.Tab
        ComponentType.NAVIGATION_DRAWER -> Icons.Outlined.Menu
        ComponentType.TOP_APP_BAR -> Icons.Outlined.WebAsset
        ComponentType.TABS -> Icons.Outlined.Tab
        ComponentType.TAB -> Icons.Outlined.Tab
        ComponentType.SEARCH -> Icons.Outlined.Search

        // Selection
        ComponentType.CHECKBOX -> Icons.Outlined.CheckBoxOutlineBlank
        ComponentType.RADIO_BUTTON -> Icons.Outlined.RadioButtonUnchecked
        ComponentType.SWITCH -> Icons.Outlined.ToggleOn
        ComponentType.SLIDER -> Icons.Outlined.Tune
        ComponentType.RANGE_SLIDER -> Icons.Outlined.LinearScale
        ComponentType.CHIPS -> Icons.AutoMirrored.Outlined.Label
        ComponentType.DATE_PICKER -> Icons.Outlined.CalendarMonth
        ComponentType.TIME_PICKER -> Icons.Outlined.Schedule
        ComponentType.MENUS -> Icons.Outlined.MoreVert
        ComponentType.DROPDOWN_MENU_ITEM -> Icons.Outlined.MenuOpen

        // Text input
        ComponentType.TEXT_FIELD -> Icons.Outlined.TextFields

        // Typography
        ComponentType.TEXT -> Icons.Outlined.FormatSize

        // Graphics
        ComponentType.ICON -> Icons.Outlined.EmojiEmotions
        ComponentType.IMAGE -> Icons.Outlined.Image
        ComponentType.HORIZONTAL_DIVIDER -> Icons.Outlined.HorizontalRule
        ComponentType.VERTICAL_DIVIDER -> Icons.Outlined.MoreVert

        // Layout
        ComponentType.SCAFFOLD -> Icons.Outlined.Dashboard
        ComponentType.BOTTOM_SHEET_SCAFFOLD -> Icons.Outlined.DashboardCustomize
        ComponentType.COLUMN -> Icons.Outlined.ViewColumn
        ComponentType.ROW -> Icons.Outlined.ViewStream
        ComponentType.BOX -> Icons.Outlined.CropSquare
        ComponentType.BOX_WITH_CONSTRAINTS -> Icons.Outlined.CropFree
        ComponentType.LAZY_COLUMN -> Icons.AutoMirrored.Outlined.ListAlt
        ComponentType.LAZY_ROW -> Icons.Outlined.ViewCarousel
        ComponentType.LAZY_VERTICAL_GRID -> Icons.Outlined.GridOn
        ComponentType.LAZY_HORIZONTAL_GRID -> Icons.Outlined.GridOn
        ComponentType.LAZY_VERTICAL_STAGGERED_GRID -> Icons.Outlined.Dashboard
        ComponentType.LAZY_HORIZONTAL_STAGGERED_GRID -> Icons.Outlined.ViewWeek
        ComponentType.HORIZONTAL_PAGER -> Icons.Outlined.ViewCarousel
        ComponentType.CAROUSEL -> Icons.Outlined.ViewCarousel
        ComponentType.FLOW_ROW -> Icons.AutoMirrored.Outlined.WrapText
        ComponentType.FLOW_COLUMN -> Icons.Outlined.TableRows
        ComponentType.SPACER -> Icons.Outlined.SpaceBar
    }
}