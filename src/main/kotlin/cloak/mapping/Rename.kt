package cloak.mapping

import cloak.mapping.mappings.*
import java.io.File


data class Rename<M : Mapping>(
    val originalName: OriginalName<M>,
    private val newName: String,
    private val newPackageName: String?,
    private val explanation: String?,
    val byObfuscated: Boolean
) {
    fun findRenameTarget(file: File): M? {
        if (!originalName.matchesFileName(file) || file.isDirectory) return null
        return originalName.findRenameTarget(MappingsFile.read(file), byObfuscated)
    }

    fun rename(mappings: M): Errorable<Unit> {
        return if (newPackageName != null) {
            // Changing the package can only be done on top-level classes
            val renamer = originalName.name as? ClassName
                ?: error("It should be verified that package rename can only be done on classes")

            renamer.renameAndChangePackage(mappings as ClassMapping, newPackageName, newName)
            Unit.success
        } else originalName.rename(mappings, newName)
    }

}


/**
 *  In the original name you can qualify to resolve ambiguities,
 *  and in the new name you can qualify to change the package.
 */
data class OriginalName<M : Mapping>(val name: Name<M>, val packageDisambiguator: String?) {
    fun matchesFileName(file: File): Boolean {
        // Subtle difference between 'endsWith(partialPackage)' and  == 'file.nameWithoutExtension',
        // the former will allow not matching entirely (only the end),
        // and the latter will force the class name to completely match.

        if (name.topLevelClassName != file.nameWithoutExtension) return false
        return packageDisambiguator == null || file.parent.endsWith(packageDisambiguator)

    }

    fun findRenameTarget(mappings: MappingsFile, byObfuscated: Boolean): M? {
        return name.findRenameTarget(mappings, byObfuscated)
    }

    fun rename(mappings: M, newName: String) = name.rename(mappings, newName)
}


sealed class Name<M : Mapping> {
    abstract val topLevelClassName: String
    abstract fun findRenameTarget(mappings: MappingsFile, byObfuscated: Boolean): M?

    // There is no default implementation because that would require Mapping.deobfuscatedName to be a var
    // which makes it so we can't make some implementation have a not nullable deobfuscatedName
    abstract fun rename(mapping: M, newName: String): Errorable<Unit>
}


data class ClassName(val topLevelName: String, val innerClass: ClassName?) : Name<ClassMapping>() {
    override val topLevelClassName = topLevelName
//    override fun findRenameTarget(mappings: MappingsFile, byObfuscated: Boolean): ClassMapping? {
//        return findRenameTarget(mappings, byObfuscated)
//    }

    override fun findRenameTarget(mappings: ClassMapping, byObfuscated: Boolean): ClassMapping? {
        val targetName = mappings.nameOrObfuscated(byObfuscated)
        if (targetName.split("/").last() != topLevelName) return null
        if (innerClass != null) {
            for (innerClassMapping in mappings.innerClasses) {
                val found = innerClass.findRenameTarget(innerClassMapping, false)
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


    //TODO: test renaming when already exists

    override fun rename(mapping: ClassMapping, newName: String): Errorable<Unit> {
        if (mapping.parent != null && mapping.parent.innerClasses.any { it.clashesWith(newName) }) {
            return fail("There already a class named $newName inside ${mapping.parent.nonNullName()}")
        }
        val packageName = (mapping.deobfuscatedName ?: mapping.obfuscatedName).split("/")
            .let { it.subList(0, it.size - 1).joinToString("/") }

        mapping.deobfuscatedName = "${if (packageName.isNotEmpty()) "$packageName/" else ""}$newName"

        return Unit.success
    }
}

private fun Mapping.clashesWith(name: String) = nonNullName() == name


data class FieldName(val fieldName: String, val classIn: ClassName) : Name<FieldMapping>() {
    override val topLevelClassName = classIn.topLevelClassName
    override fun findRenameTarget(mappings: MappingsFile, byObfuscated: Boolean): FieldMapping? {
        return classIn.findRenameTarget(mappings, false)?.fields
            ?.find { it.nameOrObfuscated(byObfuscated) == fieldName }
    }

    override fun rename(mapping: FieldMapping, newName: String): Errorable<Unit> {
        if (mapping.parent.methods.any { it.deobfuscatedName == newName }) {
            return fail("There's already a field named $newName inside ${mapping.parent.nonNullName()}")
        }
        mapping.deobfuscatedName = newName
        return Unit.success
    }


}

data class MethodName(val methodName: String, val classIn: ClassName, val parameterTypes: List<String>?) :
    Name<MethodMapping>() {
    override val topLevelClassName = classIn.topLevelClassName
    override fun findRenameTarget(mappings: MappingsFile, byObfuscated: Boolean): MethodMapping? {
        val targetClass = classIn.findRenameTarget(mappings, false) ?: return null

        // User can disambiguate with descriptor (user inputs something human readable, what is stored is not readable)
        if (parameterTypes != null) {
            TODO("Implement descriptor parser")
        }
        return targetClass.methods.find { it.nameOrObfuscated(byObfuscated) == methodName }
    }

    override fun rename(mapping: MethodMapping, newName: String): Errorable<Unit> {
        if (mapping.parent.methods.any { it.deobfuscatedName == newName && it.descriptor == mapping.descriptor }) {
            return fail("There's already a method named $newName(TODO PARSE DESCRIPTOR) inside ${mapping.parent.nonNullName()}")
        }
        mapping.deobfuscatedName = newName
        return Unit.success
    }


}

sealed class ParameterName(methodIn: MethodName) : Name<ParameterMapping>() {
    override val topLevelClassName = methodIn.topLevelClassName

    override fun rename(mapping: ParameterMapping, newName: String): Errorable<Unit> {
        if (mapping.parent.parameters.any { it.deobfuscatedName == newName }) {
            return fail("There's already a parameter named $newName inside ${mapping.parent.nonNullName()}")
        }
        mapping.deobfuscatedName = newName
        return Unit.success
    }

    data class ByIndex(val index: Int, val method: MethodName) : ParameterName(method) {
        override fun findRenameTarget(mappings: MappingsFile, byObfuscated: Boolean): ParameterMapping? {
            val targetMethod = method.findRenameTarget(mappings, false) ?: return null
            targetMethod.parameters.find { it.index == index }?.let { return it }

            //TODO: uncomment once we have descriptor parser
            /*val parameterAmount =2 *//*Find method amount from descriptor *//*
            return if(parameterAmount >= index){
                // Since the enigma format emits unnamed parameters, we will add the unnamed parameter ourselves
                // so we can rename it in the rename method
                val fabricatedParameter = ParameterMapping(index, deobfuscatedName = "", parent = targetMethod)
                targetMethod.parameters.add(fabricatedParameter)
                fabricatedParameter
            }else null*/

            return null
        }


    }

    data class ByName(val name: String, val method: MethodName) : ParameterName(method) {
        override fun findRenameTarget(mappings: MappingsFile, byObfuscated: Boolean): ParameterMapping? {
            return method.findRenameTarget(mappings, false)
                ?.parameters?.find { it.nameOrObfuscated(byObfuscated) == name }
        }

    }
}
