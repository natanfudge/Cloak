package cloak.mapping.rename

import cloak.idea.util.CommonIcons
import cloak.idea.util.ProjectWrapper
import cloak.mapping.*
import cloak.mapping.descriptor.remap
import cloak.mapping.mappings.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.lib.PersonIdent
import java.io.File
import javax.lang.model.SourceVersion
import kotlin.test.assertNotNull

data class GitUser(val name: String, val email: String) {
    val jgit = PersonIdent(name, email)
    val branchName = name
}

object Renamer {
    fun <M : Mapping> rename(
        project: ProjectWrapper,
        name: Name<M>,
        isTopLevelClass: Boolean
    ): Errorable<String> = with(project) {
        //TODO: actually get from input and save to global settings
        val user =
            GitUser(name = "natanfudge", email = "natan.lifsiz@gmail.com")
        val git = getOrCloneGitAsync(gitUser = user, yarnRepo = yarnRepoDir)
        val namedToIntermediaryClasses = getClassNamesMapAsync(YarnRepo(yarnRepoDir))


        val input = showInputDialog(message = "Choose new name", title = "Rename") {
            validate(it, isTopLevelClass)
        } ?: return fail("User didn't input a new name")

        return runBlocking {

            // Class names are named for the user, but intermediary in the mappings,
            // so we convert what the user has to intermediary.
            val namedToIntermediary = namedToIntermediaryClasses.await()
            val oldName = if (name is MethodName) name.copy(
                parameterTypes = name.parameterTypes.map { it.remap(namedToIntermediary) }
            ) else name

            val result = tryRename(
                oldName = oldName,
                newName = input,
                yarnRepo = yarnRepoDir,
                explanation = null,
                gitUser = user,
                git = git.await(),
                namedToIntermediary = namedToIntermediary
            )

            when (result) {
                //TODO: this should not exit out of the UI if there is a problem, instead there should be loading indicator
                // and an error on the same screen (if no error then exit and send message)
                is StringSuccess -> showMessageDialog(message = result.value, title = "Rename Success")
                is StringError -> showMessageDialog(
                    message = result.value,
                    title = "Rename Error",
                    icon = CommonIcons.Error
                )
            }
            result
        }

    }

    /**
     * Call this while the user is busy (typing the new name) to prevent lag later on.
     * This method will be executed asynchronously so it will return immediately and do the work in the background.
     */
    private fun getOrCloneGitAsync(gitUser: GitUser, yarnRepo: File) = GlobalScope.async {
        YarnRepo(yarnRepo).getOrCloneGit().apply { switchToBranch(gitUser.branchName) }
    }

    private fun ProjectWrapper.getClassNamesMapAsync(yarnRepo: YarnRepo) = GlobalScope.async {
        val map = getIntermediaryClassNames()
        if (map.isEmpty()) {
            yarnRepo.walkMappingsDirectory().forEach { file ->
                if(!file.isDirectory){
                    MappingsFile.read(file).visitClasses { mapping ->
                        mapping.deobfuscatedName?.let { map[it] = mapping.obfuscatedName }
                    }
                }

            }
        }
        map
    }


    /**
     * Returns a string if [newName] invalid, null if valid.
     * @param isTopLevelClass whether the element to rename is a top level class
     */
    private fun validate(newName: String, isTopLevelClass: Boolean): String? {
        val (packageName, className) = splitPackageAndName(newName)

        if (!isTopLevelClass && packageName != null) return "Package rename can only be done on top-level classes"

        for (part in packageName?.split("/") ?: listOf()) {
            if (!SourceVersion.isIdentifier(part)) return "'$part' is not a valid package name"
        }

        if (!SourceVersion.isName(className)) return "'$className' is not a valid class name"

        return null
    }

    /**
     * @param oldName The class, method, field, or parameter to rename.
     * @param newName The new name of the class, method, field, or parameter.
     * In the case of a class, it can be a fully qualified path in which case the package will be renamed too.
     * @param explanation Why the user renamed this
     * @param yarnRepo A place to store the yarn git repository.
     * @param gitUser Git identifier of the user who made the mappings
     *
     *
     * @return A success message if successful, and a failure message if it failed
     */
    private fun <M : Mapping> tryRename(
        oldName: Name<M>,
        newName: String,
        explanation: String?,
        yarnRepo: File,
        git: GitRepository,
        gitUser: GitUser,
        namedToIntermediary: MutableMap<String, String>
    )
            : Errorable<String> {
        val (packageName, className) = splitPackageAndName(newName)
        assert(packageName == null || (oldName is ClassName && oldName.innerClass == null))

        val yarn = YarnRepo(yarnRepo)

        val rename = Rename(
            originalName = oldName,
            newName = className,
            explanation = explanation,
            newPackageName = packageName
        )

        val matchingMappings = yarn.walkMappingsDirectory()
            .mapNotNull { rename.findRenameTarget(it) }
            .toList()

        //TODO: Remember to have this error happen ONLY when the mappings actually mismatch
        if (matchingMappings.isEmpty()) return fail(
            """ 
Could not find a mapping for '$oldName' (Probably mismatching mappings)
Ensure:
    - The project SDK is set correctly
- Remember to have this error happen ONLY when the mappings actually mismatch)"""
        )
        assert(matchingMappings.size == 1)


        return rename(rename, matchingMappings[0], yarn, git, gitUser,namedToIntermediary)
    }

//    fun Name<*>.type(): String = when (this) {
//        is ClassName -> "class"
//        is MethodName -> "method"
//        is FieldName -> "field"
//        is ParameterName -> "parameter"
//    }


    private fun <M : Mapping> rename(
        rename: Rename<M>,
        renameTarget: M,
        yarn: YarnRepo,
        git: GitRepository,
        user: GitUser,
        namedToIntermediary: MutableMap<String, String>
    ): Errorable<String> {
        val oldPath = renameTarget.filePath
        val presentableOldName = renameTarget.toString()
        val result = rename.rename(renameTarget)
        if (result is StringError) {
            return result.map { "" }
        }
        // Update the named -> intermediary map
        if (renameTarget is ClassMapping) {
            namedToIntermediary[renameTarget.deobfuscatedName ?: error("A name has been given")] = renameTarget.obfuscatedName
        }

        val newPath = renameTarget.filePath
        val presentableNewName = renameTarget.toString()


        if (oldPath != newPath) {
            git.remove(yarn.pathOfMappingFromGitRoot(oldPath))
        }

        renameTarget.root.writeTo(yarn.getMappingsFile(newPath))
        git.stageChanges(yarn.pathOfMappingFromGitRoot(newPath))
        git.commit(author = user.jgit, commitMessage = "$presentableOldName -> $presentableNewName")

        println("Changes commited successfully!")

        return "Renamed $presentableOldName to $presentableNewName".success
    }

//TODO: push changes
// repo.actuallyPush(YarnRepo.RemoteUrl, UsernamePasswordCredentialsProvider(YarnRepo.GithubUsername, YarnRepo.GithubPassword))

    val Mapping.filePath get() = (root.deobfuscatedName ?: root.obfuscatedName) + ".mapping"


}


fun splitPackageAndName(rawName: String): Pair<String?, String> {
    val lastSlashIndex = rawName.lastIndexOf('/')
    return if (lastSlashIndex == -1) null to rawName
    else rawName.splitOn(lastSlashIndex)
}
