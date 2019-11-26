package cloak.idea.actions

import cloak.git.GithubApi
import cloak.git.PullRequestResponse
import cloak.git.YarnRepo
import cloak.git.yarnRepoDir
import cloak.idea.RenamedIdentifierHighlighter
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import cloak.platform.ExtendedPlatform
import cloak.platform.PlatformInputValidator
import cloak.platform.saved.*
import com.intellij.openapi.actionSystem.AnActionEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Repository

class SubmitAction : CloakAction() {
    override fun isEnabled(event: AnActionEvent): Boolean {
        return IdeaPlatform(event.project ?: return false, event.editor).renamedNames.isNotEmpty()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val platform = IdeaPlatform(event.project ?: return, event.editor)
        val repo = YarnRepo.at(platform.yarnRepoDir)

        GlobalScope.launch {
            val gitUser = platform.getGitUser() ?: return@launch

            val prName = platform.getUserInput(
                title = "Pull Request",
                message = "Enter name for mappings set",
                validator = PlatformInputValidator(allowEmptyString = false)
            ) ?: return@launch

            val upstreamOwner = "natanfudge"//TODO: switch to YarnRepo.upstreamuser
            val pr = platform.createPr(repo, prName, gitUser, upstreamOwner)

            platform.showMessageDialog(
                title = "Success",
                message = "<html>Your mappings have been submitted! Track them <a href=\"${pr.htmlUrl}\">here</a>.</html>"
            )

            platform.resetWorkspace(repo, event, gitUser)
        }

    }

    private suspend fun ExtendedPlatform.resetWorkspace(repo: YarnRepo, event: AnActionEvent, gitUser: GitUser) {
        asyncWithText("Cleaning...") {
            repo.switchToBranch("master")
            repo.deleteBranch(gitUser.branchName)
            renamedNames.clear()
            // This will update the mc version because the 'McVersion' file will be deleted
            updateIntermediaryNamesToVersion(repo.getTargetMinecraftVersion())
        }

        RenamedIdentifierHighlighter.rerun(event)
    }

    private suspend fun ExtendedPlatform.createPr(
        repo: YarnRepo,
        prName: String,
        gitUser: GitUser,
        upstreamOwner: String
    ): PullRequestResponse = asyncWithText("Submitting...") {
        val newBranchName = Repository.normalizeBranchName(prName)
        repo.switchToBranch(newBranchName, startFromBranch = gitUser.branchName)
        migrateYarnChangeList(oldBranch = gitUser.branchName, newBranch = newBranchName)

        repo.push()
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

}