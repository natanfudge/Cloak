package cloak.format.mappings

fun Mapping.typeName() = when (this) {
    is ClassMapping -> "class"
    is MethodMapping -> "method"
    is FieldMapping -> "field"
    is ParameterMapping -> "parameter"
}

const val MappingsExtension = ".mapping"
const val ConstructorName = "<init>"
val MethodMapping.isConstructor get() = obfuscatedName == ConstructorName

private fun String.lastPart() = split("/").last()
private fun ClassMapping.shortName() = toString().lastPart()

fun Mapping.readableName(): String = when (this) {
    is ClassMapping -> this.toString()
    is MethodMapping -> "${parent.shortName()}${Joiner.Method}$nonNullName" +
            "(${descriptor.parameterDescriptors.joinToString(" ,") { it.toString().lastPart() }})"
    is FieldMapping -> "${parent.shortName()}${Joiner.Field}$nonNullName"
    is ParameterMapping -> "${parent.readableName()}[$index = $nonNullName]"
}

 fun Mapping.getFilePath() = (root.deobfuscatedName ?: root.obfuscatedName) + ".mapping"