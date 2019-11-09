package cloak.idea.actions

import RenamedNamesProvider
import cloak.git.*
import cloak.idea.RenamedIdentifierHighlighter
import cloak.idea.util.CloakAction
import cloak.idea.util.IdeaProjectWrapper
import cloak.idea.util.ProjectWrapper
import cloak.idea.util.editor
import cloak.mapping.rename.GitUser
import com.intellij.openapi.actionSystem.AnActionEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Repository
import java.util.*

class SubmitAction : CloakAction() {
    override fun isEnabled(event: AnActionEvent): Boolean {
        return RenamedNamesProvider.getInstance().anythingWasRenamed()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = IdeaProjectWrapper(event.project ?: return, event.editor)
        val repo = YarnRepo.at(project.yarnRepoDir)

        val gitUser = project.getGitUser() ?: return

        val prName = project.getUserInput(
            title = "Pull Request",
            message = "Enter name for mappings set",
            allowEmptyString = false
        ) ?: return

        GlobalScope.launch {
            val upstreamOwner = "natanfudge"//TODO: switch to fabricmc
            val pr = createPr(project, repo, prName, gitUser, upstreamOwner)
            project.inUiThread {
                project.showMessageDialog(
                    title = "Success",
                    message = "<html>Your mappings have been submitted! Track them <a href=\"${pr.htmlUrl}\">here</a>.</html>"
                )
            }

            project.resetWorkspace(repo, event, gitUser)
        }

    }

    private suspend fun ProjectWrapper.resetWorkspace(repo: YarnRepo, event: AnActionEvent, gitUser: GitUser) {
        asyncWithText("Cleaning...") {
            // TODO: try to see if we can start from the main one in the upstream repo, here and on first clone
            repo.getOrCloneGit().switchToBranch("master")
            repo.deleteBranch(gitUser.branchName)
            RenamedNamesProvider.getInstance().cleanNames()
        }

        RenamedIdentifierHighlighter.rerun(event)
    }

    private suspend fun createPr(
        project: IdeaProjectWrapper,
        repo: YarnRepo,
        prName: String,
        gitUser: GitUser,
        upstreamOwner: String
    ): PullRequestResponse = project.asyncWithText("Submitting...") {
        val git = repo.getOrCloneGit()
        val branchName = Repository.normalizeBranchName(prName)
        git.switchToBranch(branchName, startFromBranch = gitUser.branchName)


        repo.push()
        GithubApi.createPullRequest(
            repositoryName = "yarn",
            targetUser = upstreamOwner,
            targetBranch = GithubApi.getDefaultBranch("yarn", upstreamOwner),
            requestingBranch = branchName,
            requestingUser = YarnRepo.GithubUsername,
            title = prName,
            body = "PR"//TODO: add list of changes
        )
    }

}