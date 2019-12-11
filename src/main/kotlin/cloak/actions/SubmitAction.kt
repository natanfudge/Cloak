package cloak.actions

import cloak.git.GithubApi
import cloak.git.YarnRepo
import cloak.git.inSubmittedBranch
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.PlatformInputValidator
import cloak.platform.PullRequestResponse
import cloak.platform.saved.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Repository

private const val useDebugRepo = false

object SubmitAction {
    fun submit(platform: ExtendedPlatform) = with(platform) {

        GlobalScope.launch {
            val gitUser = getGitUser() ?: return@launch
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

            val upstreamOwner = if (useDebugRepo) "natanfudge" else YarnRepo.UpstreamUsername
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

        }
    }
}

private suspend fun ExtendedPlatform.resetWorkspace(gitUser: GitUser) {
    val repo = yarnRepo
    asyncWithText("Cleaning...") {
        resetNamedToIntermediary()
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
): PullRequestResponse? = asyncWithText("Submitting...") {


    yarnRepo.switchToBranch(newBranchName, startFromBranch = gitUser.branchName)
    yarnRepo.push()
    val response = getAuthenticatedUsername()?.let {
        createPullRequest(
            repositoryName = "yarn",
            targetUser = upstreamOwner,
            targetBranch = GithubApi.getDefaultBranch("yarn", upstreamOwner),
            requestingBranch = newBranchName,
            requestingUser = it,
            title = prName,
            body = constructPrBody(gitUser.branchName)
        )
    }
    if (response == null) {
        yarnRepo.switchToBranch(gitUser.branchName)

    }



    if (response != null) {
        //TODO
//        migrateYarnChangeList(oldBranch = gitUser.branchName, newBranch = newBranchName)
        migrateRenamedNamesBranch(oldBranch = gitUser.branchName, newBranch = newBranchName)
    }

    response
}


//TODO
private fun ExtendedPlatform.constructPrBody(branchName: String): String {
//    val changes = allChangesOfBranch(branchName)
//    return changes.filter { it.explanation != null }.joinToString("  \n") { change ->
//        """- ${change.oldName} -> ${change.newName}
//  - ${change.explanation}"""
//
//    }
    return ""
}