package cloak.git

import cloak.format.mappings.MappingsExtension
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.platform.saved.GitUser
import cloak.platform.saved.getGitUser
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.internal.nullable
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import java.io.File
import java.nio.file.Paths

private const val YarnRepositoryDirectory = "yarn"

private val ExtendedPlatform.yarnRepoDir get() = storageDirectory.resolve(YarnRepositoryDirectory).toFile()

/**
 * Don't access this too early
 */
val ExtendedPlatform.yarnRepo: YarnRepo
    get() {
        return YarnRepo.at(yarnRepoDir, this)
    }

private var ExtendedPlatform.currentBranchStore: String? by SavedState(null, "CurrentBranch", StringSerializer.nullable)

val ExtendedPlatform.currentBranch
    get() = currentBranchStore ?: error("The branch wasn't switched to something usable yet")

val ExtendedPlatform.currentBranchOrNull get() = currentBranchStore

suspend fun ExtendedPlatform.inSubmittedBranch(): Boolean = currentBranch != getGitUser()?.branchName

fun ExtendedPlatform.setCurrentBranchToDefaultIfNeeded(gitUser: GitUser) {
    if (currentBranchStore == null) currentBranchStore = gitUser.branchName
}


class YarnRepo private constructor(private val localPath: File, val platform: ExtendedPlatform) {

    private var _git: CloakRepository? = null

    private suspend fun getGit(): CloakRepository? {
        if (_git == null) _git = getOrCloneGit()
        return _git
    }


    companion object {
        // Might want to cache YarnRepo instances at some point
        fun at(location: File, platform: ExtendedPlatform) = YarnRepo(location, platform)

        //        const val GithubUsername = "Cloak-Bot"
        const val UpstreamUsername = "fabricmc"
        //        const val UpstreamUsername = "natanfudge"
        private const val RepoName = "yarn"
        //        private const val OriginUrl = "https://github.com/$GithubUsername/$RepoName"
        private const val UpstreamUrl = "https://github.com/$UpstreamUsername/$RepoName"
        private const val MappingsDirName = "mappings"
        private const val McVersionFile = "mcversion.txt"

    }

    val mappingsDirectory: File = getFile(MappingsDirName)

    suspend fun warmup() {
        getGit()
    }

    private suspend fun originUrl(): String? =
        platform.getAuthenticatedUsername()?.let { "https://github.com/$it/$RepoName" }


    fun clean() = localPath.deleteRecursively()

    //TODO: remove this, this is to move over to the user's own repository instead of cloak bot's repository.
    suspend fun fixOriginUrl() {
        originUrl()?.let { getGit()?.resetOrigin(it) }
    }


    /**
     * Returns all the paths in the mappings folder relative to the mappings folder, NOT including extensions.
     */
    fun getMappingsFilesLocations(): Set<String> {
        return mappingsDirectory.walk().filter { !it.isDirectory }
            .map { it.relativeTo(mappingsDirectory).path.removeSuffix(MappingsExtension).replace("\\", "/") }
            .toHashSet()
    }

    fun getMappingsFile(path: String): File = mappingsDirectory.toPath().resolve(path).toFile()

    suspend fun push() =
        originUrl()?.let { originUrl -> getGit()?.currentBranch()?.let { getGit()?.push(originUrl, it) } }

    suspend fun deleteBranch(branchName: String) = originUrl()
        ?.let { getGit()?.deleteBranch(remoteUrl = it, branchName = branchName) }

//    fun close() = _git?.close()

    suspend fun switchToBranch(
        branchName: String,
        startFromBranch: String? = null,
        force: Boolean = false
    ) = getGit()?.switchToBranch(
        branchName = branchName,
        defaultBaseBranch = { "upstream/${getTargetMinecraftVersion()}" },
        startFromBranch = startFromBranch,
        force = force
    ).also { mcVersion = null }.also {
        platform.currentBranchStore = branchName
        platform.getGitUser()?.branchName
    }

    suspend fun removeMappingsFile(path: String) {
        getGit()?.remove(pathOfMappingFromGitRoot(path))
    }

    suspend fun stageMappingsFile(path: String) {
        getGit()?.stageChanges(pathOfMappingFromGitRoot(path))
    }

    private var mcVersion: String? = null

    fun getTargetMinecraftVersion(): String {
        if (mcVersion == null) {
            val file = getFile(McVersionFile)
            if (!file.exists()) {
                getFile(McVersionFile).writeText(GithubApi.getDefaultBranch(RepoName, UpstreamUsername))
            }
            mcVersion = file.readText()
        }

        return mcVersion!!
    }

    suspend fun commitChanges(author: GitUser, commitMessage: String) {
        getGit()?.commit(author.jgit, commitMessage)
    }


    private fun pathOfMappingFromGitRoot(relativeMappingPath: String): String {
        return Paths.get(MappingsDirName, relativeMappingPath).toString().replace("\\", "/")
    }

    private suspend fun getOrCloneGit(): CloakRepository? {
        return if (localPath.exists()) platform.createGit(Git.open(localPath), localPath)
        else {
            val origin = originUrl() ?: return null
            val userName = platform.getAuthenticatedUsername() ?: return null
            platform.forkRepository(repositoryName = RepoName, forkedUser = UpstreamUsername, forkingUser = userName)
            println("Cloning yarn repo to $localPath")
            val jgit = Git.cloneRepository()
                .setURI(origin)
                .setDirectory(localPath)
                .call()

            platform.createGit(jgit, localPath).also {
                jgit.remoteAdd().setName("upstream").setUri(URIish(UpstreamUrl)).call()
                it.updateRemote("upstream")
            }
        }
    }

    private fun getFile(path: String): File = localPath.toPath().resolve(path).toFile()
}
