package com.denham.skyline.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Xtream panels are wildly inconsistent about JSON types: booleans arrive as
 * 0/1, "0"/"1", "true"/false; numbers arrive as strings; objects arrive as
 * empty arrays when there is no data. Every serializer here tolerates all of
 * those shapes instead of crashing on the first non-conformant provider.
 */
val XtreamJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}

private fun Decoder.element(): JsonElement =
    (this as JsonDecoder).decodeJsonElement()

/** Accepts true/false, 0/1, "0"/"1", "true"/"false"; null/absent -> false. */
object FlexBool : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("xtream.FlexBool", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean =
        when (val el = decoder.element()) {
            is JsonPrimitive -> el.booleanOrNull
                ?: el.intOrNull?.let { it != 0 }
                ?: el.content.equals("true", ignoreCase = true)
                        || el.content.trim() == "1"
            else -> false
        }

    override fun serialize(encoder: Encoder, value: Boolean) =
        encoder.encodeBoolean(value)
}

/** Accepts int, "123", 123.0; null/garbage -> 0. */
object FlexInt : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("xtream.FlexInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int =
        when (val el = decoder.element()) {
            is JsonPrimitive -> el.intOrNull
                ?: el.content.trim().toDoubleOrNull()?.toInt()
                ?: 0
            else -> 0
        }

    override fun serialize(encoder: Encoder, value: Int) =
        encoder.encodeInt(value)
}

/** Accepts long, "1735689600"; null/""/garbage -> null (e.g. unlimited exp_date). */
object FlexLongOrNull : KSerializer<Long?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("xtream.FlexLongOrNull", PrimitiveKind.LONG).nullable

    override fun deserialize(decoder: Decoder): Long? =
        when (val el = decoder.element()) {
            is JsonPrimitive -> el.longOrNull
                ?: el.content.trim().toDoubleOrNull()?.toLong()
            else -> null
        }

    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
    }
}

/** Accepts double, "4.5", int; null/garbage -> 0.0. */
object FlexDouble : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("xtream.FlexDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double =
        when (val el = decoder.element()) {
            is JsonPrimitive -> el.doubleOrNull ?: 0.0
            else -> 0.0
        }

    override fun serialize(encoder: Encoder, value: Double) =
        encoder.encodeDouble(value)
}

/** Accepts string or number; null -> "". Used for ids/ports that flip types. */
object FlexString : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("xtream.FlexString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String =
        when (val el = decoder.element()) {
            is JsonPrimitive -> if (el is JsonNull) "" else el.content
            else -> ""
        }

    override fun serialize(encoder: Encoder, value: String) =
        encoder.encodeString(value)
}

/** Accepts ["a","b"], "a", or null -> list. Used for backdrop_path etc. */
object FlexStringList : KSerializer<List<String>> {
    private val listSerializer = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<String> =
        when (val el = decoder.element()) {
            is JsonArray -> el.mapNotNull { (it as? JsonPrimitive)?.content }
            is JsonPrimitive -> if (el is JsonNull || el.content.isBlank()) emptyList() else listOf(el.content)
            else -> emptyList()
        }

    override fun serialize(encoder: Encoder, value: List<String>) =
        listSerializer.serialize(encoder, value)
}

/**
 * Tolerates `"info": []` / `"info": null` where an object is expected — a
 * real crash cause on some panels. Decodes the object when present,
 * otherwise returns null.
 */
class ObjectOrNull<T : Any>(private val dataSerializer: KSerializer<T>) : KSerializer<T?> {
    override val descriptor: SerialDescriptor = dataSerializer.descriptor.nullable

    override fun deserialize(decoder: Decoder): T? {
        val input = decoder as JsonDecoder
        return when (val el = input.decodeJsonElement()) {
            is JsonObject -> runCatching {
                input.json.decodeFromJsonElement(dataSerializer, el)
            }.getOrNull()
            else -> null // JsonArray (usually empty), JsonNull, primitives
        }
    }

    override fun serialize(encoder: Encoder, value: T?) {
        val out = encoder as JsonEncoder
        if (value == null) out.encodeJsonElement(JsonNull)
        else out.encodeSerializableValue(dataSerializer, value)
    }
}
