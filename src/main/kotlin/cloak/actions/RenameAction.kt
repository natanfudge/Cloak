package cloak.actions

import cloak.format.mappings.*
import cloak.format.rename.*
import cloak.git.yarnRepo
import cloak.platform.*
import cloak.platform.saved.NewName
import cloak.platform.saved.RenameResult
import cloak.util.fail
import cloak.util.success
import com.github.michaelbull.result.*
import kotlinx.coroutines.coroutineScope
import java.io.File
import javax.lang.model.SourceVersion

object RenameAction {
    suspend fun rename(
        platform: ExtendedPlatform,
        nameBeforeRenames: Name,
        isTopLevelClass: Boolean
    ): RenameResult {
        val result: RenameResult = try {
            platform.renameInner(nameBeforeRenames, isTopLevelClass)
        } catch (e: UserNotAuthenticatedException) {
            Err("User not authenticated")
        }

        if (result is Ok<NewName>) {
            println("$nameBeforeRenames was renamed to ${result.value}")
            platform.branch.acceptRenamedName(nameBeforeRenames, result.value)
        } else println("Could not rename: $result")

        return result
    }

    private suspend fun ExtendedPlatform.failWithErrorMessage(message: String) = Err(message)
        .also { showErrorPopup(message, title = "Cannot rename") }


    /**
     * Returns the new name
     */
    // Note: there's no need for the ability to rename top level classes just by their short name anymore.
    private suspend fun ExtendedPlatform.renameInner(
        nameBeforeRenames: Name,
        isTopLevelClass: Boolean
    ): RenameResult {

        return coroutineScope {
            val oldName = nameBeforeRenames.updateAccordingToRenames(this@renameInner)

            val matchingMapping =
                findMatchingMapping(oldName).getOrElse { return@coroutineScope failWithErrorMessage(it) }

            val (newFullName, explanation) = requestRenameInput(oldName) {
                validateUserInput(it, isTopLevelClass, matchingMapping)
            } ?: return@coroutineScope Err("User didn't input a new name")

            val (packageName, newShortName) = splitPackageAndName(newFullName)

            if (packageName != null && (oldName !is ClassName || oldName.classIn != null)) {
                return@coroutineScope failWithErrorMessage("Changing the package name can only be done on top level classes.")
            }

            asyncWithText("Renaming...") {
                val newName = NewName(name = newShortName, packageName = packageName, explanation = explanation)

                applyRename(matchingMapping, oldName, newName).map { newName }
                    .onFailure { showErrorPopup(message = it, title = "Rename Error") }

            }
        }

    }


    private suspend fun ExtendedPlatform.applyRename(
        renameTarget: Mapping, oldName: Name, newName: NewName
    ): Result<Unit, String> {
        val oldPath = renameTarget.getFilePath()
        val presentableOldName = renameTarget.readableName()
        val result = renameMappings(renameTarget, oldName, newName)
        result.onFailure { return result }

        val newPath = renameTarget.getFilePath()

        if (renameTarget.duplicatesAnotherMapping(yarnRepo.getMappingsFile(newPath))) {
            return fail("There's another ${renameTarget.typeName()} named '${renameTarget.displayedName}' already.")
        }

        val presentableNewName = renameTarget.displayedName

        if (oldPath != newPath) yarnRepo.removeMappingsFile(oldPath)

        commitChanges(
            path = newPath,
            mappings = renameTarget,
            commitMessage = "$presentableOldName -> $presentableNewName"
        )

        println("Changes commited successfully!")

        return success()
    }

    fun renameMappings(mappings: Mapping, oldName: Name, newName: NewName): Result<Unit, String> {
        return if (newName.packageName != null) {
            // Changing the package can only be done on top-level classes
            assert(oldName is ClassName) { "It should be verified that package rename can only be done on classes" }
            mappings as ClassMapping

            mappings.deobfuscatedName = "${newName.packageName}/${newName.name}"
            success()
        } else rename(mappings, newName.name)
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
            message = null, request = UserInputRequest.NewName,
            inputA = InputFieldData(
                description = "New Name",
                validator = PlatformInputValidator(allowEmptyString = false, tester = newNameValidator),
                initialValue = initialValue,
                defaultSelection = defaultSelectionRange
            ),
            inputB = InputFieldData(
                description = "Explanation",
                validator = PlatformInputValidator(allowEmptyString = true)
            ),
            helpId = "Rename-Action"

        ) ?: return null

        return Pair(newName, if (explanation == "") null else explanation)
    }


    /**
     * Returns a string if [userInputForNewName] invalid, null if valid.
     * @param isTopLevelClass whether the element to rename is a top level class
     */
    private fun validateUserInput(
        userInputForNewName: String,
        isTopLevelClass: Boolean,
        targetMapping: Mapping
    ): String? {
        val (packageName, shortName) = splitPackageAndName(userInputForNewName)

        if (userInputForNewName == targetMapping.displayedName) return "The ${targetMapping.typeName()} is already called '$userInputForNewName' in the latest yarn version"

        if (!isTopLevelClass && packageName != null) return "Package rename can only be done on top-level classes"

        for (part in packageName?.split("/") ?: listOf()) {
            if (!SourceVersion.isIdentifier(part)) return "'$part' is not a valid package name"
        }

        if (!SourceVersion.isName(shortName)) return "'$shortName' is not a valid ${targetMapping.typeName()} name"

        return null
    }

    private fun Mapping.duplicatesAnotherMapping(newMappingLocation: File): Boolean = when (this) {
        is ClassMapping -> parent?.innerClasses?.anythingElseHasTheSameObfNameAs(
            this
        ) ?: newMappingLocation.exists()
        // With methods you can overload the same name as long as the descriptor is different
        is MethodMapping -> parent.methods.any { it !== this && it.deobfuscatedName == deobfuscatedName && it.descriptor == descriptor }
        is FieldMapping -> parent.fields.anythingElseHasTheSameObfNameAs(this)
        is ParameterMapping -> parent.parameters.anythingElseHasTheSameObfNameAs(this)
    }
}