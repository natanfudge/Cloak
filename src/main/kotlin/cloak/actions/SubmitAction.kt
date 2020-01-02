package cloak.actions

import cloak.format.rename.shortName
import cloak.git.GithubApi
import cloak.git.YarnRepo
import cloak.git.inSubmittedBranch
import cloak.git.yarnRepo
import cloak.platform.*
import cloak.platform.saved.updateIntermediaryNamesToVersion
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Repository

private const val useDebugRepo = false

object SubmitAction {
    fun submit(platform: ExtendedPlatform) = with(platform) {

        GlobalScope.launch {
            try {
                val gitUser = getAuthenticatedUser()
                if (inSubmittedBranch()) {
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

                val upstreamOwner = if (useDebugRepo) "shedaniel" else YarnRepo.UpstreamUsername
                val newBranchName = Repository.normalizeBranchName(prName)
                val pr = createPr(prName, newBranchName, gitUser, upstreamOwner) ?: return@launch

//            assert(pr.prUrl != null) { "Could not get PR url of PR: $pr" }

                //TODO: modify asyncWithText to allow running concurrently
                showMessageDialog(
                    title = "Success",
                    message = "<html><p>Your mappings have been submitted! Track them <a href=\"${pr.prUrl}\">here</a>.</p>\n" +
                            "<p>You can go back and modify your submitted mappings with <b>Tools -> Fabric -> Switch Yarn Branch -> $newBranchName</b></html></p>"
                )
                resetWorkspace(gitUser)
            } catch (e: UserNotAuthenticatedException) {
                Logger.warn("User not authenticated", e)
            }


        }
    }
}

private suspend fun ExtendedPlatform.resetWorkspace(gitUser: GitUser) {
    val repo = yarnRepo
    asyncWithText("Cleaning...") {
        repo.switchToBranch("master")
        repo.deleteBranch(gitUser.branchName)
        repo.switchToBranch(gitUser.branchName)
        updateIntermediaryNamesToVersion(repo.defaultBranch)
        ActiveMappings.deactivate()
    }

}


private suspend fun ExtendedPlatform.createPr(
    prName: String,
    newBranchName: String,
    gitUser: GitUser,
    upstreamOwner: String
): PullRequestResponse? = asyncWithText("Submitting...") {

    val body = constructPrBody()
    yarnRepo.switchToBranch(newBranchName, startFromBranch = gitUser.branchName)
    yarnRepo.push()
    val response = createPullRequest(
        repositoryName = "yarn",
        targetUser = upstreamOwner,
        targetBranch = GithubApi.getDefaultBranch("yarn", upstreamOwner),
        requestingBranch = newBranchName,
        requestingUser = getAuthenticatedUser().name,
        title = prName,
        body = body
    )

    if (response == null) {
        yarnRepo.switchToBranch(gitUser.branchName)
    }

    if (response != null) {
        branch.migrateInfo(oldBranch = gitUser.branchName, newBranch = newBranchName)
    }

    response
}

private fun ExtendedPlatform.constructPrBody(): String {
    val changes = branch.renames
    return changes.filter { (_, new) -> new.explanation != null }.map { (old, new) ->
        """- ${old.shortName} -> ${if (new.packageName != null) new.packageName + "/" else "" + new.name}
  - ${new.explanation}"""

    }.joinToString("  \n")
}