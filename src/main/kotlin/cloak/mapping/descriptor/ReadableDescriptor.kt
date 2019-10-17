package cloak.mapping.descriptor

fun MethodDescriptor.readableParams(): String {
    return "(" + parameterDescriptors.joinToString(",") { it.toReadable() } + ")"
}


private fun FieldType.toReadable(): String = when (this) {
    is ObjectType -> className
    is ArrayType -> "[${componentType.toReadable()}]"
    else -> readablePrimitivesMap[this] ?: error("Impossible")
}

private val readablePrimitivesMap = mapOf(
    FieldType.Byte to "byte",
    FieldType.Char to "char",
    FieldType.Double to "double",
    FieldType.Float to "float",
    FieldType.Int to "int",
    FieldType.Long to "long",
    FieldType.Short to "short",
    FieldType.Boolean to "boolean"
)