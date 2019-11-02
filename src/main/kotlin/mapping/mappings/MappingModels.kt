package cloak.mapping.mappings

import cloak.mapping.descriptor.MethodDescriptor


object Joiner {
    const val Method = "#"
    const val Field = "%"
    const val InnerClass = "$"
    const val Parameter = "["
    val All = listOf(Method, Field, InnerClass, Parameter)
}



typealias MappingsFile = ClassMapping

fun MappingsFile.visitClasses(visitor: (ClassMapping) -> Unit) {
    visitor(this)
    for (innerClass in innerClasses) innerClass.visitClasses(visitor)
}

typealias MappingsFileCompanion = ClassMapping.Companion

data class ClassMapping(
    override val obfuscatedName: String,
    override var deobfuscatedName: String?,
    val methods: MutableList<MethodMapping>,
    val fields: MutableList<FieldMapping>,
    val innerClasses: MutableList<ClassMapping>,
    override val parent: ClassMapping?
) : Mapping() {
    // To be able to extend
    companion object;

    override fun toString(): String {
        return if (parent == null) nonNullName else "$parent${Joiner.InnerClass}$nonNullName"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassMapping

        if (obfuscatedName != other.obfuscatedName) return false
        if (deobfuscatedName != other.deobfuscatedName) return false
        if (methods != other.methods) return false
        if (fields != other.fields) return false
        if (innerClasses != other.innerClasses) return false

        return true
    }

    override fun hashCode(): Int {
        var result = obfuscatedName.hashCode()
        result = 31 * result + (deobfuscatedName?.hashCode() ?: 0)
        result = 31 * result + methods.hashCode()
        result = 31 * result + fields.hashCode()
        result = 31 * result + innerClasses.hashCode()
        return result
    }

    override val root: ClassMapping = parent?.root ?: this
}

data class MethodMapping(
    override val obfuscatedName: String,
    override var deobfuscatedName: String?,
    val descriptor: MethodDescriptor,
    val parameters: MutableList<ParameterMapping>,
    override val parent: ClassMapping
) : Mapping() {
    override fun toString() =
        "$parent${Joiner.Method}$nonNullName(${descriptor.parameterDescriptors.joinToString(" ,")})" +
                ": ${descriptor.returnDescriptor}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodMapping

        if (obfuscatedName != other.obfuscatedName) return false
        if (deobfuscatedName != other.deobfuscatedName) return false
        if (descriptor != other.descriptor) return false
        if (parameters != other.parameters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = obfuscatedName.hashCode()
        result = 31 * result + (deobfuscatedName?.hashCode() ?: 0)
        result = 31 * result + descriptor.hashCode()
        result = 31 * result + parameters.hashCode()
        return result
    }

    override val root = parent.root


}

data class FieldMapping(
    override val obfuscatedName: String, override var deobfuscatedName: String,
    val descriptor: String, override val parent: ClassMapping
) : Mapping() {

    override fun toString() = "$parent${Joiner.Field}$nonNullName"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FieldMapping

        if (obfuscatedName != other.obfuscatedName) return false
        if (deobfuscatedName != other.deobfuscatedName) return false
        if (descriptor != other.descriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = obfuscatedName.hashCode()
        result = 31 * result + deobfuscatedName.hashCode()
        result = 31 * result + descriptor.hashCode()
        return result
    }

    override val root = parent.root


}

data class ParameterMapping(
    val index: Int, override var deobfuscatedName: String,
    override val parent: MethodMapping
) : Mapping() {
    override val obfuscatedName = ""

    override fun toString() = "$parent[param $index = $nonNullName]"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParameterMapping

        if (index != other.index) return false
        if (deobfuscatedName != other.deobfuscatedName) return false
        if (obfuscatedName != other.obfuscatedName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + deobfuscatedName.hashCode()
        result = 31 * result + obfuscatedName.hashCode()
        return result
    }

    override val root = parent.root


}

sealed class Mapping {
    abstract val obfuscatedName: String
    abstract val deobfuscatedName: String?
    abstract val parent: Mapping?

    abstract val root: ClassMapping

    val nonNullName get() = deobfuscatedName ?: obfuscatedName
    fun name(obfuscated: Boolean) = if (obfuscated) obfuscatedName else deobfuscatedName

    fun List<Mapping>.anythingElseHasTheSameObfName() = any {
        it !== this@Mapping && it.deobfuscatedName == deobfuscatedName
    }
}


