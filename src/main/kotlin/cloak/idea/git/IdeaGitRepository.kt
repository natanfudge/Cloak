package cloak.idea.git

import cloak.git.CloakRepository
import cloak.git.JGit
import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.commands.GitLineHandlerListener
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryCreator
import org.apache.log4j.LogManager
import java.io.File

typealias IdeaGit = git4idea.commands.Git

private val LOGGER = LogManager.getLogger("IdeaGit")

class IdeaGitRepository(project: Project, override val git: JGit, override val path: File) :
    CloakRepository() {
    private val ideaRepo: GitRepository = GitRepositoryCreator(project).createRepositoryIfValid(
        LocalFileSystem.getInstance().findFileByIoFile(path)!!, VcsRepositoryManager.getInstance(project)
    ) as GitRepository

    override fun push(remoteUrl: String, branch: String, refSpec: String) {
        val result = IdeaGit.getInstance().push(ideaRepo, "origin", remoteUrl, refSpec, false,
            GitLineHandlerListener { line, outputType -> })
        if (!result.success()) LOGGER.warn("Could not push $branch to $remoteUrl: $result")
    }

    override fun deleteBranch(remoteUrl: String, branchName: String) {
        val result = IdeaGit.getInstance()
            .branchDelete(ideaRepo, branchName, true, GitLineHandlerListener { line, outputType -> })
        if (!result.success()) LOGGER.warn("Could not delete $branchName: $result")
        push(remoteUrl, branchName, refSpec = "+:refs/heads/$branchName")
    }

}