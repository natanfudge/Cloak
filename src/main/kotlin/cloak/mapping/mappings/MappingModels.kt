package cloak.mapping.mappings

import cloak.mapping.Joiner


typealias MappingsFile = ClassMapping
typealias MappingsFileCompanion = ClassMapping.Companion

data class ClassMapping(
    override var obfuscatedName: String,
    override var deobfuscatedName: String?,
    val methods: MutableList<MethodMapping>,
    val fields: MutableList<FieldMapping>,
    val innerClasses: MutableList<ClassMapping>,
    override val parent: ClassMapping?
) : Mapping() {
    // To be able to extend
    companion object;

    override fun humanReadableName(obfuscated: Boolean): String {
        val humanReadableName = nameOrObfuscated(obfuscated)
        return if (parent == null) humanReadableName else parent.humanReadableName(obfuscated) +
                Joiner.InnerClass + humanReadableName
    }

    override fun toString() = humanReadableName(false)
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

//TODO: this needs to also include a parsed version of the descriptor, like Class#method(int,bool,MyClass)
data class MethodMapping(
    override var obfuscatedName: String,
    override var deobfuscatedName: String?,
    override var descriptor: String,
    var parameters: MutableList<ParameterMapping>,
    override val parent: ClassMapping
) : Mapping(), Descriptored {
    override fun humanReadableName(obfuscated: Boolean) = parent.humanReadableName(obfuscated) +
            Joiner.Method + nameOrObfuscated(obfuscated)

    override fun toString() = humanReadableName(false)
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
    override var obfuscatedName: String, override var deobfuscatedName: String,
    override var descriptor: String, override val parent: ClassMapping
) : Mapping(), Descriptored {
    override fun humanReadableName(obfuscated: Boolean) = parent.humanReadableName(obfuscated) +
            Joiner.Field + nameOrObfuscated(obfuscated)

    override fun toString() = humanReadableName(false)
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
    var index: Int, override var deobfuscatedName: String,
    override val parent: MethodMapping
) : Mapping() {
    override val obfuscatedName = ""
    override fun humanReadableName(obfuscated: Boolean) = parent.humanReadableName(obfuscated) +
            "[param $index = ${nameOrObfuscated(obfuscated)}]"

    override fun toString() = humanReadableName(false)
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

    abstract fun humanReadableName(obfuscated: Boolean): String
    abstract val root: ClassMapping

    fun nonNullName() = deobfuscatedName ?: obfuscatedName
    fun nameOrObfuscated(obfuscated: Boolean) = if (obfuscated) obfuscatedName else nonNullName()
    fun name(obfuscated: Boolean) = if (obfuscated) obfuscatedName else deobfuscatedName

//    fun namesEqual(other : Mapping) = obfuscatedName == other.obfuscatedName && other.deobfuscatedName == deobfuscatedName
}


interface Descriptored {
    val descriptor: String
}