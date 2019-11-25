package cloak.mapping.rename

import cloak.git.YarnRepo
import cloak.idea.LatestIntermediaryNames
import cloak.idea.util.ProjectWrapper
import cloak.mapping.descriptor.MethodDescriptor
import cloak.mapping.descriptor.read
import cloak.mapping.doesNotExist
import cloak.mapping.getOrKey
import cloak.mapping.mappings.*

fun Name.getMatchingMappingIn(yarnRepo: YarnRepo, project: ProjectWrapper, namedToInt: Map<String, String>): Mapping? {
    val path = topLevelClass.let { "${it.packageName}/${it.className}$MappingsExtension" }
    val mappingsFilePath = yarnRepo.getMappingsFile(path)

    // Add a new top level class in case this one doesn't exist
    if (mappingsFilePath.doesNotExist) {
        if (this is ClassName && this.isTopLevelClass) {
            return createDummyTopLevelClass(project.getLatestIntermediaryNames(yarnRepo.getTargetMinecraftVersion()))
        } else error("Could not find mappings file at $mappingsFilePath for name $this")
    }

    val mappingsFile = MappingsFile.read(mappingsFilePath)
    val matchingExistingMapping = getMatchingMappingIn(mappingsFile)
    // This is the result most of the time.
    // We do the expensive fetching/caching of intermediaries only after we do this check.
    if (matchingExistingMapping != null) return matchingExistingMapping
    return addDummyMappingTo(
        mappingsFile,
        project.getLatestIntermediaryNames(yarnRepo.getTargetMinecraftVersion()),
        namedToInt
    )


}

private fun ClassName.createDummyTopLevelClass(
    intermediaries: LatestIntermediaryNames
): ClassMapping? {
    val name = this.fullyQualifiedName()
    // Make sure we don't add stuff that doesn't exist
    if (name !in intermediaries.classNames) return null
    return ClassMapping(
        // These will be intermediary names
        name,
        name,
        parent = null,
        methods = mutableListOf(),
        fields = mutableListOf(),
        innerClasses = mutableListOf()
    )
}


private fun ClassName.getIntermediaryName(intermediaryMappings: Map<String, String>): String {
    val parentsPath = "$packageName/" + this.getParents().joinToString("$") { it.className }
    val parentIntPath = intermediaryMappings.getOrKey(parentsPath)
    return "${parentIntPath}$$className"
}

//TODO: gonna need existingIntermediaryNames to give a descriptor

/**
 * In cases where a method/field/inner class does exist, but there is no mappings line for it yet, we need to add it ourselves.
 * This is only a "dummy" mapping because it's gonna get immediately renamed afterwards.
 * Will return null if it can't find a parent to attach to.
 */

private fun Name.addDummyMappingTo(
    mappingsFile: MappingsFile,
    intermediaries: LatestIntermediaryNames,
    namedToInt: Map<String, String>
): Mapping? {
    //TODO: handle renaming a top-level class
    val parent = this.parent ?: return null
    val parentMapping = parent.getMatchingMappingIn(mappingsFile) ?: return null
    return when (this) {
        is ClassName -> {
            // Make sure we don't add stuff that doesn't exist
            if (this.getIntermediaryName(namedToInt) !in intermediaries.classNames) return null
            val classParent = parentMapping.cast<ClassMapping>()
            ClassMapping(
                // These will be intermediary names
                className,
                className,
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
            // <placeholder> will be replaced
            ParameterMapping(index, "<placeholder>", methodParent).also { methodParent.parameters.add(it) }
        }
    }
}

private fun Name.getMatchingMappingIn(mappingsFile: MappingsFile): Mapping? = when (this) {
    is ClassName -> getMatchingMappingIn(mappingsFile)
    is FieldName -> getMatchingMappingIn(mappingsFile)
    is MethodName -> getMatchingMappingIn(mappingsFile)
    is ParamName -> getMatchingMappingIn(mappingsFile)
}

private fun ClassName.getMatchingMappingIn(mapping: ClassMapping): ClassMapping? {
    val path = getParentsAndSelf()
    val topLevelClass = path.first()
    if ("${topLevelClass.packageName}/${topLevelClass.className}" != mapping.nonNullName) return null
    // If it's just a top level class then we found it
    if (path.size == 1) return mapping

    val nameInnerClassIter = path.iterator()
    nameInnerClassIter.next() // skip the first one
    var nextOfMapping: ClassMapping? = mapping
    do {
        val searchedName = nameInnerClassIter.next()
        nextOfMapping = nextOfMapping!!.innerClasses.find { it.nonNullName == searchedName.className }
        if (nextOfMapping == null) return null
    } while (nameInnerClassIter.hasNext())

    return nextOfMapping

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