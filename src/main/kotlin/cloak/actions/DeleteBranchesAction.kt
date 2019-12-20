package cloak.actions

//import cloak.platform.saved.allBranches
//import cloak.platform.saved.deleteRenamesNamesOfBranch
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.UserNotAuthenticatedException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.log4j.LogManager

val Logger = LogManager.getLogger("Cloak")

object DeleteBranchesAction {
    fun delete(platform: ExtendedPlatform) = with(platform) {
        GlobalScope.launch {
            try {
                val default = getAuthenticatedUser().branchName
                val branches = platform.branch.all.filter { it != default }.toList()
                val chosen = getMultipleChoicesBetweenOptions("Choose Branches to delete", branches)
                for (branch in chosen) {
                    asyncWithText("Deleting $branch...") {
                        yarnRepo.deleteBranch(branch)
                        platform.branch.deleteBranch(branch)
                    }
                }
            } catch (e: UserNotAuthenticatedException) {
                Logger.warn("User not authenticated", e)
            }

        }
    }
}