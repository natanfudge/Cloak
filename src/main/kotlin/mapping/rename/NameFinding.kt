package cloak.mapping.rename

import cloak.git.YarnRepo
import cloak.mapping.mappings.*

fun Name.getMatchingMappingIn(yarnRepo: YarnRepo): Mapping? {
    val path = topLevelClass.let { "${it.packageName}/${it.className}$MappingsExtension" }
    return getMatchingMappingIn(MappingsFile.read(yarnRepo.getMappingsFile(path)))
}

private fun Name.getMatchingMappingIn(mappingsFile: MappingsFile): Mapping? = when (this) {
    is ClassName -> getMatchingMappingIn(mappingsFile, isTopLevelClass = true)
    is FieldName -> getMatchingMappingIn(mappingsFile)
    is MethodName -> getMatchingMappingIn(mappingsFile)
    is ParamName -> getMatchingMappingIn(mappingsFile)
}

private fun ClassName.getMatchingMappingIn(mappings: ClassMapping, isTopLevelClass: Boolean): ClassMapping? {
    val currentMappingName = mappings.nonNullName
    // Only top level classes have the package prefixed
    val expectedName = if (isTopLevelClass) "$packageName/$className" else className
    if (currentMappingName != expectedName) return null
    if (innerClass != null) {
        for (innerClassMapping in mappings.innerClasses) {
            val found = innerClass.getMatchingMappingIn(innerClassMapping, isTopLevelClass = false)
            if (found != null) return found
        }
        return null
    } else {
        return mappings
    }
}

private fun FieldName.getMatchingMappingIn(mappings: MappingsFile) =
    classIn.getMatchingMappingIn(mappings)?.cast<ClassMapping>()?.fields?.find { it.nonNullName == fieldName }

private fun MethodName.getMatchingMappingIn(mappings: MappingsFile): Mapping? {
    val targetClass = classIn.getMatchingMappingIn(mappings) ?: return null

    return targetClass.cast<ClassMapping>().methods
        .find { it.nonNullName == methodName && it.descriptor.parameterDescriptors == parameterTypes }
}

private fun ParamName.getMatchingMappingIn(mappings: MappingsFile): Mapping? {
    val targetMethod = methodIn.getMatchingMappingIn(mappings) ?: return null
    return targetMethod.cast<MethodMapping>().parameters.find { it.index == index }
}

private inline fun <reified T> Any.cast() = this as T