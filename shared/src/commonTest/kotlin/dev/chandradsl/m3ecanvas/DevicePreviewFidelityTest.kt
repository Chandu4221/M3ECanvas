package dev.chandradsl.m3ecanvas

import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import kotlin.test.*

class DevicePreviewFidelityTest {

    @Test
    fun testWindowWidthSizeClassBoundaries() {
        assertEquals(WindowWidthSizeClass.COMPACT, WindowWidthSizeClass.fromWidth(0f))
        assertEquals(WindowWidthSizeClass.COMPACT, WindowWidthSizeClass.fromWidth(412f))
        assertEquals(WindowWidthSizeClass.COMPACT, WindowWidthSizeClass.fromWidth(599.9f))

        assertEquals(WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.fromWidth(600f))
        assertEquals(WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.fromWidth(720f))
        assertEquals(WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.fromWidth(839.9f))

        assertEquals(WindowWidthSizeClass.EXPANDED, WindowWidthSizeClass.fromWidth(840f))
        assertEquals(WindowWidthSizeClass.EXPANDED, WindowWidthSizeClass.fromWidth(1200f))
        assertEquals(WindowWidthSizeClass.EXPANDED, WindowWidthSizeClass.fromWidth(2560f))
    }

    @Test
    fun testWindowHeightSizeClassBoundaries() {
        assertEquals(WindowHeightSizeClass.COMPACT, WindowHeightSizeClass.fromHeight(400f))
        assertEquals(WindowHeightSizeClass.COMPACT, WindowHeightSizeClass.fromHeight(479.9f))

        assertEquals(WindowHeightSizeClass.MEDIUM, WindowHeightSizeClass.fromHeight(480f))
        assertEquals(WindowHeightSizeClass.MEDIUM, WindowHeightSizeClass.fromHeight(800f))
        assertEquals(WindowHeightSizeClass.MEDIUM, WindowHeightSizeClass.fromHeight(899.9f))

        assertEquals(WindowHeightSizeClass.EXPANDED, WindowHeightSizeClass.fromHeight(900f))
        assertEquals(WindowHeightSizeClass.EXPANDED, WindowHeightSizeClass.fromHeight(1600f))
    }

    @Test
    fun testDeviceOrientationFlipping() {
        val portraitProfile = DeviceProfile(
            id = "test_device",
            displayName = "Test Device",
            size = CanvasSize(width = 412f, height = 915f),
            category = DeviceCategory.PHONE,
            orientation = DeviceOrientation.PORTRAIT
        )

        val controller = EditorController()
        controller.setDeviceProfile(portraitProfile)
        assertEquals(412f, controller.state.project.deviceProfile.effectiveWidth)
        assertEquals(915f, controller.state.project.deviceProfile.effectiveHeight)
        assertEquals(WindowWidthSizeClass.COMPACT, controller.state.project.deviceProfile.widthSizeClass)

        controller.toggleDeviceOrientation()
        val landscapeProfile = controller.state.project.deviceProfile
        assertEquals(915f, landscapeProfile.effectiveWidth)
        assertEquals(412f, landscapeProfile.effectiveHeight)
        assertEquals(DeviceOrientation.LANDSCAPE, landscapeProfile.orientation)
        assertEquals(WindowWidthSizeClass.EXPANDED, landscapeProfile.widthSizeClass)
    }

    @Test
    fun testViewportResizingAndClamping() {
        val controller = EditorController()

        // Below minimum 300dp width & 400dp height
        controller.resizeViewport(widthDp = 150f, heightDp = 200f)
        val clampedSmall = controller.state.project.deviceProfile
        assertEquals(300f, clampedSmall.size.width)
        assertEquals(400f, clampedSmall.size.height)
        assertEquals(DeviceCategory.CUSTOM, clampedSmall.category)

        // Above maximum 1200dp width & 1400dp height
        controller.resizeViewport(widthDp = 1800f, heightDp = 2000f)
        val clampedLarge = controller.state.project.deviceProfile
        assertEquals(1200f, clampedLarge.size.width)
        assertEquals(1400f, clampedLarge.size.height)
        assertEquals(DeviceCategory.CUSTOM, clampedLarge.category)

        // Within valid range
        controller.resizeViewport(widthDp = 720f, heightDp = 900f)
        val mid = controller.state.project.deviceProfile
        assertEquals(720f, mid.size.width)
        assertEquals(900f, mid.size.height)
        assertEquals(WindowWidthSizeClass.MEDIUM, mid.widthSizeClass)
    }

    @Test
    fun testWindowSizePresetSwitching() {
        val controller = EditorController()

        controller.setWindowSizePreset(WindowWidthSizeClass.COMPACT)
        assertEquals(WindowWidthSizeClass.COMPACT, controller.state.project.deviceProfile.widthSizeClass)

        controller.setWindowSizePreset(WindowWidthSizeClass.MEDIUM)
        assertEquals(WindowWidthSizeClass.MEDIUM, controller.state.project.deviceProfile.widthSizeClass)

        controller.setWindowSizePreset(WindowWidthSizeClass.EXPANDED)
        assertEquals(WindowWidthSizeClass.EXPANDED, controller.state.project.deviceProfile.widthSizeClass)
    }

    @Test
    fun testComponentParameterUnsetVsExplicit() {
        val unsetParam = ComponentParameter(name = "elevation")
        assertTrue(unsetParam.isUnset)
        assertEquals(ParameterValue.Unset, unsetParam.value)

        val explicitParam = ComponentParameter(
            name = "elevation",
            value = ParameterValue.DpVal(8f),
            isExplicitDefault = true
        )
        assertFalse(explicitParam.isUnset)
        assertTrue(explicitParam.isExplicitDefault)
        assertEquals(ParameterValue.DpVal(8f), explicitParam.value)
    }

    @Test
    fun testComponentPropertyBidirectionalConversion() {
        val textProp = ComponentProperty.Text("text", "Submit")
        val textParam = textProp.toParameter()
        assertEquals("text", textParam.name)
        assertEquals(ParameterValue.StringVal("Submit"), textParam.value)
        assertEquals(textProp, textParam.toProperty())

        val boolProp = ComponentProperty.BooleanFlag("enabled", true)
        val boolParam = boolProp.toParameter()
        assertEquals(ParameterValue.BooleanVal(true), boolParam.value)
        assertEquals(boolProp, boolParam.toProperty())

        val numProp = ComponentProperty.Numeric("alpha", 0.8f)
        val numParam = numProp.toParameter()
        assertEquals(ParameterValue.FloatVal(0.8f), numParam.value)
        assertEquals(numProp, numParam.toProperty())

        val colorProp = ComponentProperty.ColorHex("color", "#FF6200EE")
        val colorParam = colorProp.toParameter()
        assertEquals(ParameterValue.ColorVal("#FF6200EE"), colorParam.value)
        assertEquals(colorProp, colorParam.toProperty())

        val variantProp = ComponentProperty.Variant("variant", MaterialVariant.Button.ELEVATED)
        val variantParam = variantProp.toParameter()
        assertEquals(ParameterValue.VariantVal(MaterialVariant.Button.ELEVATED), variantParam.value)
        assertEquals(variantProp, variantParam.toProperty())

        val iconProp = ComponentProperty.Icon("icon", "Add")
        val iconParam = iconProp.toParameter()
        assertEquals(ParameterValue.IconVal("Add"), iconParam.value)
        assertEquals(iconProp, iconParam.toProperty())
    }
}
