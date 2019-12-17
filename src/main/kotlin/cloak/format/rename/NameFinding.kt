package cloak.format.rename

import cloak.actions.getClassIntermediaryName
import cloak.format.descriptor.MethodDescriptor
import cloak.format.descriptor.read
import cloak.format.mappings.*
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.saved.ExplainedResult
import cloak.platform.saved.LatestIntermediaryNames
import cloak.platform.saved.getIntermediaryNamesOfVersion
import cloak.util.doesNotExist
import cloak.util.success
import com.github.michaelbull.result.Err
import kotlin.test.assertNotNull

fun Name.getMatchingMappingIn(platform: ExtendedPlatform): ExplainedResult<Mapping> {
    val path = topLevelClass.let { "${it.packageName}/${it.className}$MappingsExtension" }
    val mappingsFilePath = platform.yarnRepo.getMappingsFile(path)

    // Add a new top level class in case this one doesn't exist
    if (mappingsFilePath.doesNotExist) {
        return if (this is ClassName && this.isTopLevelClass) {
            createDummyTopLevelClass(platform.getIntermediaryNamesOfVersion(platform.branch.minecraftVersion))
        } else Err("Packages can only be changed by renaming the top level class")
    }

    val mappingsFile = MappingsFile.read(mappingsFilePath)
    val matchingExistingMapping = getMatchingMappingIn(mappingsFile)
    // This is the result most of the time.
    // We do the expensive fetching/caching of intermediaries only after we do this check.
    if (matchingExistingMapping != null) return matchingExistingMapping.success
    return addDummyMappingTo(
        platform,
        mappingsFile,
        platform.getIntermediaryNamesOfVersion(platform.branch.minecraftVersion)
    )

}

private fun ClassName.createDummyTopLevelClass(
    intermediaries: LatestIntermediaryNames
): ExplainedResult<Mapping> {
    val name = this.fullyQualifiedName()
    // Make sure we don't add stuff that doesn't exist
    if (name !in intermediaries.classNames) return Err("'$name' does not exist in newer versions and thus cannot be renamed")
    return ClassMapping(
        // These will be intermediary names
        name,
        name,
        parent = null
    ).success
}

// This is needed for inner classes because their parent class will incorrectly have a named name.
private fun ClassName.getIntermediaryName(platform: ExtendedPlatform): String {
    val parentsPath = "$packageName/" + this.getParents().joinToString("$") { it.className }
    val parentIntPath = platform.getClassIntermediaryName(parentsPath) ?: parentsPath
    return "${parentIntPath}$$className"
}


/**
 * In cases where a method/field/inner class does exist, but there is no mappings line for it yet, we need to add it ourselves.
 * This is only a "dummy" mapping because it's gonna get immediately renamed afterwards.
 * Will return null if it can't find a parent to attach to.
 */

private fun Name.addDummyMappingTo(
    platform: ExtendedPlatform,
    mappingsFile: MappingsFile,
    intermediaries: LatestIntermediaryNames
): ExplainedResult<Mapping> {
    val parent = this.parent
    assertNotNull(parent) { "Top level class are handled in createDummyTopLevelClass, so the parent should not be null" }
    val parentMapping = parent.getMatchingMappingIn(mappingsFile) ?: return Err("$this could not be found in ${mappingsFile.displayedName}")
    return when (this) {

        // If the class/field/method/parameter name is in intermediary, then the checks against `intermediaries` will check if they exist.
        // If they are in named, then they already belong to some entry, and cannot create a new entry by themselves.

        is ClassName -> {
            // Make sure we don't add stuff that doesn't exist
            if (this.getIntermediaryName(platform) !in intermediaries.classNames) return Err("$className does not exist in the newest version, and thus cannot be renamed")
            val classParent = parentMapping.cast<ClassMapping>()
            ClassMapping(
                // These will be intermediary names
                className,
                className,
                parent = classParent
            ).also { classParent.innerClasses.add(it) }.success
        }
        is FieldName -> {
            val descriptor = intermediaries.fieldNames[this.fieldName] ?: return Err("$fieldName does not exist in the newest version, and thus cannot be renamed")
            val classParent = parentMapping.cast<ClassMapping>()
            FieldMapping(fieldName, fieldName, descriptor, classParent).also { classParent.fields.add(it) }.success
        }
        is MethodName -> {
            val descriptor = intermediaries.methodNames[this.methodName] ?: return Err("$methodName does not exist in the newest version, and thus cannot be renamed")
            val classParent = parentMapping.cast<ClassMapping>()
            MethodMapping(methodName, methodName, MethodDescriptor.read(descriptor), mutableListOf(), classParent)
                .also { classParent.methods.add(it) }.success
        }
        is ParamName -> {
            val methodParent = parentMapping.cast<MethodMapping>()
            // <unnamed> will be replaced
            ParameterMapping(index, "<unnamed>", methodParent).also { methodParent.parameters.add(it) }.success
        }
    }
}

private fun Name.getMatchingMappingIn(mappingsFile: MappingsFile): Mapping? = when (this) {
    is ClassName -> getMatchingMappingIn(mappingsFile)
    is FieldName -> getMatchingMappingIn(mappingsFile)
    is MethodName -> getMatchingMappingIn(mappingsFile)
    is ParamName -> getMatchingMappingIn(mappingsFile)
}

private fun ClassName.getMatchingMappingIn(mapping: ClassMapping): Mapping? {
    val path = getParentsAndSelf()
    val topLevelClass = path.first()
    if ("${topLevelClass.packageName}/${topLevelClass.className}" != mapping.displayedName) return null
    // If it's just a top level class then we found it
    if (path.size == 1) return mapping

    val nameInnerClassIter = path.iterator()
    nameInnerClassIter.next() // skip the first one
    var nextOfMapping: ClassMapping? = mapping
    do {
        val searchedName = nameInnerClassIter.next()
        nextOfMapping = nextOfMapping!!.innerClasses.find { it.displayedName == searchedName.className }
        if (nextOfMapping == null) return null
    } while (nameInnerClassIter.hasNext())

    return nextOfMapping

}

private fun FieldName.getMatchingMappingIn(mappings: MappingsFile) =
    classIn.getMatchingMappingIn(mappings)?.cast<ClassMapping>()?.fields?.find { it.displayedName == fieldName }

private fun MethodName.getMatchingMappingIn(mappings: MappingsFile): Mapping? {
    val targetClass = classIn.getMatchingMappingIn(mappings) as? ClassMapping ?: return null

    return targetClass.methods.find { it.displayedName == methodName && it.descriptor.parameterDescriptors == parameterTypes }
}

private fun ParamName.getMatchingMappingIn(mappings: MappingsFile): Mapping? {
    val targetMethod = methodIn.getMatchingMappingIn(mappings) as? MethodMapping ?: return null
    return targetMethod.parameters.find {
        // Parameters in constructors begin from 1 in enigma format
        val paramIndex = it.index
        paramIndex == index
    }
}

private inline fun <reified T> Any.cast() = this as T