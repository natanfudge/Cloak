package cloak.actions

import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.saved.allBranches
import cloak.platform.saved.deleteRenamesNamesOfBranch
//import cloak.platform.saved.deleteYarnChangesOfBranch
import cloak.platform.saved.getDefaultUserBranch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object DeleteBranchesAction {
    fun delete(platform: ExtendedPlatform) = with(platform) {
        GlobalScope.launch {
            val default = getDefaultUserBranch()
            val branches = allBranches.filter { it != default }.toList()
            val chosen = getMultipleChoicesBetweenOptions("Choose Branches to delete", branches)
            for (branch in chosen) {
                asyncWithText("Deleting $branch...") {
                    yarnRepo.deleteBranch(branch)
                    deleteRenamesNamesOfBranch(branch)
                    //TODO
//                    deleteYarnChangesOfBranch(branch)
                }
            }
        }
    }
}