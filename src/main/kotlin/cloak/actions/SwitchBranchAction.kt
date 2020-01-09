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
                // Removeall with a filtered list instead of just directly accepting a lambda because this way it's an inline function
                // and can use suspend
                options.removeAll(options.filter { it == yarnRepo.getCurrentBranch() || it == "${yarnRepo.getCurrentBranch()} (Main)" })

                val branch = platform.getChoiceBetweenOptions("Switch from ${yarnRepo.getCurrentBranch()}", options)
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