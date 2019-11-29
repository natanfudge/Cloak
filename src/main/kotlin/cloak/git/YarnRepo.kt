package cloak.git

import cloak.format.mappings.MappingsExtension
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.platform.saved.GitUser
import cloak.platform.saved.getGitUser
import kotlinx.serialization.internal.BooleanSerializer
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.internal.nullable
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.nio.file.Paths

private const val YarnRepositoryDirectory = "yarn"

private val ExtendedPlatform.yarnRepoDir get() = storageDirectory.resolve(YarnRepositoryDirectory).toFile()
val ExtendedPlatform.yarnRepo get() = YarnRepo.at(yarnRepoDir, this)

private var ExtendedPlatform.currentBranchStore: String? by SavedState(null,"", StringSerializer.nullable)

val ExtendedPlatform.currentBranch
    get() = currentBranchStore ?: error("The branch wasn't switched to something usable yet")

val ExtendedPlatform.currentBranchOrNull get() = currentBranchStore

var ExtendedPlatform.inSubmittedBranch: Boolean by SavedState(false, "InSubmittedBranch" ,BooleanSerializer)
    private set

fun ExtendedPlatform.setCurrentBranchToDefaultIfNeeded(gitUser: GitUser) {
    if (currentBranchStore == null) currentBranchStore = gitUser.branchName
}


class YarnRepo private constructor(private val localPath: File, val platform: ExtendedPlatform) {

    private var _git: GitRepository? = null
    private val git: GitRepository
        get() {
            if (_git == null) _git = getOrCloneGit()
            return _git!!
        }

    companion object {
        // Might want to cache YarnRepo instances at some point
        fun at(location: File, platform: ExtendedPlatform) = YarnRepo(location, platform)

        const val GithubUsername = "Cloak-Bot"
        //        const val CloakEmail = "natandestroyer101@gmail.com"
        //TODO: this should be used when sending PRs
        const val UpstreamUsername = "fabricmc"
        private const val RepoName = "yarn"
        private const val OriginUrl = "https://github.com/$GithubUsername/$RepoName"
        private const val UpstreamUrl = "https://github.com/$UpstreamUsername/$RepoName"
        private const val MappingsDirName = "mappings"
        private const val McVersionFile = "mcversion.txt"

        //TODO: use .rc with encryption or smthn
        private val GithubPassword = System.getenv("GITHUB_PASSWORD")
    }

    val mappingsDirectory: File = getFile(MappingsDirName)


    fun clean() = localPath.deleteRecursively()


    /**
     * Returns all the paths in the mappings folder relative to the mappings folder, NOT including extensions.
     */
    fun getMappingsFilesLocations(): Set<String> {
        return mappingsDirectory.walk().filter { !it.isDirectory }
            .map { it.relativeTo(mappingsDirectory).path.removeSuffix(MappingsExtension).replace("\\", "/") }
            .toHashSet()
    }

    fun getMappingsFile(path: String): File = mappingsDirectory.toPath().resolve(path).toFile()

    fun push() = git.push(OriginUrl, credentials)

    fun deleteBranch(branchName: String) = git.deleteBranch(branchName, credentials)

    fun close() = _git?.close()

    suspend fun switchToBranch(
        branchName: String,
        startFromBranch: String? = null,
        force: Boolean = false
    ): Unit = git.switchToBranch(
        branchName = branchName,
        defaultBaseBranch = { "upstream/${getTargetMinecraftVersion()}" },
        startFromBranch = startFromBranch,
        force = force
    ).also { mcVersion = null }.also {
        platform.currentBranchStore = branchName
        val userBranch = platform.getGitUser()?.branchName
        platform.inSubmittedBranch = userBranch != branchName
    }

    fun removeMappingsFile(path: String) {
        git.remove(pathOfMappingFromGitRoot(path))
    }

    fun stageMappingsFile(path: String) {
        git.stageChanges(pathOfMappingFromGitRoot(path))
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

    fun commitChanges(author: GitUser, commitMessage: String) {
        git.commit(author.jgit, commitMessage)
    }


    private fun pathOfMappingFromGitRoot(relativeMappingPath: String): String {
        return Paths.get(MappingsDirName, relativeMappingPath).toString().replace("\\", "/")
    }

    private val credentials = UsernamePasswordCredentialsProvider(
        GithubUsername,
        GithubPassword
    )

    private fun getOrCloneGit(): GitRepository {
        return if (localPath.exists()) GitRepository(Git.open(localPath))
        else {
            println("Cloning yarn repo to $localPath")
            val jgit = Git.cloneRepository()
                .setURI(OriginUrl)
                .setDirectory(localPath)
                .call()

            GitRepository(jgit).also {
                jgit.remoteAdd().setName("upstream").setUri(URIish(UpstreamUrl)).call()
                it.updateRemote("upstream")
            }
        }
    }

    private fun getFile(path: String): File = localPath.toPath().resolve(path).toFile()
}
