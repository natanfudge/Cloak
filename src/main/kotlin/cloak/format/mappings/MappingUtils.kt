package cloak.format.mappings

import cloak.format.rename.flattenWithSelf

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


fun ClassMapping.fullObfName() =
    flattenWithSelf(this) { parent }.reversed().joinToString("$") { it.obfuscatedName }

fun Mapping.obfNameInTinyMappings(): String = when (this) {
    is ClassMapping -> fullObfName()
    else -> obfuscatedName
}

fun ClassMapping.fullDeobfName(): String? {
    val chain = flattenWithSelf(this) { parent }
    if (chain.any { it.deobfuscatedName == null }) return null
    return chain.reversed().joinToString("$") { it.deobfuscatedName!! }
}

fun Mapping.deobfNameInTinyMappings(): String? = when (this) {
    is ClassMapping -> fullDeobfName()
    else -> obfuscatedName
}

fun Mapping.readableName(): String = when (this) {
    is ClassMapping -> this.toString()
    is MethodMapping -> "${parent.shortName()}${Joiner.Method}$displayedName" +
            "(${descriptor.parameterDescriptors.size})"
    is FieldMapping -> "${parent.shortName()}${Joiner.Field}$displayedName"
    is ParameterMapping -> "${parent.readableName()}[$index = $displayedName]"
}

val Mapping.displayedName get() = deobfuscatedName ?: obfuscatedName

fun List<Mapping>.anythingElseHasTheSameObfNameAs(mapping: Mapping) = any {
    it !== mapping && it.deobfuscatedName == mapping.deobfuscatedName
}

fun Mapping.getFilePath() = (root.deobfuscatedName ?: root.obfuscatedName) + ".mapping"

val Mapping.multilineComment get() = comment.joinToString("\n")

val Mapping.children: List<Mapping>
    get() = when (this) {
        is ClassMapping -> this.innerClasses + this.methods + this.fields
        is MethodMapping -> this.parameters
        is FieldMapping -> listOf()
        is ParameterMapping -> listOf()
    }

fun Mapping.visit(visitor: (Mapping) -> Unit) {
    visitor(this)
    children.forEach { it.visit(visitor) }
}

@Suppress("IfThenToElvis")
val ClassMapping.packageName: String
    get() = if (parent != null) parent.packageName
    else displayedName.split("/").let { it.subList(0, it.size - 1) }.joinToString("/")

