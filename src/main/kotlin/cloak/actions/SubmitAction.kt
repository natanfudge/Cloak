package cloak.actions

import cloak.git.*
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
            if (inSubmittedBranch) {
                asyncWithText("Submitting") {
                    yarnRepo.push()
                }
                return@launch
            }

            val prName = getUserInput(
                title = "Pull Request",
                message = "Enter name for mappings set",
                validator = PlatformInputValidator(allowEmptyString = false)
            ) ?: return@launch

            val upstreamOwner = "natanfudge"//TODO: switch to YarnRepo.upstreamuser
            val newBranchName = Repository.normalizeBranchName(prName)
            val pr = createPr(prName, newBranchName, gitUser, upstreamOwner)

            showMessageDialog(
                title = "Success",
                message = "<html><p>Your mappings have been submitted! Track them <a href=\"${pr.htmlUrl}\">here</a>.</p>\n" +
                        "<p>You can go back and modify your submitted mappings with <b>Tools -> Fabric -> Switch Yarn Branch -> $newBranchName</b></html></p>"
            )

            resetWorkspace(gitUser)
        }
    }
}

private suspend fun ExtendedPlatform.resetWorkspace(gitUser: GitUser) {
    val repo = yarnRepo
    asyncWithText("Cleaning...") {
        repo.switchToBranch("master")
        repo.deleteBranch(gitUser.branchName)
        repo.switchToBranch(gitUser.branchName)
        // This will update the mc version because the 'McVersion' file will be deleted
        updateIntermediaryNamesToVersion(repo.getTargetMinecraftVersion())
    }

}


private suspend fun ExtendedPlatform.createPr(
    prName: String,
    newBranchName: String,
    gitUser: GitUser,
    upstreamOwner: String
): PullRequestResponse = asyncWithText("Submitting...") {

    yarnRepo.switchToBranch(newBranchName, startFromBranch = gitUser.branchName)
    migrateYarnChangeList(oldBranch = gitUser.branchName, newBranch = newBranchName)
    migrateRenamedNamesBranch(oldBranch = gitUser.branchName, newBranch = newBranchName)
    resetNamedToIntermediary()

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
    val changes = allChangesOfBranch(branchName)
    return changes.filter { it.explanation != null }.joinToString("  \n") { change ->
        """- ${change.oldName} -> ${change.newName}  
  - ${change.explanation}"""

    }
}