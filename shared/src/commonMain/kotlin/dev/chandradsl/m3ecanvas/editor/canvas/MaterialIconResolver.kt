package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Standard list of Material icons offered in the properties panel and canvas components.
 */
object MaterialIconResolver {
    val AVAILABLE_MATERIAL_ICONS = listOf(
        "Favorite",
        "Home",
        "Search",
        "Settings",
        "Add",
        "Close",
        "Star",
        "Notifications",
        "Share",
        "Delete",
        "Edit",
        "Info",
        "Menu",
        "Check",
        "ArrowBack",
        "Person",
        "Refresh",
        "Lock",
        "Email",
        "Phone",
        "Send",
        "ThumbUp",
        "PlayArrow",
        "Warning"
    )

    /**
     * Maps a string icon name to its [ImageVector] representation using only Material font icons.
     * Catches any classloading or linkage errors gracefully.
     */
    fun resolve(iconName: String): ImageVector {
        return try {
            when (iconName.trim().lowercase()) {
                "favorite" -> Icons.Outlined.Favorite
                "favoriteborder" -> Icons.Outlined.FavoriteBorder
                "home" -> Icons.Outlined.Home
                "search" -> Icons.Outlined.Search
                "settings" -> Icons.Outlined.Settings
                "add" -> Icons.Outlined.Add
                "close" -> Icons.Outlined.Close
                "star" -> Icons.Outlined.Star
                "notifications" -> Icons.Outlined.Notifications
                "share" -> Icons.Outlined.Share
                "delete" -> Icons.Outlined.Delete
                "edit" -> Icons.Outlined.Edit
                "info" -> Icons.Outlined.Info
                "menu" -> Icons.Outlined.Menu
                "check" -> Icons.Outlined.Check
                "arrowback", "arrow_back" -> Icons.AutoMirrored.Outlined.ArrowBack
                "person" -> Icons.Outlined.Person
                "refresh" -> Icons.Outlined.Refresh
                "lock" -> Icons.Outlined.Lock
                "email", "mail" -> Icons.Outlined.Email
                "phone" -> Icons.Outlined.Phone
                "send" -> Icons.AutoMirrored.Outlined.Send
                "thumbup", "thumb_up" -> Icons.Outlined.ThumbUp
                "playarrow", "play_arrow" -> Icons.Outlined.PlayArrow
                "warning" -> Icons.Outlined.Warning
                else -> Icons.Outlined.Favorite
            }
        } catch (_: Throwable) {
            Icons.Outlined.Favorite
        }
    }
}

val AVAILABLE_MATERIAL_ICONS: List<String>
    get() = MaterialIconResolver.AVAILABLE_MATERIAL_ICONS

fun resolveMaterialIcon(iconName: String): ImageVector {
    return MaterialIconResolver.resolve(iconName)
}
