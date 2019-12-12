package cloak.git

import cloak.platform.GitUser
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.FetchResult
import org.eclipse.jgit.transport.URIish
import java.io.File

typealias JGit = org.eclipse.jgit.api.Git

abstract class CloakRepository {
    protected abstract val git: JGit
    protected abstract val path: File

    fun currentBranch(): String = git.repository.branch

    fun switchToBranch(
        branchName: String,
        startFromBranch: String? = null,
        force: Boolean = false,
        defaultBaseBranch: () -> String = { "refs/heads/master" }
    ) {
        if (git.repository.branch == branchName) return

        val remoteBranchExists = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
            .any { it.name == "refs/remotes/origin/$branchName" }

        val localBranchAlreadyExists = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
            .any { it.name == "refs/heads/$branchName" }

        val startPoint = if (startFromBranch != null) "refs/heads/$startFromBranch" else when {
            localBranchAlreadyExists -> branchName
            remoteBranchExists -> "origin/$branchName"
            else -> defaultBaseBranch()
        }

        updateRemote("upstream")

        git.checkout()
            .setCreateBranch(!localBranchAlreadyExists)
            .setName(branchName).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .setForced(force)
            .setStartPoint(startPoint).call()

    }


    fun remove(path: String): DirCache {
        return git.rm().addFilepattern(path).call()
    }

    fun stageChanges(path: String): DirCache {
        return git.add().addFilepattern(path).call()
    }

//    fun deleteBranch(branchName: String, credentialsProvider: CredentialsProvider) {
//        git.branchDelete().setBranchNames("refs/heads/$branchName").setForce(true).call()
//        val refspec = RefSpec().setSource(null).setDestination("refs/heads/$branchName")
//        git.push().setRefSpecs(refspec).setRemote("origin").setCredentialsProvider(credentialsProvider).call()
//    }

    abstract fun deleteBranch(remoteUrl: String, branchName: String)

    fun getBranches(): List<Ref> = git.branchList().call()

    abstract fun commit(commitMessage: String)
//    {
//        git.commit().setAuthor(author).setCommitter(author).setMessage(commitMessage).call()
//    }

    fun resetOrigin(newRemoteUrl: String) {
        git.remoteSetUrl().setRemoteName("origin").setRemoteUri(URIish(newRemoteUrl)).call()
    }

//    open fun push(remoteUrl: String, credentialsProvider: CredentialsProvider) {
//        git.remoteAdd().setName("origin").setUri(URIish(remoteUrl)).call()
//        git.push().setCredentialsProvider(credentialsProvider).call()
//    }


    abstract fun push(remoteUrl: String, branch: String,refSpec : String = "+refs/heads/$branch:refs/heads/$branch")

    fun updateRemote(remote: String): FetchResult = git.fetch().setRemote(remote).call()

    fun close() = git.close()
}

