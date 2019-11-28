package cloak.actions

import cloak.git.currentBranch
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.saved.allBranches
import cloak.platform.saved.getGitUser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object SwitchBranchAction {
    fun switch(platform: ExtendedPlatform) = with(platform) {
        GlobalScope.launch {
            val user = getGitUser() ?: return@launch
            val options = (allBranches + user.branchName).filter { it != currentBranch }
            val branch = platform.getChoiceBetweenOptions("Switch from $currentBranch", options)
            asyncWithText("Switching...") {
                yarnRepo.switchToBranch(branchName = branch)
            }
        }
    }
}