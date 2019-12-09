package cloak.idea.git

import cloak.git.CloakRepository
import cloak.git.JGit
import com.intellij.dvcs.repo.Repository
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.commands.GitLineHandlerListener
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryCreator
import java.io.File

typealias IdeaGit = git4idea.commands.Git

class IdeaGitRepository(private val project: Project, override val git: JGit, override val path: File) :
    CloakRepository() {
    private val ideaRepo: GitRepository = GitRepositoryCreator(project).createRepositoryIfValid(
            LocalFileSystem.getInstance().findFileByIoFile(path)!!
        ) { git.close() } as GitRepository

    override fun push(remoteUrl: String, branch : String) {
        val refspec = "+refs/heads/$branch:refs/heads/$branch"
        IdeaGit.getInstance().push(ideaRepo,"origin",null,refspec,false,
            GitLineHandlerListener { line, outputType -> })
    }

    override fun deleteBranch(branchName: String) {
        IdeaGit.getInstance().branchDelete(ideaRepo,branchName,true, GitLineHandlerListener { line, outputType ->  })
    }

}