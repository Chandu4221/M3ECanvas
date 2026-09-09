package dev.chandradsl.m3ecanvas.domain.model

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
 * A named device preset for the design canvas.
 * Mirrors the device profiles provided by Android Studio.
 * All dimensions are in dp, matching Compose's unit system.
 */
data class DeviceProfile(
    val id: String,
    val displayName: String,
    val size: CanvasSize,
    val category: DeviceCategory
) {
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
                category = DeviceCategory.PHONE
            ),
            DeviceProfile(
                id = "pixel_8_pro",
                displayName = "Pixel 8 Pro",
                size = CanvasSize(width = 412f, height = 892f),
                category = DeviceCategory.PHONE
            ),
            DeviceProfile(
                id = "pixel_7a",
                displayName = "Pixel 7a",
                size = CanvasSize(width = 412f, height = 915f),
                category = DeviceCategory.PHONE
            ),
            DeviceProfile(
                id = "pixel_5",
                displayName = "Pixel 5",
                size = CanvasSize(width = 393f, height = 851f),
                category = DeviceCategory.PHONE
            ),

            // Foldable
            DeviceProfile(
                id = "pixel_fold",
                displayName = "Pixel Fold",
                size = CanvasSize(width = 600f, height = 840f),
                category = DeviceCategory.FOLDABLE
            ),

            // Tablets
            DeviceProfile(
                id = "pixel_tablet",
                displayName = "Pixel Tablet",
                size = CanvasSize(width = 1280f, height = 800f),
                category = DeviceCategory.TABLET
            ),
            DeviceProfile(
                id = "pixel_c",
                displayName = "Pixel C",
                size = CanvasSize(width = 1280f, height = 900f),
                category = DeviceCategory.TABLET
            )
        )

        /** The default profile used when creating a new project. */
        val default: DeviceProfile = presets.first()
    }
}