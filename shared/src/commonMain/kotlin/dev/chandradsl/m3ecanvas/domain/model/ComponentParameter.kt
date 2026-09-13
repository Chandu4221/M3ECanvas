package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Strongly-typed value representation for component parameters.
 * Supports primitives, measurements (dp, sp), theme token references,
 * enums, icons, variants, and explicit UNSET state.
 */
@Serializable
sealed interface ParameterValue {

    /**
     * Represents an unconfigured/unset parameter.
     * When generating code, parameters with this value are omitted,
     * permitting Compose runtime default values to be used.
     */
    @Serializable
    data object Unset : ParameterValue

    @Serializable
    data class StringVal(val value: String) : ParameterValue

    @Serializable
    data class BooleanVal(val value: Boolean) : ParameterValue

    @Serializable
    data class IntVal(val value: Int) : ParameterValue

    @Serializable
    data class LongVal(val value: Long) : ParameterValue

    @Serializable
    data class FloatVal(val value: Float) : ParameterValue

    @Serializable
    data class DoubleVal(val value: Double) : ParameterValue

    @Serializable
    data class DpVal(val value: Float) : ParameterValue

    @Serializable
    data class SpVal(val value: Float) : ParameterValue

    @Serializable
    data class ColorVal(val hex: String) : ParameterValue

    @Serializable
    data class ColorSchemeRef(val tokenName: String) : ParameterValue

    @Serializable
    data class TypographyRef(val tokenName: String) : ParameterValue

    @Serializable
    data class EnumVal(val value: String, val enumType: String) : ParameterValue

    @Serializable
    data class VariantVal(val variant: MaterialVariant) : ParameterValue

    @Serializable
    data class IconVal(val iconName: String) : ParameterValue

    @Serializable
    data class ShapeVal(val cornerRadiusDp: Float) : ParameterValue

    @Serializable
    data class ListVal(val items: List<ParameterValue>) : ParameterValue
}

/**
 * A typed parameter on a [CanvasNode].
 *
 * Distinguishes between:
 * - [isUnset]: parameter has not been modified and should use API default
 * - [isExplicitDefault]: user explicitly configured the parameter to its default value
 */
@Serializable
data class ComponentParameter(
    val name: String,
    val value: ParameterValue = ParameterValue.Unset,
    val isExplicitDefault: Boolean = false
) {
    val isUnset: Boolean get() = value is ParameterValue.Unset
}

/**
 * Conversion extensions between legacy [ComponentProperty] and modern [ComponentParameter].
 */
fun ComponentProperty.toParameter(): ComponentParameter {
    val paramValue: ParameterValue = when (this) {
        is ComponentProperty.Text -> ParameterValue.StringVal(value)
        is ComponentProperty.BooleanFlag -> ParameterValue.BooleanVal(value)
        is ComponentProperty.Numeric -> ParameterValue.FloatVal(value)
        is ComponentProperty.ColorHex -> ParameterValue.ColorVal(value)
        is ComponentProperty.Variant -> ParameterValue.VariantVal(value)
        is ComponentProperty.Icon -> ParameterValue.IconVal(iconName)
    }
    return ComponentParameter(name = key, value = paramValue)
}

fun ComponentParameter.toProperty(): ComponentProperty? {
    return when (val v = value) {
        is ParameterValue.StringVal -> ComponentProperty.Text(key = name, value = v.value)
        is ParameterValue.BooleanVal -> ComponentProperty.BooleanFlag(key = name, value = v.value)
        is ParameterValue.FloatVal -> ComponentProperty.Numeric(key = name, value = v.value)
        is ParameterValue.IntVal -> ComponentProperty.Numeric(key = name, value = v.value.toFloat())
        is ParameterValue.DpVal -> ComponentProperty.Numeric(key = name, value = v.value)
        is ParameterValue.SpVal -> ComponentProperty.Numeric(key = name, value = v.value)
        is ParameterValue.ColorVal -> ComponentProperty.ColorHex(key = name, value = v.hex)
        is ParameterValue.VariantVal -> ComponentProperty.Variant(key = name, value = v.variant)
        is ParameterValue.IconVal -> ComponentProperty.Icon(key = name, iconName = v.iconName)
        else -> null
    }
}
