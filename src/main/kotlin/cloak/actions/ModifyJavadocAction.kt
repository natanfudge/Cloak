package cloak.actions


import cloak.format.mappings.getFilePath
import cloak.format.mappings.multilineComment
import cloak.format.mappings.readableName
import cloak.format.mappings.writeTo
import cloak.format.rename.Name
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.PlatformInputValidator
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.coroutineScope

object ModifyJavadocAction {
    suspend fun modify(platform: ExtendedPlatform, nameBeforeRenames: Name): Boolean = with(platform) {
        coroutineScope {
            val name = nameBeforeRenames.updateAccordingToRenames(this@with)

            val matchingMapping = findMatchingMapping(name).getOrElse { return@coroutineScope failWithErrorMessage(it) }

            val oldJavadoc = matchingMapping.multilineComment
            val newJavadoc = getMultilineInput(
                title = "Modify Javadoc", message = "Enter documentation", validator =
                PlatformInputValidator(allowEmptyString = false), initialValue = oldJavadoc
            ) ?: return@coroutineScope false

            matchingMapping.comment = newJavadoc.split("\n").toMutableList()

            val path = matchingMapping.getFilePath()
            val presentableName = matchingMapping.readableName()

            matchingMapping.root.writeTo(yarnRepo.getMappingsFile(path))
            yarnRepo.stageMappingsFile(path)
            val charChange = newJavadoc.length - oldJavadoc.length
            val changeSymbol = if (charChange >= 0) "+" else ""

            yarnRepo.commitChanges(commitMessage = "$changeSymbol$charChange doc in $presentableName")

            true
        }
    }

    private suspend fun ExtendedPlatform.failWithErrorMessage(message: String) =
        showErrorPopup(message, title = "Cannot modify javadoc").run { false }
}