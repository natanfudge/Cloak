package cloak.format.mappings

import cloak.format.descriptor.MethodDescriptor
import java.util.*


object Joiner {
    const val Method = "#"
    const val Field = "%"
    const val InnerClass = "$"
    const val Parameter = "["
    val All = listOf(
        Method,
        Field,
        InnerClass,
        Parameter
    )
}

private fun Mapping.hash(vararg values: Any?) =
    Objects.hash(arrayOf(obfuscatedName, deobfuscatedName, comment, *values))


private fun Mapping.eq(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Mapping
    if (obfuscatedName != other.obfuscatedName) return false
    if (deobfuscatedName != other.deobfuscatedName) return false
    return true
}

sealed class Mapping {
    abstract val obfuscatedName: String
    abstract val deobfuscatedName: String?
    abstract var comment: MutableList<String>
    abstract val parent: Mapping?

    abstract val root: ClassMapping
}


typealias MappingsFile = ClassMapping

fun MappingsFile.visitClasses(visitor: (ClassMapping) -> Unit) {
    visitor(this)
    for (innerClass in innerClasses) innerClass.visitClasses(visitor)
}


data class ClassMapping(
    override val obfuscatedName: String,
    override var deobfuscatedName: String?,
    val methods: MutableList<MethodMapping> = mutableListOf(),
    val fields: MutableList<FieldMapping> = mutableListOf(),
    val innerClasses: MutableList<ClassMapping> = mutableListOf(),
    override val parent: ClassMapping?, override var comment: MutableList<String> = mutableListOf()
) : Mapping() {
    // To be able to extend
    companion object;

    override fun toString(): String {
        return if (parent == null) displayedName else "$parent${Joiner.InnerClass}$displayedName"
    }

    override fun equals(other: Any?): Boolean {
        if (eq(other)) return true

        other as ClassMapping

        if (methods != other.methods) return false
        if (fields != other.fields) return false
        if (innerClasses != other.innerClasses) return false

        return true
    }

    override fun hashCode(): Int = hash(methods, fields, innerClasses)

    override val root: ClassMapping = parent?.root ?: this
}

data class MethodMapping(
    override val obfuscatedName: String,
    override var deobfuscatedName: String?,
    val descriptor: MethodDescriptor,
    val parameters: MutableList<ParameterMapping> = mutableListOf(),
    override val parent: ClassMapping, override var comment: MutableList<String> = mutableListOf()
) : Mapping() {
    override fun toString() =
        "$parent${Joiner.Method}$displayedName(${descriptor.parameterDescriptors.joinToString(" ,")})" +
                ": ${descriptor.returnDescriptor}"

    override fun equals(other: Any?): Boolean {
        if (eq(other)) return true

        other as MethodMapping

        if (descriptor != other.descriptor) return false
        if (parameters != other.parameters) return false

        return true
    }

    override fun hashCode(): Int  = hash(descriptor,parameters)

    override val root = parent.root


}

data class FieldMapping(
    override val obfuscatedName: String, override var deobfuscatedName: String?,
    val descriptor: String, override val parent: ClassMapping, override var comment: MutableList<String> = mutableListOf()
) : Mapping() {

    override fun toString() = "$parent${Joiner.Field}$displayedName"
    override fun equals(other: Any?): Boolean {
        if (eq(other)) return true

        other as FieldMapping

        if (descriptor != other.descriptor) return false

        return true
    }

    override fun hashCode(): Int = hash(descriptor)

    override val root = parent.root


}

data class ParameterMapping(
    val index: Int, override var deobfuscatedName: String?,
    override val parent: MethodMapping, override var comment: MutableList<String> = mutableListOf()
) : Mapping() {
    override val obfuscatedName = ""

    override fun toString() = "$parent[param $index = $displayedName]"
    override fun equals(other: Any?): Boolean {
        if (eq(other)) return true

        other as ParameterMapping

        if (index != other.index) return false

        return true
    }

    override fun hashCode(): Int = hash(index)

    override val root = parent.root
}


