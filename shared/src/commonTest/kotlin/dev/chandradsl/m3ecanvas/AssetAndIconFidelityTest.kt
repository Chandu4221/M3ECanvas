package dev.chandradsl.m3ecanvas

import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.asset.DefaultAssetRepository
import dev.chandradsl.m3ecanvas.editor.canvas.MaterialIconResolver
import dev.chandradsl.m3ecanvas.editor.canvas.resolveMaterialIcon
import dev.chandradsl.m3ecanvas.editor.codegen.ComposeCodeGenerator
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssetAndIconFidelityTest {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    @Test
    fun testIconReferenceCreationAndSerialization() {
        val iconRef = IconReference(
            name = "Notifications",
            source = IconSource.MATERIAL,
            family = IconFamily.MATERIAL_DESIGN,
            style = IconStyle.FILLED,
            metadata = mapOf("badge" to "true")
        )

        assertEquals("Notifications", iconRef.name)
        assertEquals(IconSource.MATERIAL, iconRef.source)
        assertEquals(IconFamily.MATERIAL_DESIGN, iconRef.family)
        assertEquals(IconStyle.FILLED, iconRef.style)
        assertEquals("true", iconRef.metadata["badge"])

        val encoded = json.encodeToString(IconReference.serializer(), iconRef)
        val decoded = json.decodeFromString(IconReference.serializer(), encoded)
        assertEquals(iconRef, decoded)
    }

    @Test
    fun testIconPropertyBackwardCompatibility() {
        // Test constructing with legacy iconName
        val legacyProp = ComponentProperty.Icon(key = "icon", iconName = "Search")
        assertEquals("Search", legacyProp.iconName)
        assertEquals("Search", legacyProp.iconRef.name)
        assertEquals(IconStyle.OUTLINED, legacyProp.iconRef.style)

        // Test constructing with modern IconReference
        val refProp = ComponentProperty.Icon(
            key = "icon",
            reference = IconReference(name = "Home", style = IconStyle.FILLED)
        )
        assertEquals("Home", refProp.iconName)
        assertEquals("Home", refProp.iconRef.name)
        assertEquals(IconStyle.FILLED, refProp.iconRef.style)

        // Roundtrip serialization with ComponentProperty
        val encoded = json.encodeToString(ComponentProperty.serializer(), legacyProp)
        val decoded = json.decodeFromString(ComponentProperty.serializer(), encoded)
        assertTrue(decoded is ComponentProperty.Icon)
        assertEquals("Search", decoded.iconName)
        assertEquals("Search", decoded.iconRef.name)
    }

    @Test
    fun testParameterValueIconValAndAssetValConversion() {
        val iconProp = ComponentProperty.Icon(key = "icon", iconName = "Star")
        val iconParam = iconProp.toParameter()
        assertTrue(iconParam.value is ParameterValue.IconVal)
        assertEquals("Star", (iconParam.value as ParameterValue.IconVal).iconName)
        assertEquals("Star", (iconParam.value as ParameterValue.IconVal).iconRef.name)

        val backToProp = iconParam.toProperty()
        assertTrue(backToProp is ComponentProperty.Icon)
        assertEquals("Star", backToProp.iconName)

        val assetRef = AssetReference(
            assetId = "sample_hero_landscape",
            source = AssetSourceType.BUNDLED,
            scale = ContentScaleType.CROP,
            alignment = AssetAlignment.CENTER
        )
        val assetProp = ComponentProperty.Asset(key = "asset", reference = assetRef)
        val assetParam = assetProp.toParameter()
        assertTrue(assetParam.value is ParameterValue.AssetVal)
        assertEquals("sample_hero_landscape", (assetParam.value as ParameterValue.AssetVal).assetId)

        val backAssetProp = assetParam.toProperty()
        assertTrue(backAssetProp is ComponentProperty.Asset)
        assertEquals("sample_hero_landscape", backAssetProp.assetId)
        assertEquals(ContentScaleType.CROP, backAssetProp.assetRef.scale)
    }

    @Test
    fun testMaterialIconResolverAcrossStyles() {
        val resolver = MaterialIconResolver.default

        // Outlined resolution
        val outlinedFavorite = resolver.resolve(IconReference.material("Favorite", IconStyle.OUTLINED))
        assertNotNull(outlinedFavorite)

        // Filled resolution
        val filledFavorite = resolver.resolve(IconReference.material("Favorite", IconStyle.FILLED))
        assertNotNull(filledFavorite)

        // Unknown icon fallback should never throw
        val unknown = resolver.resolve(IconReference.material("completely_unknown_icon_xyz"))
        assertNotNull(unknown)

        // Legacy string resolution
        val legacy = resolveMaterialIcon("Settings")
        assertNotNull(legacy)
    }

    @Test
    fun testAssetReferenceFidelityAndProjectIntegration() {
        val asset = AssetReference(
            assetId = "custom_banner_01",
            source = AssetSourceType.LOCAL,
            uri = "file:///path/to/banner.png",
            mimeType = "image/png",
            dimensions = AssetDimensions(width = 1920f, height = 1080f),
            metadata = mapOf("author" to "Designer A", "dpi" to "300"),
            crop = AssetCrop(left = 0.1f, top = 0.1f, right = 0.9f, bottom = 0.9f),
            scale = ContentScaleType.FIT,
            alignment = AssetAlignment.TOP_CENTER
        )

        assertEquals("custom_banner_01", asset.assetId)
        assertEquals(AssetSourceType.LOCAL, asset.source)
        assertEquals(1920f, asset.dimensions?.width)
        assertEquals(1080f, asset.dimensions?.height)
        assertEquals(0.1f, asset.crop?.left)
        assertEquals(ContentScaleType.FIT, asset.scale)
        assertEquals(AssetAlignment.TOP_CENTER, asset.alignment)

        // Project asset management
        var project = M3EProject(name = "AssetTestProject")
        assertTrue(project.assets.isEmpty())

        project = project.withAsset(asset)
        assertEquals(1, project.assets.size)
        assertEquals(asset, project.findAsset("custom_banner_01"))

        // Update asset
        val updatedAsset = asset.copy(scale = ContentScaleType.CROP)
        project = project.withAsset(updatedAsset)
        assertEquals(1, project.assets.size)
        assertEquals(ContentScaleType.CROP, project.findAsset("custom_banner_01")?.scale)

        // Remove asset
        project = project.removeAsset("custom_banner_01")
        assertEquals(0, project.assets.size)
        assertNull(project.findAsset("custom_banner_01"))
    }

    @Test
    fun testAssetRepositoryBundledSamples() {
        val repo = DefaultAssetRepository.default
        val assets = repo.listAssets()
        assertTrue(assets.isNotEmpty())

        val hero = repo.getAsset("sample_hero_landscape")
        assertNotNull(hero)
        assertEquals(AssetSourceType.BUNDLED, hero.source)
        assertEquals(ContentScaleType.CROP, hero.scale)

        val avatar = repo.getAsset("sample_avatar_user")
        assertNotNull(avatar)
        assertEquals(AssetSourceType.BUNDLED, avatar.source)
    }

    @Test
    fun testComposeCodeGeneratorProducesRealImageComposables() {
        val imageNode = CanvasNode(
            name = "HeroImage",
            type = ComponentType.IMAGE,
            position = CanvasPosition(0f, 0f),
            size = CanvasSize(200f, 200f),
            properties = listOf(
                ComponentProperty.Asset(
                    key = "asset",
                    reference = AssetReference(
                        assetId = "sample_hero_landscape",
                        source = AssetSourceType.BUNDLED,
                        scale = ContentScaleType.CROP,
                        alignment = AssetAlignment.CENTER
                    )
                )
            )
        )

        val project = M3EProject(name = "ImageGenProject", nodes = listOf(imageNode))
        val codeGen = ComposeCodeGenerator()
        val generated = codeGen.generateFile(project)

        // Verify that it emits real Image composable with painter and ContentScale
        assertTrue(generated.contains("Image("), "Generated code must include Image(...)")
        assertTrue(generated.contains("painter = painterResource(\"assets/sample_hero_landscape.png\")"))
        assertTrue(generated.contains("contentScale = ContentScale.Crop"))
        assertTrue(generated.contains("alignment = Alignment.Center"))
        // MUST NOT contain placeholder Surface with placeholder text!
        assertTrue(!generated.contains("Image placeholder"), "Must not generate placeholder Surface")
    }

    @Test
    fun testComposeCodeGeneratorHandlesRemoteAssets() {
        val remoteImageNode = CanvasNode(
            name = "RemoteLogo",
            type = ComponentType.IMAGE,
            position = CanvasPosition(0f, 0f),
            size = CanvasSize(200f, 200f),
            properties = listOf(
                ComponentProperty.Asset(
                    key = "asset",
                    reference = AssetReference(
                        assetId = "remote_logo_id",
                        source = AssetSourceType.REMOTE,
                        uri = "https://example.com/logo.png",
                        scale = ContentScaleType.FIT,
                        alignment = AssetAlignment.TOP_START
                    )
                )
            )
        )

        val project = M3EProject(name = "RemoteGenProject", nodes = listOf(remoteImageNode))
        val codeGen = ComposeCodeGenerator()
        val generated = codeGen.generateFile(project)

        assertTrue(generated.contains("rememberAsyncImagePainter(\"https://example.com/logo.png\")"))
        assertTrue(generated.contains("contentScale = ContentScale.Fit"))
        assertTrue(generated.contains("alignment = Alignment.TopStart"))
    }

    @Test
    fun testComposeCodeGeneratorIconStyleFidelity() {
        val filledIconNode = CanvasNode(
            name = "FilledStar",
            type = ComponentType.ICON,
            position = CanvasPosition(0f, 0f),
            size = CanvasSize(48f, 48f),
            properties = listOf(
                ComponentProperty.Icon(
                    key = "icon",
                    reference = IconReference(name = "Star", style = IconStyle.FILLED)
                )
            )
        )

        val project = M3EProject(name = "IconGenProject", nodes = listOf(filledIconNode))
        val codeGen = ComposeCodeGenerator()
        val generated = codeGen.generateFile(project)

        assertTrue(generated.contains("Icons.Filled.Star"), "Filled style must emit Icons.Filled.Star")
    }
}
