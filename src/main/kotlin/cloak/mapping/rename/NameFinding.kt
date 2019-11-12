package cloak.mapping.rename

import cloak.git.YarnRepo
import cloak.idea.LatestIntermediaryNames
import cloak.idea.util.ProjectWrapper
import cloak.mapping.descriptor.MethodDescriptor
import cloak.mapping.descriptor.read
import cloak.mapping.mappings.*
import cloak.mapping.plus

fun Name.getMatchingMappingIn(yarnRepo: YarnRepo, project: ProjectWrapper): Mapping? {
    val path = topLevelClass.let { "${it.packageName}/${it.className}$MappingsExtension" }
    val mappingsFile = MappingsFile.read(yarnRepo.getMappingsFile(path))
    val matchingExistingMapping = getMatchingMappingIn(mappingsFile)
    // This is the result most of the time. We do the expensive fetching/caching of intermediaries only after we do this check.
    if (matchingExistingMapping != null) return matchingExistingMapping
    return addDummyMappingTo(mappingsFile, project.getLatestIntermediaryNames(yarnRepo.getTargetMinecraftVersion()))
}

//private val Name.selfName
//    get() = when (this) {
//        is ClassName -> className
//        is FieldName -> fieldName
//        is MethodName -> methodName
//        is ParamName -> throw UnsupportedOperationException("selfName not applicable for ParamName")
//    }

// This goes from top to bottom
private val ClassName.parents get() = generateSequence(this) { this.innerClass }.toList()
private val ClassName.parentsAndSelf get() = this + parents


private val Name.parent
    get() = when (this) {
        is ClassName -> parents.lastOrNull()
        is FieldName -> classIn
        is MethodName -> classIn
        is ParamName -> methodIn
    }

private val ClassName.fullyQualifiedName
    get() = "$packageName/${parentsAndSelf.joinToString("$") { className }}"

//TODO: gonna need existingIntermediaryNames to give a descriptor

/**
 * In cases where a method/field/inner class does exist, but there is no mappings line for it yet, we need to add it ourselves.
 * This is only a "dummy" mapping because it's gonna get immediately renamed afterwards.
 * Will return null if it can't find a parent to attach to.
 */

private fun Name.addDummyMappingTo(mappingsFile: MappingsFile, intermediaries: LatestIntermediaryNames): Mapping? {
    //TODO: handle renaming a top-level class
    val parent = this.parent ?: return null
    val parentMapping = parent.getMatchingMappingIn(mappingsFile) ?: return null
    return when (this) {
        is ClassName -> {
            // Make sure we don't add stuff that doesn't exist
            if (this.fullyQualifiedName !in intermediaries.classNames) return null
            val classParent = parentMapping.cast<ClassMapping>()
            ClassMapping(
                // These will be intermediary names
                fullyQualifiedName,
                fullyQualifiedName,
                parent = classParent,
                methods = mutableListOf(),
                fields = mutableListOf(),
                innerClasses = mutableListOf()
            ).also { classParent.innerClasses.add(it) }
        }
        is FieldName -> {
            val descriptor = intermediaries.fieldNames[this.fieldName] ?: return null
            val classParent = parentMapping.cast<ClassMapping>()
            FieldMapping(fieldName, fieldName, descriptor, classParent).also { classParent.fields.add(it) }
        }
        is MethodName -> {
            val descriptor = intermediaries.methodNames[this.methodName] ?: return null
            val classParent = parentMapping.cast<ClassMapping>()
            MethodMapping(methodName, methodName, MethodDescriptor.read(descriptor), mutableListOf(), classParent)
                .also { classParent.methods.add(it) }
        }
        is ParamName -> {
            val methodParent = parentMapping.cast<MethodMapping>()
            ParameterMapping(index, "<placeholder>", methodParent).also { methodParent.parameters.add(it) }
        }
    }
}

private fun Name.getMatchingMappingIn(mappingsFile: MappingsFile): Mapping? = when (this) {
    is ClassName -> getMatchingMappingIn(mappingsFile, isTopLevelClass = true)
    is FieldName -> getMatchingMappingIn(mappingsFile)
    is MethodName -> getMatchingMappingIn(mappingsFile)
    is ParamName -> getMatchingMappingIn(mappingsFile)
}

private fun ClassName.getMatchingMappingIn(mapping: ClassMapping, isTopLevelClass: Boolean): ClassMapping? {
    val currentMappingName = mapping.nonNullName
    // Only top level classes have the package prefixed
    val expectedName = if (isTopLevelClass) "$packageName/$className" else className
    if (currentMappingName != expectedName) return null
    if (innerClass != null) {
        for (innerClassMapping in mapping.innerClasses) {
            val found = innerClass.getMatchingMappingIn(innerClassMapping, isTopLevelClass = false)
            if (found != null) return found
        }
        return null
    } else {
        return mapping
    }
}

private fun FieldName.getMatchingMappingIn(mappings: MappingsFile) =
    classIn.getMatchingMappingIn(mappings)?.cast<ClassMapping>()?.fields?.find { it.nonNullName == fieldName }

private fun MethodName.getMatchingMappingIn(mappings: MappingsFile): Mapping? {
    val targetClass = classIn.getMatchingMappingIn(mappings) as? ClassMapping ?: return null

    return targetClass.methods.find { it.nonNullName == methodName && it.descriptor.parameterDescriptors == parameterTypes }
}

private fun ParamName.getMatchingMappingIn(mappings: MappingsFile): Mapping? {
    val targetMethod = methodIn.getMatchingMappingIn(mappings) as? MethodMapping ?: return null
    return targetMethod.parameters.find {
        // Parameters in constructors begin from 1 in enigma format
        val paramIndex = if (targetMethod.isConstructor) it.index - 1 else it.index
        paramIndex == index
    }
}

private inline fun <reified T> Any.cast() = this as T