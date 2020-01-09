package cloak.git

import cloak.format.mappings.MappingsExtension
import cloak.platform.ExtendedPlatform
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import java.io.File
import java.nio.file.Paths

private const val YarnRepositoryDirectory = "yarn"

private val ExtendedPlatform.yarnRepoDir get() = getStorageDirectory().resolve(YarnRepositoryDirectory).toFile()

/**
 * Don't access this too early
 */
val ExtendedPlatform.yarnRepo: YarnRepo
    get() {
        return YarnRepo.at(yarnRepoDir, this)
    }


suspend fun ExtendedPlatform.inSubmittedBranch(): Boolean =
    yarnRepo.getCurrentBranch() != getAuthenticatedUser().branchName


class YarnRepo private constructor(private val localPath: File, val platform: ExtendedPlatform) {

    private var _git: CloakRepository? = null

    private fun getOrOpenGit(): CloakRepository? {
        if (!isCloned()) return null
        if (_git == null) _git = openGit()
        return _git!!
    }

    private suspend fun getOrCreateGit(onClone: () -> Unit = {}, onFork: () -> Unit = {}): CloakRepository {
        if (_git == null) _git = getOrCloneGit(onClone, onFork)
        return _git!!
    }

    val defaultBranch: String by lazy {
        GithubApi.getDefaultBranch(RepoName, UpstreamUsername)
    }

    companion object {
        private var repo: YarnRepo? = null

        fun at(location: File, platform: ExtendedPlatform): YarnRepo {
            if (repo == null) repo = YarnRepo(location, platform)
            return repo!!
        }

        const val UpstreamUsername = "fabricmc"
        private const val RepoName = "yarn"
        const val UpstreamUrl = "https://github.com/$UpstreamUsername/$RepoName"
        private const val MappingsDirName = "mappings"

    }

    suspend fun getCurrentBranch(): String = getOrCreateGit().currentBranch
    fun getCurrentBranchOrNull(): String? = getOrOpenGit()?.currentBranch

    private val mappingsDirectory: File = getFile(MappingsDirName)

    /**
     * @param onClone Called when the repo is cloned from scratch
     */
    suspend fun warmup(onClone: () -> Unit = {}, onFork: () -> Unit = {}) {
        getOrCreateGit(onClone, onFork)
    }

    private suspend fun originUrl(): String = "https://github.com/${platform.getAuthenticatedUser().name}/$RepoName"


    fun clean() = localPath.deleteRecursively()


    /**
     * Returns all the paths in the mappings folder relative to the mappings folder, NOT including extensions.
     */
    fun getMappingsFilesLocations(): Set<String> {
        return mappingsDirectory.walk().filter { !it.isDirectory && "." + it.extension == MappingsExtension }
            .map { it.relativeTo(mappingsDirectory).path.removeSuffix(MappingsExtension).replace("\\", "/") }
            .toHashSet()
    }

    fun getMappingsFile(path: String): File = mappingsDirectory.toPath().resolve(path).toFile()

    suspend fun push() {
        val git = getOrCreateGit()
        git.push(originUrl(), git.currentBranch)
    }

    suspend fun deleteBranch(branchName: String) {
        getOrCreateGit().deleteBranch(remoteUrl = originUrl(), branchName = branchName)
    }


    suspend fun switchToBranch(
        branchName: String,
        startFromBranch: String? = null,
        force: Boolean = false,
        git: CloakRepository? = null
    ) {
        val usedGit = git ?: getOrCreateGit()

        platform.branch.createBranch(
            branchName = branchName,
            minecraftVersion = if (startFromBranch != null) platform.branch.getMinecraftVersion() ?: return
            else defaultBranch
        )

        usedGit.switchToBranch(
            branchName = branchName,
            defaultBaseBranch = { "upstream/$defaultBranch" },
            startFromBranch = startFromBranch,
            force = force
        )
    }

    suspend fun removeMappingsFile(path: String) {
        getOrCreateGit().remove(pathOfMappingFromGitRoot(path))
    }

    suspend fun stageMappingsFile(path: String) {
        getOrCreateGit().stageChanges(pathOfMappingFromGitRoot(path))
    }

    suspend fun commitChanges(commitMessage: String) {
        getOrCreateGit().commit(commitMessage)
    }

    private fun pathOfMappingFromGitRoot(relativeMappingPath: String): String {
        return Paths.get(MappingsDirName, relativeMappingPath).toString().replace("\\", "/")
    }

    fun isCloned(): Boolean = localPath.exists()

    private fun openGit(): CloakRepository = platform.createGit(Git.open(localPath), localPath)

    private suspend fun getOrCloneGit(onClone: () -> Unit = {}, onFork: () -> Unit = {}): CloakRepository {
        return if (isCloned()) openGit()
        else {
            val origin = originUrl()
            val user = platform.getAuthenticatedUser()
            onFork()
            platform.forkRepository(repositoryName = RepoName, forkedUser = UpstreamUsername, forkingUser = user.name)
            println("Cloning yarn repo to $localPath")
            onClone()
            val jgit = Git.cloneRepository()
                .setURI(origin)
                .setDirectory(localPath)
                .call()

            platform.createGit(jgit, localPath).also {
                jgit.remoteAdd().setName("upstream").setUri(URIish(UpstreamUrl)).call()
                it.updateRemote("upstream", UpstreamUrl)

                switchToBranch(platform.getAuthenticatedUser().branchName, git = it)
            }

        }
    }

    private fun getFile(path: String): File = localPath.toPath().resolve(path).toFile()
}
