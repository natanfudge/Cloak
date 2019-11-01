package cloak.mapping

import cloak.mapping.mappings.MappingsExtension
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.nio.file.Paths
// To not lose the caches, we reuse the same instance
private var yarnRepo : YarnRepo? = null
class YarnRepo private constructor(private val localPath: File) {

    private var git: GitRepository? = null

    companion object {
        /** Note: the location only matters in the first call */
        fun at(location : File) : YarnRepo{
            if(yarnRepo == null) yarnRepo = YarnRepo(location)
            return yarnRepo!!
        }
        private const val RemoteUrl = "https://github.com/natanfudge/yarn"
        private const val MappingsDirName = "mappings"
        private const val GithubUsername = "natanfudge"
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

    //TODO: update the cache after every change
    /**
     * Returns all the paths in the mappings folder relative to the mappings folder, NOT including extensions.
     */
    fun getMappingsFilesLocations(): Set<String> {
        if (mappingsDirectoryFilesCache == null) {
            mappingsDirectoryFilesCache = mappingsDirectory.walk().filter { !it.isDirectory }
                .map { it.relativeTo(mappingsDirectory).path.removeSuffix(MappingsExtension).replace("\\", "/") }
                .toHashSet()
        }
        return mappingsDirectoryFilesCache!!
    }

    // Avoid reading the entire repository every time
    private var mappingsDirectoryFilesCache: HashSet<String>? = null

    /**
     * Must be called after changing mappings files. This accepts RELATIVE locations.
     */
    fun updateMappingsFileLocation(oldLocation: String, newLocation: String) {
        mappingsDirectoryFilesCache?.remove(oldLocation) ?: error("The cache shouldn't be null here")
        mappingsDirectoryFilesCache!!.add(newLocation)
    }

//    fun walkMappingsDirectory(): FileTreeWalk =


    fun pathOfMappingFromGitRoot(relativeMappingPath: String): String {
        return Paths.get(MappingsDirName, relativeMappingPath).toString().replace("\\", "/")
    }

    fun getMappingsFile(path: String): File = mappingsDirectory.toPath().resolve(path).toFile()

    fun push(repo: GitRepository) =
        repo.actuallyPush(RemoteUrl, UsernamePasswordCredentialsProvider(GithubUsername, GithubPassword))
}
