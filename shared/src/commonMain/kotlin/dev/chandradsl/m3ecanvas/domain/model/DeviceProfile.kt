package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Categories of device form factors.
 */
enum class DeviceCategory(val displayName: String) {
    PHONE(displayName = "Phone"),
    FOLDABLE(displayName = "Foldable"),
    TABLET(displayName = "Tablet"),
    DESKTOP(displayName = "Desktop"),
    CUSTOM(displayName = "Custom")
}

/**
 * Screen orientation of the device viewport.
 */
enum class DeviceOrientation(val displayName: String) {
    PORTRAIT(displayName = "Portrait"),
    LANDSCAPE(displayName = "Landscape")
}

/**
 * Standard Material 3 Window Width Size Classes.
 * - Compact: typical phone in portrait (< 600 dp)
 * - Medium: foldables and small tablets (600 dp <= width < 840 dp)
 * - Expanded: tablets, desktops, large foldables (>= 840 dp)
 */
enum class WindowWidthSizeClass(val displayName: String) {
    COMPACT("Compact (<600dp)"),
    MEDIUM("Medium (600–840dp)"),
    EXPANDED("Expanded (>840dp)");

    companion object {
        fun fromWidth(widthDp: Float): WindowWidthSizeClass = when {
            widthDp < 600f -> COMPACT
            widthDp < 840f -> MEDIUM
            else -> EXPANDED
        }
    }
}

/**
 * Standard Material 3 Window Height Size Classes.
 */
enum class WindowHeightSizeClass(val displayName: String) {
    COMPACT("Compact (<480dp)"),
    MEDIUM("Medium (480–900dp)"),
    EXPANDED("Expanded (>900dp)");

    companion object {
        fun fromHeight(heightDp: Float): WindowHeightSizeClass = when {
            heightDp < 480f -> COMPACT
            heightDp < 900f -> MEDIUM
            else -> EXPANDED
        }
    }
}

/**
 * A named device preset for the design canvas.
 * Mirrors the device profiles provided by Android Studio.
 * All dimensions are in dp, matching Compose's unit system.
 */
@Serializable
data class DeviceProfile(
    val id: String,
    val displayName: String,
    val size: CanvasSize,
    val category: DeviceCategory,
    val orientation: DeviceOrientation = DeviceOrientation.PORTRAIT,
    val density: Float = 2.75f,
    val fontScale: Float = 1.0f
) {
    /**
     * Width taking current device size into account.
     */
    val effectiveWidth: Float get() = size.width

    /**
     * Height taking current device size into account.
     */
    val effectiveHeight: Float get() = size.height

    /**
     * Material 3 window width size class computed from the effective width.
     */
    val widthSizeClass: WindowWidthSizeClass
        get() = WindowWidthSizeClass.fromWidth(effectiveWidth)

    /**
     * Material 3 window height size class computed from the effective height.
     */
    val heightSizeClass: WindowHeightSizeClass
        get() = WindowHeightSizeClass.fromHeight(effectiveHeight)

    companion object {
        /**
         * Default device presets, similar to Android Studio's emulator list.
         * Dimensions are in dp and can be extended or customized later.
         */
        val presets: List<DeviceProfile> = listOf(
            // Phones
            DeviceProfile(
                id = "pixel_8",
                displayName = "Pixel 8",
                size = CanvasSize(width = 412f, height = 915f),
                category = DeviceCategory.PHONE,
                orientation = DeviceOrientation.PORTRAIT
            ),
            DeviceProfile(
                id = "pixel_8_pro",
                displayName = "Pixel 8 Pro",
                size = CanvasSize(width = 412f, height = 892f),
                category = DeviceCategory.PHONE,
                orientation = DeviceOrientation.PORTRAIT
            ),
            DeviceProfile(
                id = "pixel_7a",
                displayName = "Pixel 7a",
                size = CanvasSize(width = 412f, height = 915f),
                category = DeviceCategory.PHONE,
                orientation = DeviceOrientation.PORTRAIT
            ),
            DeviceProfile(
                id = "pixel_5",
                displayName = "Pixel 5",
                size = CanvasSize(width = 393f, height = 851f),
                category = DeviceCategory.PHONE,
                orientation = DeviceOrientation.PORTRAIT
            ),

            // Foldable
            DeviceProfile(
                id = "pixel_fold",
                displayName = "Pixel Fold",
                size = CanvasSize(width = 600f, height = 840f),
                category = DeviceCategory.FOLDABLE,
                orientation = DeviceOrientation.PORTRAIT
            ),

            // Tablets
            DeviceProfile(
                id = "pixel_tablet",
                displayName = "Pixel Tablet",
                size = CanvasSize(width = 1280f, height = 800f),
                category = DeviceCategory.TABLET,
                orientation = DeviceOrientation.LANDSCAPE
            ),
            DeviceProfile(
                id = "pixel_c",
                displayName = "Pixel C",
                size = CanvasSize(width = 1280f, height = 900f),
                category = DeviceCategory.TABLET,
                orientation = DeviceOrientation.LANDSCAPE
            ),

            // Desktop
            DeviceProfile(
                id = "desktop_window",
                displayName = "Desktop Window",
                size = CanvasSize(width = 1200f, height = 800f),
                category = DeviceCategory.DESKTOP,
                orientation = DeviceOrientation.LANDSCAPE,
                density = 1.0f
            )
        )

        /** The default profile used when creating a new project. */
        val default: DeviceProfile = presets.first()
    }
}