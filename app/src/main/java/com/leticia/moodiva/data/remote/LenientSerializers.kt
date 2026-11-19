package com.leticia.moodiva.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

// O JSON do Last.fm é convertido do XML original e tem particularidades (planejamento, 3.3).
// Estes serializadores as absorvem nos DTOs, para os mappers trabalharem com tipos simples.

/**
 * Lê um valor primitivo como texto, venha ele entre aspas ou não.
 *
 * Ex.: `"count": 100` (artist.getTopTags) e `"listeners": "194991"` (artist.getTopTracks)
 * viram `"100"` e `"194991"`. A conversão para número fica no mapper, que trata valores ruins.
 */
object LenientStringSerializer : KSerializer<String> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeString()
        return (element as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content.orEmpty()
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

/**
 * Aceita uma lista, um objeto único (o Last.fm troca a lista por um objeto quando há um
 * só item) ou um texto vazio (quando não há itens), sempre devolvendo uma lista.
 */
open class SingleOrListSerializer<T>(element: KSerializer<T>) :
    JsonTransformingSerializer<List<T>>(ListSerializer(element)) {

    override fun transformDeserialize(element: JsonElement): JsonElement = when (element) {
        is JsonArray -> element
        is JsonObject -> JsonArray(listOf(element))
        else -> JsonArray(emptyList())
    }
}

/** Aceita um objeto ou, no lugar dele, um texto vazio (devolvido como objeto vazio). */
open class ObjectOrEmptySerializer<T : Any>(serializer: KSerializer<T>) :
    JsonTransformingSerializer<T>(serializer) {

    override fun transformDeserialize(element: JsonElement): JsonElement =
        element as? JsonObject ?: JsonObject(emptyMap())
}
