package cloak.actions

import cloak.git.GithubApi
import cloak.git.PullRequestResponse
import cloak.git.YarnRepo
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.PlatformInputValidator
import cloak.platform.saved.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Repository

object SubmitAction {
    fun submit(platform: ExtendedPlatform) = with(platform) {

        GlobalScope.launch {
            val gitUser = getGitUser() ?: return@launch

            val prName = getUserInput(
                title = "Pull Request",
                message = "Enter name for mappings set",
                validator = PlatformInputValidator(allowEmptyString = false)
            ) ?: return@launch

            val upstreamOwner = "natanfudge"//TODO: switch to YarnRepo.upstreamuser
            val pr = createPr(prName, gitUser, upstreamOwner)

            showMessageDialog(
                title = "Success",
                message = "<html>Your mappings have been submitted! Track them <a href=\"${pr.htmlUrl}\">here</a>.</html>"
            )

            resetWorkspace(gitUser)
        }
    }
}

private suspend fun ExtendedPlatform.resetWorkspace(gitUser: GitUser) {
    val repo = yarnRepo
    asyncWithText("Cleaning...") {
        repo.switchToBranch("master", isSubmittedBranch = false)
        repo.deleteBranch(gitUser.branchName)
        repo.switchToBranch(gitUser.branchName, isSubmittedBranch = false)
        // This will update the mc version because the 'McVersion' file will be deleted
        updateIntermediaryNamesToVersion(repo.getTargetMinecraftVersion())
    }

}

private suspend fun ExtendedPlatform.createPr(
    prName: String,
    gitUser: GitUser,
    upstreamOwner: String
): PullRequestResponse = asyncWithText("Submitting...") {
    val newBranchName = Repository.normalizeBranchName(prName)
    yarnRepo.switchToBranch(newBranchName, startFromBranch = gitUser.branchName, isSubmittedBranch = true)
    migrateYarnChangeList(oldBranch = gitUser.branchName, newBranch = newBranchName)
    migrateRenamedNamesBranch(oldBranch = gitUser.branchName, newBranch = newBranchName)

    yarnRepo.push()
    GithubApi.createPullRequest(
        repositoryName = "yarn",
        targetUser = upstreamOwner,
        targetBranch = GithubApi.getDefaultBranch("yarn", upstreamOwner),
        requestingBranch = newBranchName,
        requestingUser = YarnRepo.GithubUsername,
        title = prName,
        body = constructPrBody(newBranchName)
    )

}


private fun ExtendedPlatform.constructPrBody(branchName: String): String {
    //TODO: change branch name when we can switch between branches
    val changes = allChangesOfBranch(branchName)
    return changes.filter { it.explanation != null }.joinToString("  \n") { change ->
        """- ${change.oldName} -> ${change.newName}  
  - ${change.explanation}"""

    }
}