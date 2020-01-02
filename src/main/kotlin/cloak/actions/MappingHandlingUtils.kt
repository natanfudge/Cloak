package cloak.actions


import cloak.format.descriptor.remap
import cloak.format.mappings.*
import cloak.format.rename.*
import cloak.git.yarnRepo
import cloak.platform.ActiveMappings
import cloak.platform.ExtendedPlatform
import cloak.platform.saved.ExplainedResult
import cloak.platform.saved.showedNoteAboutLicense
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.system.measureTimeMillis

suspend fun ExtendedPlatform.warmupAsync(): Deferred<Unit> = coroutineScope {
    getAuthenticatedUser()

    val promise = async {
        yarnRepo.warmup()
    }
    if (!showedNoteAboutLicense) {
        showMessageDialog(
            message = """Yarn mappings are licensed under a permissive license and should stay so.
| If you know the name of something from another, less permissive mappings
| set (such as MCP(Forge) or Mojang Proguard output), DO NOT use that name.""".trimMargin(),
            title = "Warning"
        )
        showedNoteAboutLicense = true
    }

    promise
}

/**
 * After the player renames something, the yarn repository has different information than what is in the editor.
 * This updates the information in the editor to be up to date to the repo.
 * */
fun Name.updateAccordingToRenames(platform: ExtendedPlatform): Name {
    val branch = platform.branch
    fun ClassName.updateAccordingToRenames(): ClassName =
        branch.getRenamedTo(this)?.let {
            var newName = copy(className = it.name)
            if (it.packageName != null) newName = newName.copy(packageName = it.packageName)
            newName
        } ?: this

    fun FieldName.updateAccordingToRenames(): FieldName {
        val newClassName = classIn.updateAccordingToRenames()
        return branch.getRenamedTo(this)?.let { copy(fieldName = it.name, classIn = newClassName) }
            ?: copy(classIn = newClassName)
    }

    fun MethodName.updateAccordingToRenames(): MethodName {
        val newClassName = classIn.updateAccordingToRenames()
        return branch.getRenamedTo(this)?.let { copy(methodName = it.name, classIn = newClassName) }
            ?: copy(classIn = newClassName)
    }

    fun ParamName.updateAccordingToRenames(): ParamName {
        val newMethodName = methodIn.updateAccordingToRenames()
        return branch.getRenamedTo(this)?.let {
            copy(paramName = it.name, methodIn = newMethodName)
        } ?: copy(methodIn = newMethodName)
    }

    return when (this) {
        is ClassName -> updateAccordingToRenames()
        is FieldName -> updateAccordingToRenames()
        is MethodName -> updateAccordingToRenames()
        is ParamName -> updateAccordingToRenames()
    }
}

suspend fun ExtendedPlatform.findMatchingMapping(name: Name): ExplainedResult<Mapping> {

    val repoPromise = warmupAsync()

    return asyncWithText("Preparing rename...") {
        repoPromise.await()

        if (!ActiveMappings.areActive()) ActiveMappings.refresh(this@findMatchingMapping)

        val oldName = name.remapParameterDescriptorsToInt(this)

        oldName.getMatchingMappingIn(platform = this)

    }

}


fun ExtendedPlatform.getClassIntermediaryName(namedName: String): String? {
    val parts = namedName.split("$")
    var index = 0
    val file = yarnRepo.getMappingsFile(parts[0] + MappingsExtension)
    if (!file.exists()) return null

    val mappings: List<ClassMapping> = flattenWithSelf(MappingsFile.read(file)) {
        index++
        val part = parts.getOrNull(index) ?: return@flattenWithSelf null
        // If the inner class name doesn't exist then we don't have an int name to give
        this.innerClasses.find { it.displayedName == part } ?: return@getClassIntermediaryName null
    }
    return mappings.joinToString("$") { it.obfuscatedName }
}

/** User input is in named but the repo is in intermediary */
private fun <T : Name> T.remapParameterDescriptorsToInt(platform: ExtendedPlatform): T = when (this) {
    is MethodName -> copy(
        parameterTypes = parameterTypes.map { type -> type.remap { platform.getClassIntermediaryName(it) } }
    ) as T
    is ParamName -> copy(methodIn = methodIn.remapParameterDescriptorsToInt(platform)) as T
    else -> this
}

suspend fun ExtendedPlatform.commitChanges(path: String, mappings: Mapping, commitMessage: String) {
    val mappingsFile = mappings.root
    mappingsFile.writeTo(yarnRepo.getMappingsFile(path))
    yarnRepo.stageMappingsFile(path)
    yarnRepo.commitChanges(commitMessage)

    ActiveMappings.update(mappingsFile)
}
