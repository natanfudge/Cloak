package cloak.actions

import cloak.format.rename.shortName
import cloak.git.GithubApi
import cloak.git.YarnRepo
import cloak.git.inSubmittedBranch
import cloak.git.yarnRepo
import cloak.platform.*
import cloak.platform.saved.updateIntermediaryNamesToVersion
import org.eclipse.jgit.lib.Repository

private const val useDebugRepo = true

object SubmitAction {
    suspend fun submit(platform: ExtendedPlatform) = with(platform) {

        try {
            val gitUser = getAuthenticatedUser()
            if (inSubmittedBranch()) {
                asyncWithText("Pushing changes...") {
                    push(it)
                }
                return
            }

            val (prTitle, prBody) = getTwoInputs(
                message = "Specify title and body for opened mappings PR",
                request = UserInputRequest.PullRequest,
                inputA = InputFieldData(
                    description = "Title",
                    multiline = false,
                    validator = PlatformInputValidator(allowEmptyString = false)
                ),
                inputB = InputFieldData(description = "Body", multiline = true)
            ) ?: return

            val upstreamOwner = if (useDebugRepo) "shedaniel" else YarnRepo.UpstreamUsername
            val newBranchName = Repository.normalizeBranchName(prTitle)
            val pr = createPr(prTitle, prBody ?: "", newBranchName, gitUser, upstreamOwner) ?: return

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

private suspend fun ExtendedPlatform.push(asyncContext: AsyncContext) {
    asyncContext.changeText("Pushing changes...") {
        yarnRepo.push()
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
    prTitle: String,
    prBody: String,
    newBranchName: String,
    gitUser: GitUser,
    upstreamOwner: String
): PullRequestResponse? = asyncWithText("Submitting...") { context ->

    val body = constructPrBody(prBody)
    yarnRepo.switchToBranch(newBranchName, startFromBranch = gitUser.branchName)
    push(context)
    val response = context.changeText("Opening pull request...") {
        createPullRequest(
            repositoryName = "yarn",
            targetUser = upstreamOwner,
            targetBranch = GithubApi.getDefaultBranch("yarn", upstreamOwner),
            requestingBranch = newBranchName,
            requestingUser = getAuthenticatedUser().name,
            title = prTitle,
            body = body
        )
    }

//    if (response == null) {
//        context.changeText("Switching branch...") {
//            yarnRepo.switchToBranch(gitUser.branchName)
//        }
//    }

//    if (response != null) {
    branch.migrateInfo(oldBranch = gitUser.branchName, newBranch = newBranchName)
//    }

    response
}

private suspend fun ExtendedPlatform.constructPrBody(writtenBody: String): String {
    val changes = branch.getRenames()
    val introduction = if (writtenBody == "") "" else writtenBody + "\n"
    return introduction + changes.filter { (_, new) -> new.explanation != null }.map { (old, new) ->
        """- ${old.shortName} -> ${if (new.packageName != null) new.packageName + "/" else "" + new.name}
  - ${new.explanation}"""

    }.joinToString("  \n")
}