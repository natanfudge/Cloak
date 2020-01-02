package cloak.actions

import cloak.git.yarnRepo
import cloak.platform.ActiveMappings
import cloak.platform.ExtendedPlatform
import cloak.platform.UserNotAuthenticatedException
//import cloak.platform.saved.allBranches
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object SwitchBranchAction {
    fun switch(platform: ExtendedPlatform) = with(platform) {
        GlobalScope.launch {
            try {
                val user = getAuthenticatedUser()

                val mainBranchLabel = "${user.branchName} (Main)"
                val options = mutableListOf(mainBranchLabel)
                options.addAll(branch.all.filter { it != user.branchName })
                options.removeIf { it == yarnRepo.currentBranch || it == "${yarnRepo.currentBranch} (Main)" }

                val branch = platform.getChoiceBetweenOptions("Switch from ${yarnRepo.currentBranch}", options)
                    .let { if (it == mainBranchLabel) user.branchName else it }
                asyncWithText("Switching...") {
                    yarnRepo.switchToBranch(branchName = branch)
                    ActiveMappings.deactivate()
                }
            } catch (e: UserNotAuthenticatedException) {
                Logger.warn("User not authenticated", e)
            }

        }
    }
}