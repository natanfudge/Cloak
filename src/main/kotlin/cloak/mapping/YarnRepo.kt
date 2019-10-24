package cloak.mapping

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.nio.file.Paths

class YarnRepo(private val localPath: File) {
    private var git : GitRepository? = null
    companion object {
        private const val RemoteUrl = "https://github.com/natanfudge/yarn"
        private const val MappingsDirName = "mappings"
        private const val GithubUsername = "natanfudge"
        //TODO: use .rc with encryption or smthn
        private val GithubPassword = System.getenv("GITHUB_PASSWORD")
    }

    val mappingsDirectory: File = getFile(MappingsDirName)


    fun clean() = localPath.deleteRecursively()

    fun getOrCloneGit(): GitRepository {
        if(git == null) git = GitRepository(
            if (localPath.exists()) Git.open(localPath) else Git.cloneRepository()
                .setURI(RemoteUrl)
                .setDirectory(localPath)
                .call()
        )
        return git!!
    }


    fun getFile(path: String): File = localPath.toPath().resolve(path).toFile()

    fun walkMappingsDirectory(): FileTreeWalk = mappingsDirectory.walk()


    fun pathOfMappingFromGitRoot(relativeMappingPath: String): String {
        return Paths.get(MappingsDirName, relativeMappingPath).toString().replace("\\", "/")
    }

    fun getMappingsFile(path: String): File = mappingsDirectory.toPath().resolve(path).toFile()

    fun push(repo: GitRepository) =
        repo.actuallyPush(RemoteUrl, UsernamePasswordCredentialsProvider(GithubUsername, GithubPassword))
}
