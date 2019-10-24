package cloak.mapping.descriptor

import cloak.mapping.getOrKey

fun FieldType.Companion.parsePresentableTypeName(rawTypeName: String, isGenericType: Boolean): FieldType {
    if (isGenericType) return ObjectType("java/lang/Object")
    primitiveStringsToObjects[rawTypeName]?.let { return it }
    if (rawTypeName.endsWith("[]")) return ArrayType(
        parsePresentableTypeName(
            rawTypeName.substring(0, rawTypeName.length - 2),
            //TODO: actually determine if it's generic
            isGenericType = false
        )
    )

    return ObjectType(rawTypeName)
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