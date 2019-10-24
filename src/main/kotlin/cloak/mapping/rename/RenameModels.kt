package cloak.mapping.rename

import cloak.mapping.Errorable
import cloak.mapping.descriptor.ParameterDescriptor
import cloak.mapping.mappings.*
import cloak.mapping.success
import java.io.File
import java.nio.file.Paths

//TODO: store mappings in memory


data class Rename<M : Mapping>(
    val originalName: Name<M>,
    private val newName: String,
    private val newPackageName: String?,
    private val explanation: String?
) {
    fun findRenameTarget(file: File): M? {
        if (file.isDirectory) return null
        val topLevelClass = originalName.topLevelClass
        if (topLevelClass.className != file.nameWithoutExtension) return null
        if (!Paths.get(file.parent).endsWith(Paths.get(topLevelClass.packageName))) return null

        return originalName.findRenameTarget(MappingsFile.read(file))
    }

    fun rename(mappings: M): Errorable<Unit> {
        return if (newPackageName != null) {
            // Changing the package can only be done on top-level classes
            val renamer = originalName as? ClassName
                ?: error("It should be verified that package rename can only be done on classes")

            renamer.renameAndChangePackage(mappings as ClassMapping, newPackageName, newName)
            success()
        } else rename(mappings, newName)
    }

}

sealed class Name<M : Mapping> {
    abstract val topLevelClass: ClassName

    abstract fun findRenameTarget(mappings: MappingsFile): M?
}


data class ClassName(val className: String, val packageName: String, val innerClass: ClassName?) :
    Name<ClassMapping>() {
    override val topLevelClass = this

    override fun findRenameTarget(mappings: ClassMapping): ClassMapping? =
        findRenameTarget(mappings, isTopLevelClass = true)

    private fun findRenameTarget(mappings: ClassMapping, isTopLevelClass: Boolean): ClassMapping? {
        val currentMappingName = mappings.nonNullName
        // Only top level classes have the package prefixed
        val expectedName = if (isTopLevelClass) "$packageName/$className" else className
        if (currentMappingName != expectedName) return null
        if (innerClass != null) {
            for (innerClassMapping in mappings.innerClasses) {
                val found = innerClass.findRenameTarget(innerClassMapping, isTopLevelClass = false)
                if (found != null) return found
            }
            return null
        } else {
            return mappings
        }
    }

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


data class FieldName(val fieldName: String, val classIn: ClassName) : Name<FieldMapping>() {
    override val topLevelClass = classIn
    override fun findRenameTarget(mappings: MappingsFile): FieldMapping? {
        return classIn.findRenameTarget(mappings)?.fields
            ?.find { it.nonNullName == fieldName }
    }

    override fun toString() = "$classIn${Joiner.Field}$fieldName"

}


data class MethodName(val methodName: String, val classIn: ClassName, val parameterTypes: List<ParameterDescriptor>) :
    Name<MethodMapping>() {
    override val topLevelClass = classIn
    override fun findRenameTarget(mappings: MappingsFile): MethodMapping? {
        val targetClass = classIn.findRenameTarget(mappings) ?: return null

        return targetClass.methods
            .find {

                it.nonNullName == methodName &&
                        it.descriptor.parameterDescriptors == parameterTypes
            }
    }

    override fun toString() = "$classIn${Joiner.Method}$methodName(${parameterTypes.joinToString(", ")})"


}

class ParameterName(val index: Int, private val methodIn: MethodName) : Name<ParameterMapping>() {
    override val topLevelClass = methodIn.classIn

    override fun findRenameTarget(mappings: MappingsFile): ParameterMapping? {
        val targetMethod = methodIn.findRenameTarget(mappings) ?: return null
        targetMethod.parameters.find { it.index == index }?.let { return it }
        return null
    }

    override fun toString() = "$methodIn[$index]"
}
