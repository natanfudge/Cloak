package cloak.git

import cloak.mapping.mappings.MappingsExtension
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.nio.file.Paths

class YarnRepo private constructor(private val localPath: File) {

    private var git: GitRepository? = null

    companion object {
        // Might want to cache YarnRepo instances at some point
        fun at(location: File) = YarnRepo(location)

        const val GithubUsername = "Cloak-Bot"
        private const val RemoteUrl = "https://github.com/$GithubUsername/yarn"
        private const val MappingsDirName = "mappings"

        //TODO: use .rc with encryption or smthn
        private val GithubPassword = System.getenv("GITHUB_PASSWORD")
    }

    val mappingsDirectory: File = getFile(MappingsDirName)


    fun clean() = localPath.deleteRecursively()

    fun getOrCloneGit(): GitRepository {
        if (git == null) git = GitRepository(
            if (localPath.exists()) Git.open(localPath) else Git.cloneRepository()
                .setURI(RemoteUrl)
                .setDirectory(localPath)
                .call()
        )
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
        RemoteUrl, credentials
    )

    fun deleteBranch(branchName: String) = getOrCloneGit().actuallyDeleteBranch(branchName, credentials)

    private val credentials = UsernamePasswordCredentialsProvider(
        GithubUsername,
        GithubPassword
    )
}
