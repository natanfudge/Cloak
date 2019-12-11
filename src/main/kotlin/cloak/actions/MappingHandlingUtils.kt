package cloak.actions


import cloak.format.descriptor.remap
import cloak.format.mappings.Mapping
import cloak.format.rename.*
import cloak.git.currentBranch
import cloak.git.setCurrentBranchToDefaultIfNeeded
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.saved.*
import cloak.util.*
import kotlinx.coroutines.*

suspend fun ExtendedPlatform.warmupAuthAndYarn() = coroutineScope<Errorable<Deferred<Unit>>> {
    val user = getGitUser() ?: return@coroutineScope fail("User didn't provide git info")

    getAuthenticatedUsername() ?: return@coroutineScope fail("User did not provide auth info")
    setCurrentBranchToDefaultIfNeeded(user)

    val promise = async { yarnRepo.warmup() }
    if (!showedNoteAboutLicense) {
        showMessageDialog(
            message = """Yarn mappings are licensed under a permissive license and should stay so.
| If you know the name of something from another, less permissive mappings 
| set (such as MCP(Forge) or Mojang Proguard output), DO NOT use that name.""".trimMargin(),
            title = "Warning"
        )
        showedNoteAboutLicense = true
    }

    promise.success
}

/**
 * After the player renames something, the yarn repository has different information than what is in the editor.
 * This updates the information in the editor to be up to date to the repo.
 * */
fun Name.updateAccordingToRenames(platform: ExtendedPlatform): Name {
    fun ClassName.updateAccordingToRenames(): ClassName =
        platform.getRenamedTo(this)?.let {
            var newName = copy(className = it.newName)
            if (it.newPackageName != null) newName = newName.copy(packageName = it.newPackageName)
            newName
        } ?: this

    fun FieldName.updateAccordingToRenames(): FieldName {
        val newClassName = classIn.updateAccordingToRenames()
        return platform.getRenamedTo(this)?.let { copy(fieldName = it.newName, classIn = newClassName) }
            ?: copy(classIn = newClassName)
    }

    fun MethodName.updateAccordingToRenames(): MethodName {
        val newClassName = classIn.updateAccordingToRenames()
        return platform.getRenamedTo(this)?.let { copy(methodName = it.newName, classIn = newClassName) }
            ?: copy(classIn = newClassName)
    }

    fun ParamName.updateAccordingToRenames(): ParamName {
        val newMethodName = methodIn.updateAccordingToRenames()
        return platform.getRenamedTo(this)?.let {
            copy(paramName = it.newName, methodIn = newMethodName)
        } ?: copy(methodIn = newMethodName)
    }

    return when (this) {
        is ClassName -> updateAccordingToRenames()
        is FieldName -> updateAccordingToRenames()
        is MethodName -> updateAccordingToRenames()
        is ParamName -> updateAccordingToRenames()
    }
}

suspend fun ExtendedPlatform.findMatchingMapping(name: Name): Mapping? {
    // Start cloning the repo early while the user is reading
    val repoPromise = when (val repoResult = warmupAuthAndYarn()) {
        is StringSuccess -> repoResult.value
        is StringError -> return null
    }

    return asyncWithText("Preparing rename...") {
        repoPromise.await()
        //TODO: remove
        yarnRepo.fixOriginUrl()
        switchToCorrectBranch()
        val namedToIntermediaryClasses = getNamedToIntermediary()

        val oldName = name.remapParameterDescriptors(namedToIntermediaryClasses)

        oldName.getMatchingMappingIn(platform = this, namedToInt = namedToIntermediaryClasses)

    }

}

/**
 * Call this while the user is busy (typing the new name) to prevent lag later on.
 * This method will be executed asynchronously so it will return immediately and do the work in the background.
 */
private suspend fun ExtendedPlatform.switchToCorrectBranch() = withContext(Dispatchers.IO) {
    yarnRepo.switchToBranch(currentBranch)
}

/** User input is in named but the repo is in intermediary */
private fun <T : Name> T.remapParameterDescriptors(namedToIntermediary: Map<String, String>): T = when (this) {
    is MethodName -> copy(
        parameterTypes = parameterTypes.map { it.remap(namedToIntermediary) }
    ) as T
    is ParamName -> copy(methodIn = methodIn.remapParameterDescriptors(namedToIntermediary)) as T
    else -> this
}

