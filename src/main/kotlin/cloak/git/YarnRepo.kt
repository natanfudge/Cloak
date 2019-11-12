package cloak.git

import cloak.mapping.mappings.MappingsExtension
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.nio.file.Paths

class YarnRepo private constructor(private val localPath: File) {

    private var git: GitRepository? = null

    companion object {
        // Might want to cache YarnRepo instances at some point
        fun at(location: File) = YarnRepo(location)

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

    fun getOrCloneGit(): GitRepository {
        if (git == null) {
            if (localPath.exists()) git = GitRepository(Git.open(localPath))
            else {
                val jgit = Git.cloneRepository()
                    .setURI(OriginUrl)
                    .setDirectory(localPath)
                    .call()

                git = GitRepository(jgit)
                jgit.remoteAdd().setName("upstream").setUri(URIish(UpstreamUrl)).call()
                git!!.updateRemote("upstream")
            }
        }

        return git!!
    }


    fun getFile(path: String): File = localPath.toPath().resolve(path).toFile()

    /**
     * Returns all the paths in the mappings folder relative to the mappings folder, NOT including extensions.
     */
    fun getMappingsFilesLocations(): Set<String> {
        return mappingsDirectory.walk().filter { !it.isDirectory }
            .map { it.relativeTo(mappingsDirectory).path.removeSuffix(MappingsExtension).replace("\\", "/") }
            .toHashSet()
    }


    fun pathOfMappingFromGitRoot(relativeMappingPath: String): String {
        return Paths.get(MappingsDirName, relativeMappingPath).toString().replace("\\", "/")
    }

    fun getMappingsFile(path: String): File = mappingsDirectory.toPath().resolve(path).toFile()

    fun push() = getOrCloneGit().actuallyPush(
        OriginUrl, credentials
    )

    fun deleteBranch(branchName: String) = getOrCloneGit().actuallyDeleteBranch(branchName, credentials)

    fun close() = git?.close()


    //TODO: delete mcversionfile before publishing
    fun switchToBranch(branchName: String): Unit = getOrCloneGit().internalSwitchToBranch(branchName,
        defaultBaseBranch = { "upstream/${getTargetMinecraftVersion()}" }
    )

    fun getTargetMinecraftVersion(): String {
        val file = getFile(McVersionFile)
        if (!file.exists()) {
            getFile(McVersionFile).writeText(GithubApi.getDefaultBranch(RepoName, UpstreamUsername))
        }
        return file.readText()
    }

    private val credentials = UsernamePasswordCredentialsProvider(
        GithubUsername,
        GithubPassword
    )
}
