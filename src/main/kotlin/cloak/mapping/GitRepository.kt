package cloak.mapping

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish

open class GitRepository(private val git: Git) {

    fun switchToBranch(branchName: String) {
        if (git.repository.branch == branchName) return

        val remoteBranchExists = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
            .any { it.name == "refs/remotes/origin/$branchName" }

        val localBranchAlreadyExists = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
            .any { it.name == "refs/heads/$branchName" }

        val startPoint = when {
            localBranchAlreadyExists -> branchName
            remoteBranchExists -> "origin/$branchName"
            else -> "refs/heads/master"
        }

        git.checkout()
            .setCreateBranch(!localBranchAlreadyExists)
            .setName(branchName).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .setStartPoint(startPoint).call()

    }


    fun remove(path: String): DirCache {
        return git.rm().addFilepattern(path).call()
    }

    fun stageChanges(path: String): DirCache {
        return git.add().addFilepattern(path).call()
    }

    fun getBranches(): List<Ref> = git.branchList().call()

    open fun commit(author: PersonIdent, commitMessage: String) {
        git.commit().setAuthor(author).setCommitter(author).setMessage(commitMessage).call()
    }

    open fun actuallyPush(remoteUrl: String, credentialsProvider: CredentialsProvider) {
        git.remoteAdd().setName("origin").setUri(URIish(remoteUrl)).call()
        git.push().setCredentialsProvider(credentialsProvider).call()
    }
}

