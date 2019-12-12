package cloak.actions

import cloak.format.descriptor.remap
import cloak.format.mappings.*
import cloak.format.rename.*
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

object RenameAction {
    suspend fun rename(
        platform: ExtendedPlatform,
        nameBeforeRenames: Name,
        isTopLevelClass: Boolean
    ): Errorable<NewName> {
        val result = platform.renameInner(nameBeforeRenames, isTopLevelClass)

        if (result is StringSuccess) {
            println("$nameBeforeRenames was renamed to ${result.value}")
        } else {
            println("Could not rename: $result")
        }

        return result
    }

    private suspend fun ExtendedPlatform.failWithErrorMessage(message: String) = fail<NewName>(message)
        .also { showErrorPopup(message, title = "Cannot rename") }


    /**
     * Returns the new name
     */
    // Note: there's no need for the ability to rename top level classes just by their short name anymore.
    private suspend fun ExtendedPlatform.renameInner(nameBeforeRenames: Name, isTopLevelClass: Boolean): Errorable<NewName> {

        return coroutineScope {

            val oldName = nameBeforeRenames.updateAccordingToRenames(this@renameInner)

            val matchingMapping = findMatchingMapping(oldName)
                ?: return@coroutineScope  failWithErrorMessage("Cannot rename this")


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

        val user = getAuthenticatedUser()!!
        renameTarget.root.writeTo(newMappingLocation)
        yarnRepo.stageMappingsFile(newPath)
        yarnRepo.commitChanges(commitMessage = "$presentableOldName -> $presentableNewName")

        //TODO
//        appendYarnChange(
//            branch = currentBranch,
//            change = Change(
//                oldName = presentableOldName,
//                newName = presentableNewName,
//                explanation = rename.explanation
//            )
//        )
    }







    private fun ExtendedPlatform.updateNamedIntermediaryMap(renameTarget: Mapping) {
        if (renameTarget is ClassMapping) {
            setIntermediaryName(
                renameTarget.deobfuscatedName ?: error("A name was unexpectedly not given to $renameTarget"),
                renameTarget.obfuscatedName
            )
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
        is ClassMapping -> if (parent == null) newMappingLocation.exists() else parent.innerClasses.anythingElseHasTheSameObfNameAs(
            this
        )
        // With methods you can overload the same name as long as the descriptor is different
        is MethodMapping -> parent.methods.any { it !== this && it.deobfuscatedName == deobfuscatedName && it.descriptor == descriptor }
        is FieldMapping -> parent.fields.anythingElseHasTheSameObfNameAs(this)
        is ParameterMapping -> parent.parameters.anythingElseHasTheSameObfNameAs(this)
    }
}