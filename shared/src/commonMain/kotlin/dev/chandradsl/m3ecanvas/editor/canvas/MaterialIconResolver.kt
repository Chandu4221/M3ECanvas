package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chandradsl.m3ecanvas.domain.model.IconReference
import dev.chandradsl.m3ecanvas.domain.model.IconStyle
import dev.chandradsl.m3ecanvas.util.AppLogger

/**
 * Metadata for icons in the catalog, allowing rich search and categorization in IconPicker.
 */
data class IconCatalogEntry(
    val name: String,
    val category: String,
    val keywords: List<String> = emptyList()
)

val ICON_CATALOG: List<IconCatalogEntry> = listOf(
    IconCatalogEntry("Favorite", "Actions", listOf("heart", "like", "love")),
    IconCatalogEntry("Star", "Actions", listOf("bookmark", "rate", "favorite")),
    IconCatalogEntry("Share", "Actions", listOf("send", "export", "forward")),
    IconCatalogEntry("Delete", "Actions", listOf("trash", "remove", "bin")),
    IconCatalogEntry("Edit", "Actions", listOf("pencil", "modify", "write")),
    IconCatalogEntry("Add", "Actions", listOf("plus", "create", "new")),
    IconCatalogEntry("Close", "Actions", listOf("cancel", "dismiss", "x")),
    IconCatalogEntry("Check", "Actions", listOf("tick", "ok", "confirm", "done")),
    IconCatalogEntry("Refresh", "Actions", listOf("reload", "sync", "update")),
    IconCatalogEntry("ThumbUp", "Actions", listOf("like", "upvote", "approve")),

    IconCatalogEntry("Home", "Navigation", listOf("main", "dashboard", "root")),
    IconCatalogEntry("Search", "Navigation", listOf("find", "magnifier", "query")),
    IconCatalogEntry("Menu", "Navigation", listOf("hamburger", "drawer", "options")),
    IconCatalogEntry("ArrowBack", "Navigation", listOf("back", "return", "previous")),

    IconCatalogEntry("Notifications", "Communication", listOf("bell", "alert", "notice")),
    IconCatalogEntry("Email", "Communication", listOf("mail", "letter", "inbox")),
    IconCatalogEntry("Phone", "Communication", listOf("call", "telephone", "contact")),
    IconCatalogEntry("Send", "Communication", listOf("message", "transmit")),

    IconCatalogEntry("Person", "Social", listOf("user", "account", "profile")),
    IconCatalogEntry("Settings", "System", listOf("gear", "preferences", "config")),
    IconCatalogEntry("Lock", "System", listOf("security", "password", "privacy")),
    IconCatalogEntry("Info", "System", listOf("help", "about", "details")),
    IconCatalogEntry("Warning", "System", listOf("alert", "caution", "error")),
    IconCatalogEntry("PlayArrow", "Media", listOf("play", "start", "video"))
)

val DEFAULT_AVAILABLE_MATERIAL_ICONS = ICON_CATALOG.map { it.name }

/**
 * Port interface for resolving an [IconReference] or icon name to a rendered [ImageVector].
 * Adheres to Section 31 of the specification.
 */
interface IconResolver {
    fun resolve(reference: IconReference): ImageVector
    fun resolve(iconName: String): ImageVector
    fun resolve(iconName: String, style: IconStyle): ImageVector
}

/**
 * Material design icon resolver implementing [IconResolver].
 * Resolves [IconReference] dynamically taking style (Outlined vs Filled) into account.
 */
open class MaterialIconResolver(
    val availableIcons: List<String> = DEFAULT_AVAILABLE_MATERIAL_ICONS
) : IconResolver {

    override fun resolve(reference: IconReference): ImageVector {
        return resolve(iconName = reference.name, style = reference.style)
    }

    override fun resolve(iconName: String): ImageVector {
        return resolve(iconName = iconName, style = IconStyle.OUTLINED)
    }

    override fun resolve(iconName: String, style: IconStyle): ImageVector {
        return try {
            val key = iconName.trim().lowercase()
            when (style) {
                IconStyle.FILLED -> resolveFilled(key)
                else -> resolveOutlined(key)
            }
        } catch (t: Throwable) {
            AppLogger.error(
                "MaterialIconResolver",
                "Failed to resolve icon '$iconName' with style '$style', falling back to Outlined.Favorite",
                t
            )
            Icons.Outlined.Favorite
        }
    }

    private fun resolveOutlined(key: String): ImageVector {
        return when (key) {
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
            else -> {
                AppLogger.warn(
                    "MaterialIconResolver",
                    "Unknown outlined icon name '$key', falling back to Favorite"
                )
                Icons.Outlined.Favorite
            }
        }
    }

    private fun resolveFilled(key: String): ImageVector {
        return when (key) {
            "favorite" -> Icons.Filled.Favorite
            "home" -> Icons.Filled.Home
            "search" -> Icons.Filled.Search
            "settings" -> Icons.Filled.Settings
            "add" -> Icons.Filled.Add
            "close" -> Icons.Filled.Close
            "star" -> Icons.Filled.Star
            "notifications" -> Icons.Filled.Notifications
            "share" -> Icons.Filled.Share
            "delete" -> Icons.Filled.Delete
            "edit" -> Icons.Filled.Edit
            "info" -> Icons.Filled.Info
            "menu" -> Icons.Filled.Menu
            "check" -> Icons.Filled.Check
            "arrowback", "arrow_back" -> Icons.AutoMirrored.Outlined.ArrowBack
            "person" -> Icons.Filled.Person
            "refresh" -> Icons.Filled.Refresh
            "lock" -> Icons.Filled.Lock
            "email", "mail" -> Icons.Filled.Email
            "phone" -> Icons.Filled.Phone
            "send" -> Icons.AutoMirrored.Outlined.Send
            "thumbup", "thumb_up" -> Icons.Filled.ThumbUp
            "playarrow", "play_arrow" -> Icons.Filled.PlayArrow
            "warning" -> Icons.Filled.Warning
            else -> {
                AppLogger.warn(
                    "MaterialIconResolver",
                    "Unknown filled icon name '$key', falling back to Filled.Favorite"
                )
                Icons.Filled.Favorite
            }
        }
    }

    companion object {
        val default: MaterialIconResolver by lazy { MaterialIconResolver() }
        val AVAILABLE_MATERIAL_ICONS = DEFAULT_AVAILABLE_MATERIAL_ICONS

        fun resolve(iconName: String): ImageVector = default.resolve(iconName)
        fun resolve(reference: IconReference): ImageVector = default.resolve(reference)
    }
}

val AVAILABLE_MATERIAL_ICONS: List<String>
    get() = MaterialIconResolver.AVAILABLE_MATERIAL_ICONS

fun resolveMaterialIcon(iconName: String): ImageVector {
    return MaterialIconResolver.resolve(iconName)
}

fun resolveMaterialIcon(reference: IconReference): ImageVector {
    return MaterialIconResolver.resolve(reference)
}
