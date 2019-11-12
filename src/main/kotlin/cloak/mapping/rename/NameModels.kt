package cloak.mapping.rename

import cloak.mapping.Errorable
import cloak.mapping.descriptor.ParameterDescriptor
import cloak.mapping.descriptor.ParameterDescriptorSerializer
import cloak.mapping.mappings.*
import cloak.mapping.success
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Paths

//TODO: handle renaming top-level classes without a previous mapping.
data class Rename(
    val originalName: Name,
    private val newName: String,
    private val newPackageName: String?,
    private val explanation: String?
) {

    fun rename(mappings: Mapping): Errorable<Unit> {
        return if (newPackageName != null) {
            // Changing the package can only be done on top-level classes
            assert(originalName is ClassName){"It should be verified that package rename can only be done on classes"}
            mappings as ClassMapping

            mappings.deobfuscatedName = "$newPackageName/$newName"
            success()
        } else rename(mappings, newName)
    }

}

sealed class Name{
    abstract val topLevelClass: ClassName

//    abstract fun findRenameTarget(mappings: MappingsFile): Mapping?
}

@Serializable
data class ClassName(val className: String, val packageName: String, val innerClass: ClassName?) :
    Name() {
    override val topLevelClass get() = this

//    override fun findRenameTarget(mappings: ClassMapping): ClassMapping? =
//        findRenameTarget(mappings, isTopLevelClass = true)
//
//    private fun findRenameTarget(mappings: ClassMapping, isTopLevelClass: Boolean): ClassMapping? {
//        val currentMappingName = mappings.nonNullName
//        // Only top level classes have the package prefixed
//        val expectedName = if (isTopLevelClass) "$packageName/$className" else className
//        if (currentMappingName != expectedName) return null
//        if (innerClass != null) {
//            for (innerClassMapping in mappings.innerClasses) {
//                val found = innerClass.findRenameTarget(innerClassMapping, isTopLevelClass = false)
//                if (found != null) return found
//            }
//            return null
//        } else {
//            return mappings
//        }
//    }

    fun renameAndChangePackage(mappings: ClassMapping, newPackageName: String, newName: String) {
        mappings.deobfuscatedName = "$newPackageName/$newName"
    }

    override fun toString(): String = toString(isInnerClass = false)

    private fun toString(isInnerClass: Boolean): String {
        val packageName = if (isInnerClass) "" else "$packageName/"
        val innerClassStr = if (innerClass != null) Joiner.InnerClass + innerClass.toString(isInnerClass = true) else ""
        return "$packageName$className$innerClassStr"
    }

}

@Serializable
data class FieldName(val fieldName: String, val classIn: ClassName) : Name() {
    override val topLevelClass = classIn
//    override fun findRenameTarget(mappings: MappingsFile): FieldMapping? {
//        return classIn.findRenameTarget(mappings)?.fields
//            ?.find { it.nonNullName == fieldName }
//    }

    override fun toString() = "$classIn${Joiner.Field}$fieldName"

}

@Serializable
data class MethodName(
    val methodName: String, val classIn: ClassName,
     val parameterTypes: List<@Serializable(with = ParameterDescriptorSerializer::class) ParameterDescriptor>
) : Name() {


    override val topLevelClass = classIn
//    override fun findRenameTarget(mappings: MappingsFile): MethodMapping? {
//        val targetClass = classIn.findRenameTarget(mappings) ?: return null
//
//        return targetClass.methods
//            .find {
//
//                it.nonNullName == methodName &&
//                        it.descriptor.parameterDescriptors == parameterTypes
//            }
//    }

    override fun toString() = "$classIn${Joiner.Method}$methodName(${parameterTypes.joinToString(", ")})"


}

@Serializable
data class ParamName(val index: Int,  val methodIn: MethodName) : Name() {
    override val topLevelClass = methodIn.classIn

//    override fun findRenameTarget(mappings: MappingsFile): ParameterMapping? {
//        val targetMethod = methodIn.findRenameTarget(mappings) ?: return null
//        targetMethod.parameters.find { it.index == index }?.let { return it }
//        return null
//    }

    override fun toString() = "$methodIn[$index]"
}
