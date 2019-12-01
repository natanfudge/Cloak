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

            val mainBranchLabel = "${user.branchName} (Main)"
            val options = mutableListOf(mainBranchLabel)
            options.addAll(allBranches)
            options.removeIf { it == currentBranch || it == "$currentBranch (Main)" }

            val branch = platform.getChoiceBetweenOptions("Switch from $currentBranch", options)
                .let { if(it == mainBranchLabel) user.branchName else it }
            asyncWithText("Switching...") {
                yarnRepo.switchToBranch(branchName = branch)
            }
        }
    }
}