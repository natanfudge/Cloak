package cloak.format.rename

import cloak.git.YarnRepo
import cloak.git.yarnRepoDir
import cloak.format.descriptor.remap
import cloak.format.mappings.*
import cloak.platform.ExtendedPlatform
import cloak.platform.PlatformInputValidator
import cloak.platform.UserInputRequest
import cloak.platform.saved.*
import cloak.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.lang.model.SourceVersion

object Renamer {

    private suspend fun ExtendedPlatform.failWithErrorMessage(message: String) = fail<NewName>(
        message
    ).also {
        showErrorPopup(message, title = "Cannot rename")
    }

    /**
     * Returns the new name
     */
    suspend fun ExtendedPlatform.rename( nameBeforeRenames: Name, isTopLevelClass: Boolean): Errorable<NewName> {
        val user = getGitUser() ?: return fail("User didn't provide git info")
        val oldName = nameBeforeRenames.updateAccordingToRenames(renamedNames)

        val matchingMapping = findMatchingMapping(user, oldName)
            ?: return failWithErrorMessage("This was already renamed or doesn't exist in a newer version.")


        val (newFullName, explanation) = requestRenameInput { validateUserInput(it, isTopLevelClass,matchingMapping.typeName()) }
            ?: return fail("User didn't input a new name")

        val (packageName, newShortName) = splitPackageAndName(newFullName)

        if (packageName != null && (oldName !is ClassName || oldName.classIn != null)) {
            return failWithErrorMessage("Changing the package name can only be done on top level classes.")
        }

        return asyncWithText("Renaming...") {
            val renameInstance = RenameInstance(oldName, newShortName, packageName, explanation)

            when (val result = applyRename(renameInstance, matchingMapping)) {
                is StringSuccess -> {
                    val newName = NewName(newShortName, packageName)
                    renamedNames[nameBeforeRenames] = newName
                    newName.success
                }
                is StringError -> {
                    showErrorPopup(message = result.value, title = "Rename Error")
                    result.map { NewName("", null) }
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

        updateNamedIntermediaryMap(renameTarget)

        val newPath = renameTarget.getFilePath()
        val newMappingLocation = YarnRepo.at(yarnRepoDir).getMappingsFile(newPath)


        if (renameTarget.duplicatesAnotherMapping(newMappingLocation)) {
            return fail("There's another ${renameTarget.typeName()} named that way already.")
        }
        val presentableNewName = renameTarget.readableName()

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
        val yarn = YarnRepo.at(yarnRepoDir)
        if (oldPath != newPath) yarn.removeMappingsFile(oldPath)

        val user = getGitUser()!!
        renameTarget.root.writeTo(newMappingLocation)
        yarn.stageMappingsFile(newPath)
        yarn.commitChanges(author = user, commitMessage = "$presentableOldName -> $presentableNewName")

        appendYarnChange(
            //TODO: modify this when you can switch between branches
            branch = user.branchName,
            change = Change(
                oldName = presentableOldName,
                newName = presentableNewName,
                explanation = rename.explanation
            )
        )
    }

    /** User input is in named but the repo is in intermediary */
    private fun <T> T.remapParameterDescriptors(namedToIntermediary: Map<String, String>): T = when (this) {
        is MethodName -> copy(
            parameterTypes = parameterTypes.map { it.remap(namedToIntermediary) }
        ) as T
        is ParamName -> copy(index, methodIn.remapParameterDescriptors(namedToIntermediary)) as T
        else -> this
    }

    /**
     * After the player renames something, the yarn repository has different information than what is in the editor.
     * This updates the information in the editor to be up to date to the repo.
     * */
    private fun Name.updateAccordingToRenames(renames: Map<Name, NewName>) = when (this) {
        is ClassName -> updateAccordingToRenames(renames)
        is FieldName -> updateAccordingToRenames(renames)
        is MethodName -> updateAccordingToRenames(renames)
        is ParamName -> updateAccordingToRenames(renames)
    }

    private fun ClassName.updateAccordingToRenames(renames: Map<Name, NewName>): ClassName =
        renames[this]?.let {
            var newName = copy(className = it.newName)
            if (it.newPackageName != null) newName = newName.copy(packageName = it.newPackageName)
            newName
        } ?: this

    private fun FieldName.updateAccordingToRenames(renames: Map<Name, NewName>): FieldName {
        val newClassName = classIn.updateAccordingToRenames(renames)
        return renames[this]?.let { copy(fieldName = it.newName, classIn = newClassName) }
            ?: copy(classIn = newClassName)
    }

    private fun MethodName.updateAccordingToRenames(renames: Map<Name, NewName>): MethodName {
        val newClassName = classIn.updateAccordingToRenames(renames)
        return renames[this]?.let { copy(methodName = it.newName, classIn = newClassName) }
            ?: copy(classIn = newClassName)
    }

    private fun ParamName.updateAccordingToRenames(renames: Map<Name, NewName>) =
        copy(methodIn = methodIn.updateAccordingToRenames(renames))


    private fun ExtendedPlatform.updateNamedIntermediaryMap(renameTarget: Mapping) {
        if (renameTarget is ClassMapping) {
            setIntermediaryName(
                renameTarget.deobfuscatedName ?: error("A name was unexpectedly not given to $renameTarget"),
                renameTarget.obfuscatedName
            )
        }
    }

    private suspend fun ExtendedPlatform.findMatchingMapping(
        user: GitUser,
        name: Name
    ): Mapping? {
        return asyncWithText("Preparing rename...") {
            switchToUserBranch(gitUser = user, yarnRepo = yarnRepoDir)
            val namedToIntermediaryClasses = getNamedToIntermediary(YarnRepo.at(yarnRepoDir))
            val yarn = YarnRepo.at(yarnRepoDir)

            val oldName = name.remapParameterDescriptors(namedToIntermediaryClasses)

            oldName.getMatchingMappingIn(yarn, platform = this, namedToInt = namedToIntermediaryClasses)

        }
    }

    private suspend fun ExtendedPlatform.requestRenameInput(newNameValidator: (String) -> String?): Pair<String, String?>? {
        val (newName, explanation) = getTwoInputs(
            message = null, request = UserInputRequest.NewName, descriptionA = "New Name", descriptionB = "Explanation",
            validatorA = PlatformInputValidator(allowEmptyString = false, tester = newNameValidator),
            validatorB = PlatformInputValidator(allowEmptyString = true)
        ) ?: return null

        return Pair(newName, if (explanation == "") null else explanation)
    }


    /**
     * Call this while the user is busy (typing the new name) to prevent lag later on.
     * This method will be executed asynchronously so it will return immediately and do the work in the background.
     */
    private suspend fun switchToUserBranch(gitUser: GitUser, yarnRepo: File): Unit = withContext(Dispatchers.IO) {
        val yarn = YarnRepo.at(yarnRepo)
        yarn.switchToBranch(gitUser.branchName)
    }


    /**
     * Returns a string if [userInputForNewName] invalid, null if valid.
     * @param isTopLevelClass whether the element to rename is a top level class
     */
    private fun validateUserInput(userInputForNewName: String, isTopLevelClass: Boolean,mappingType : String): String? {
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

