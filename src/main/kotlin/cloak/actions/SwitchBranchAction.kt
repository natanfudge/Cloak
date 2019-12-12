package cloak.actions

import cloak.git.currentBranch
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.saved.allBranches
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object SwitchBranchAction {
    fun switch(platform: ExtendedPlatform) = with(platform) {
        GlobalScope.launch {
            val user = getAuthenticatedUser() ?: return@launch

            val mainBranchLabel = "${user.branchName} (Main)"
            val options = mutableListOf(mainBranchLabel)
            options.addAll(allBranches.filter { it != user.branchName })
            options.removeIf { it == currentBranch || it == "$currentBranch (Main)" }

            val branch = platform.getChoiceBetweenOptions("Switch from $currentBranch", options)
                .let { if(it == mainBranchLabel) user.branchName else it }
            asyncWithText("Switching...") {
                yarnRepo.switchToBranch(branchName = branch)
            }
        }
    }
}