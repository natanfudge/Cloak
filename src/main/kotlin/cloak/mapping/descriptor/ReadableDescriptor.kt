package cloak.mapping.descriptor

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

//fun MethodDescriptor.readableParams(): String {
//    return "(" + parameterDescriptors.joinToString(",") { it.toReadable() } + ")"
//}


//private fun FieldType.toReadable(): String = when (this) {
//    is ObjectType -> className
//    is ArrayType -> "[${componentType.toReadable()}]"
//    else -> readablePrimitivesMap[this] ?: error("Impossible")
//}
//
//private val readablePrimitivesMap = mapOf(
//    FieldType.Byte to "byte",
//    FieldType.Char to "char",
//    FieldType.Double to "double",
//    FieldType.Float to "float",
//    FieldType.Int to "int",
//    FieldType.Long to "long",
//    FieldType.Short to "short",
//    FieldType.Boolean to "boolean"
//)

private val primitiveStringsToObjects = mapOf(
    "byte" to FieldType.Byte,
    "char" to FieldType.Char,
    "double" to FieldType.Double,
    "float" to FieldType.Float,
    "int" to FieldType.Int,
    "long" to FieldType.Long,
    "short" to FieldType.Short,
    "boolean" to FieldType.Boolean
)