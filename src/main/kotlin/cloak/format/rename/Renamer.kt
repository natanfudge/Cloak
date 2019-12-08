package cloak.format.rename

import cloak.format.descriptor.remap
import cloak.format.mappings.*
import cloak.git.currentBranch
import cloak.git.setCurrentBranchToDefaultIfNeeded
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.PlatformInputValidator
import cloak.platform.UserInputRequest
import cloak.platform.saved.*
import cloak.util.*
import kotlinx.coroutines.*
import java.io.File
import javax.lang.model.SourceVersion

//TODO: need some way to reduce the amount of mutable state and global variables

object Renamer {

    private suspend fun ExtendedPlatform.failWithErrorMessage(message: String) = fail<NewName>(message)
        .also { showErrorPopup(message, title = "Cannot rename") }

    /**
     * Returns the new name
     */
    // Note: there's no need for the ability to rename top level classes just by their short name anymore.
    suspend fun ExtendedPlatform.rename(nameBeforeRenames: Name, isTopLevelClass: Boolean): Errorable<NewName> {
        val user = getGitUser() ?: return fail("User didn't provide git info")
        setCurrentBranchToDefaultIfNeeded(user)

        return coroutineScope {
            // Start cloning the repo early while the user is reading
            val repo = async { yarnRepo.warmup() }

            if (!showedNoteAboutLicense) {
                showMessageDialog(
                    message = """Yarn mappings are licensed under a permissive license and should stay so.
| If you know the name of something from another, less permissive mappings 
| set (such as MCP(Forge) or Mojang Proguard output), DO NOT use that name.""".trimMargin(),
                    title = "Warning"
                )
                showedNoteAboutLicense = true
            }
            val oldName = nameBeforeRenames.updateAccordingToRenames(this@rename)

            val matchingMapping = findMatchingMapping(oldName, repo)
            //TODO: specialized error for each case
                ?: return@coroutineScope failWithErrorMessage("This was already renamed, is automatically named, is a non-mc method, or doesn't exist in a newer version.")


            val (newFullName, explanation) = requestRenameInput(oldName) {
                validateUserInput(
                    it,
                    isTopLevelClass,
                    matchingMapping.typeName()
                )
            } ?: return@coroutineScope fail<NewName>("User didn't input a new name")

            val (packageName, newShortName) = splitPackageAndName(newFullName)

            if (packageName != null && (oldName !is ClassName || oldName.classIn != null)) {
                return@coroutineScope failWithErrorMessage("Changing the package name can only be done on top level classes.")
            }

            asyncWithText("Renaming...") {
                val renameInstance = RenameInstance(oldName, newShortName, packageName, explanation)

                when (val result = applyRename(renameInstance, matchingMapping)) {
                    is StringSuccess -> {
                        val newName = NewName(newShortName, packageName)

                        setRenamedTo(nameBeforeRenames, newName)
                        newName.success
                    }
                    is StringError -> {
                        showErrorPopup(message = result.value, title = "Rename Error")
                        result.map { NewName("", null) }
                    }
                }

            }
        }

    }


    private suspend fun ExtendedPlatform.applyRename(
        rename: RenameInstance,
        renameTarget: Mapping
    ): Errorable<Unit> {
        val oldPath = renameTarget.getFilePath()
        val presentableOldName = renameTarget.readableName()
        val result = rename.rename(renameTarget)
        if (result is StringError) return result.map { Unit }

        val newPath = renameTarget.getFilePath()
        val newMappingLocation = yarnRepo.getMappingsFile(newPath)

        if (renameTarget.duplicatesAnotherMapping(newMappingLocation)) {
            return fail("There's another ${renameTarget.typeName()} named that way already.")
        }

        updateNamedIntermediaryMap(renameTarget)

        val presentableNewName = renameTarget.nonNullName

        commitChanges(
            oldPath,
            newPath,
            renameTarget,
            newMappingLocation,
            presentableOldName,
            presentableNewName,
            rename
        )

        println("Changes commited successfully!")

        return success()
    }

    private suspend fun ExtendedPlatform.commitChanges(
        oldPath: String,
        newPath: String,
        renameTarget: Mapping,
        newMappingLocation: File,
        presentableOldName: String,
        presentableNewName: String,
        rename: RenameInstance
    ) {
        if (oldPath != newPath) yarnRepo.removeMappingsFile(oldPath)

        val user = getGitUser()!!
        renameTarget.root.writeTo(newMappingLocation)
        yarnRepo.stageMappingsFile(newPath)
        yarnRepo.commitChanges(author = user, commitMessage = "$presentableOldName -> $presentableNewName")

        //TODO: remap the parameter of methods to named
        appendYarnChange(
            branch = currentBranch,
            change = Change(
                oldName = presentableOldName,
                newName = presentableNewName,
                explanation = rename.explanation
            )
        )
    }


    /** User input is in named but the repo is in intermediary */
    private fun <T : Name> T.remapParameterDescriptors(namedToIntermediary: Map<String, String>): T = when (this) {
        is MethodName -> copy(
            parameterTypes = parameterTypes.map { it.remap(namedToIntermediary) }
        ) as T
        is ParamName -> copy(methodIn = methodIn.remapParameterDescriptors(namedToIntermediary)) as T
        else -> this
    }

    /**
     * After the player renames something, the yarn repository has different information than what is in the editor.
     * This updates the information in the editor to be up to date to the repo.
     * */
    private fun Name.updateAccordingToRenames(platform: ExtendedPlatform): Name {
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

        fun ParamName.updateAccordingToRenames() = copy(methodIn = methodIn.updateAccordingToRenames())

        return when (this) {
            is ClassName -> updateAccordingToRenames()
            is FieldName -> updateAccordingToRenames()
            is MethodName -> updateAccordingToRenames()
            is ParamName -> updateAccordingToRenames()
        }
    }


    private fun ExtendedPlatform.updateNamedIntermediaryMap(renameTarget: Mapping) {
        if (renameTarget is ClassMapping) {
            setIntermediaryName(
                renameTarget.deobfuscatedName ?: error("A name was unexpectedly not given to $renameTarget"),
                renameTarget.obfuscatedName
            )
        }
    }

    private suspend fun ExtendedPlatform.findMatchingMapping(name: Name, repoPromise: Deferred<Unit>): Mapping? {
        return asyncWithText("Preparing rename...") {
            repoPromise.await()
            switchToCorrectBranch()
            val namedToIntermediaryClasses = getNamedToIntermediary()

            val oldName = name.remapParameterDescriptors(namedToIntermediaryClasses)

            oldName.getMatchingMappingIn(platform = this, namedToInt = namedToIntermediaryClasses)

        }
    }

    private suspend fun ExtendedPlatform.requestRenameInput(
        oldName: Name,
        newNameValidator: (String) -> String?
    ): Pair<String, String?>? {
        val initialValue = oldName.getOwnName()
        val defaultSelectionRange = if (oldName is ClassName && oldName.isTopLevelClass) {
            (initialValue.lastIndexOf('/') + 1) until initialValue.length
        } else initialValue.indices

        val (newName, explanation) = getTwoInputs(
            message = null, request = UserInputRequest.NewName, descriptionA = "New Name", descriptionB = "Explanation",
            validatorA = PlatformInputValidator(allowEmptyString = false, tester = newNameValidator),
            validatorB = PlatformInputValidator(allowEmptyString = true),
            initialValueA = initialValue,
            defaultSelectionA = defaultSelectionRange
        ) ?: return null

        return Pair(newName, if (explanation == "") null else explanation)
    }


    /**
     * Call this while the user is busy (typing the new name) to prevent lag later on.
     * This method will be executed asynchronously so it will return immediately and do the work in the background.
     */
    private suspend fun ExtendedPlatform.switchToCorrectBranch(): Unit = withContext(Dispatchers.IO) {
        yarnRepo.switchToBranch(currentBranch)
    }


    /**
     * Returns a string if [userInputForNewName] invalid, null if valid.
     * @param isTopLevelClass whether the element to rename is a top level class
     */
    private fun validateUserInput(userInputForNewName: String, isTopLevelClass: Boolean, mappingType: String): String? {
        val (packageName, shortName) = splitPackageAndName(userInputForNewName)

        if (!isTopLevelClass && packageName != null) return "Package rename can only be done on top-level classes"

        for (part in packageName?.split("/") ?: listOf()) {
            if (!SourceVersion.isIdentifier(part)) return "'$part' is not a valid package name"
        }

        if (!SourceVersion.isName(shortName)) return "'$shortName' is not a valid $mappingType name"

        return null
    }

    private fun Mapping.duplicatesAnotherMapping(newMappingLocation: File): Boolean = when (this) {
        is ClassMapping -> if (parent == null) newMappingLocation.exists() else parent.innerClasses.anythingElseHasTheSameObfName()
        // With methods you can overload the same name as long as the descriptor is different
        is MethodMapping -> parent.methods.any { it !== this && it.deobfuscatedName == deobfuscatedName && it.descriptor == descriptor }
        is FieldMapping -> parent.fields.anythingElseHasTheSameObfName()
        is ParameterMapping -> parent.parameters.anythingElseHasTheSameObfName()
    }

}

