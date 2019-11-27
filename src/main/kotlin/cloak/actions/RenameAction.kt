package cloak.actions

import cloak.format.rename.Name
import cloak.format.rename.Renamer
import cloak.platform.ExtendedPlatform
import cloak.platform.saved.NewName
import cloak.util.Errorable
import cloak.util.StringSuccess

object RenameAction {
    suspend fun rename(
        platform: ExtendedPlatform,
        nameBeforeRenames: Name,
        isTopLevelClass: Boolean
    ): Errorable<NewName> {
        val result = with(Renamer) {
            platform.rename(nameBeforeRenames, isTopLevelClass)
        }

        if (result is StringSuccess) {
            println("$nameBeforeRenames was renamed to ${result.value}")
        } else {
            println("Could not rename: $result")
        }

        return result
    }
}