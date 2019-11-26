package cloak.format.descriptor

import cloak.util.getOrKey
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor

fun FieldType.Companion.parsePresentableTypeName(rawTypeName: String): FieldType {
    primitiveStringsToObjects[rawTypeName]?.let { return it }
    if (rawTypeName.endsWith("[]")) return ArrayType(
        parsePresentableTypeName(rawTypeName.substring(0, rawTypeName.length - 2))
    )

    // We trust that if a type doesn't have a package it means it's a generic type
    if ("." !in rawTypeName) return ObjectType("java/lang/Object")

    return ObjectType(rawTypeName.replace(".", "/"))
}


private val primitiveStringsToObjects = mapOf(
    "byte" to PrimitiveType.Byte,
    "char" to PrimitiveType.Char,
    "double" to PrimitiveType.Double,
    "float" to PrimitiveType.Float,
    "int" to PrimitiveType.Int,
    "long" to PrimitiveType.Long,
    "short" to PrimitiveType.Short,
    "boolean" to PrimitiveType.Boolean
)


fun <T : Descriptor> T.remap(map: Map<String, String>): T = when (this) {
    is PrimitiveType, ReturnDescriptor.Void -> this
    is ObjectType -> this.copy(map.getOrKey(className))
    is ArrayType -> this.copy(componentType.remap(map))
    is MethodDescriptor -> this.copy(parameterDescriptors.map { it.remap(map) }, returnDescriptor.remap(map))
    else -> error("Impossible")
} as T

@Serializer(forClass = ParameterDescriptor::class)
object ParameterDescriptorSerializer : KSerializer<ParameterDescriptor> {
    override val descriptor = StringDescriptor

    override fun deserialize(decoder: Decoder) = ParameterDescriptor.read(decoder.decodeString())

    override fun serialize(encoder: Encoder, obj: ParameterDescriptor) {
        encoder.encodeString(obj.classFileName)
    }

}