package cloak.git

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.FetchResult
import org.eclipse.jgit.transport.URIish
import java.io.File

typealias JGit = org.eclipse.jgit.api.Git

abstract class CloakRepository(protected val git: JGit, protected val path: File) {

    // Cache the current branch because getting it is pretty slow
    var currentBranch: String = git.repository.branch
        private set

    fun switchToBranch(
        branchName: String,
        startFromBranch: String? = null,
        force: Boolean = false,
        defaultBaseBranch: () -> String = { "refs/heads/master" }
    ) {
        if (git.repository.branch == branchName) return
        currentBranch = branchName

        val remoteBranchExists = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
            .any { it.name == "refs/remotes/origin/$branchName" }

        val localBranchAlreadyExists = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
            .any { it.name == "refs/heads/$branchName" }

        val startPoint = if (startFromBranch != null) "refs/heads/$startFromBranch" else when {
            localBranchAlreadyExists -> branchName
            remoteBranchExists -> "origin/$branchName"
            else -> defaultBaseBranch()
        }

        updateRemote("upstream", YarnRepo.UpstreamUrl)

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


    abstract fun deleteBranch(remoteUrl: String, branchName: String)

    fun getBranches(): List<Ref> = git.branchList().call()

    abstract fun commit(commitMessage: String)

    fun resetOrigin(newRemoteUrl: String) {
        git.remoteSetUrl().setRemoteName("origin").setRemoteUri(URIish(newRemoteUrl)).call()
    }


    abstract fun push(remoteUrl: String, branch: String, refSpec: String = "+refs/heads/$branch:refs/heads/$branch")

    fun updateRemote(remote: String, remoteUrl: String, calledBefore: Boolean = false): FetchResult = try {
        git.fetch().setRemote(remote).call()
    } catch (e: InvalidRemoteException) {
        println("Remote $remote was not found, recreating it")
        git.remoteAdd().setName(remote).setUri(URIish(remoteUrl)).call()
        if (!calledBefore) updateRemote(remote, remoteUrl, calledBefore = true)
        else error("Remote was somehow invalid even after adding it!")
    }


    fun close() = git.close()
}

