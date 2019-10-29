package cloak.mapping.rename

import cloak.idea.util.CommonIcons
import cloak.idea.util.ProjectWrapper
import cloak.mapping.*
import cloak.mapping.descriptor.remap
import cloak.mapping.mappings.*
import kotlinx.coroutines.*
import org.eclipse.jgit.lib.PersonIdent
import java.io.File
import javax.lang.model.SourceVersion

data class GitUser(val name: String, val email: String) {
    val jgit = PersonIdent(name, email)
    val branchName = name
}

object Renamer {
    /**
     * Returns the new name
     */
    suspend fun rename(
        project: ProjectWrapper,
        name: Name,
        isTopLevelClass: Boolean
    )  : Errorable<String> = with(project) {
        coroutineScope {
            val user = GitUser(name = "natanfudge", email = "natan.lifsiz@gmail.com")
            val git = async { getOrCloneGit(gitUser = user, yarnRepo = yarnRepoDir) }
            val namedToIntermediaryClasses = async { getClassNamesMap(YarnRepo(yarnRepoDir)) }

            val input = getFromUiThread {
                showInputDialog(message = "Choose new name", title = "Rename") {
                    validate(it, isTopLevelClass)
                }
            } ?: return@coroutineScope fail<String>("User didn't input a new name")

            renameOnBackgroundThread(namedToIntermediaryClasses, name, input, user, git)
        }


        //TODO: actually get from input and save to global settings


    }




    //TODO: quick popup when can't find it
    private suspend fun ProjectWrapper.renameOnBackgroundThread(
        namedToIntermediaryClasses: Deferred<MutableMap<String, String>>,
        name: Name,
        input: String,
        user: GitUser,
        git: Deferred<GitRepository>
    ) = asyncWithProgressBar("Renaming...") {

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
//                    is StringSuccess -> showMessageDialog(message = result.value, title = "Rename Success")
            is StringError -> inUiThread {
                showMessageDialog(
                    message = result.value,
                    title = "Rename Error",
                    icon = CommonIcons.Error
                )
            }
        }

        result
    }

    /**
     * Call this while the user is busy (typing the new name) to prevent lag later on.
     * This method will be executed asynchronously so it will return immediately and do the work in the background.
     */
    private suspend fun getOrCloneGit(gitUser: GitUser, yarnRepo: File) = withContext(Dispatchers.IO) {
        YarnRepo(yarnRepo).getOrCloneGit().apply { switchToBranch(gitUser.branchName) }
    }

    private suspend fun ProjectWrapper.getClassNamesMap(yarnRepo: YarnRepo) = withContext(Dispatchers.IO) {
        val map = getIntermediaryClassNames()
        if (map.isEmpty()) {
            yarnRepo.walkMappingsDirectory().forEach { file ->
                if (!file.isDirectory) {
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
     * @return The new name if successful, and a failure message if it failed
     */
    private fun tryRename(
        oldName: Name,
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
            """This was already renamed in a newer version.
If it wasn't, ensure the project SDK is set correctly.
(- Remember to have this error happen ONLY when it has been renamed later)"""
        )
        assert(matchingMappings.size == 1)


        return applyRename(rename, matchingMappings[0], yarn, git, gitUser, namedToIntermediary)
    }


    private fun applyRename(
        rename: Rename,
        renameTarget: Mapping,
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
            namedToIntermediary[renameTarget.deobfuscatedName ?: error("A name has been given")] =
                renameTarget.obfuscatedName
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

        return renameTarget.deobfuscatedName!!.shortName().success
    }

//TODO: push changes
// repo.actuallyPush(YarnRepo.RemoteUrl, UsernamePasswordCredentialsProvider(YarnRepo.GithubUsername, YarnRepo.GithubPassword))

    val Mapping.filePath get() = (root.deobfuscatedName ?: root.obfuscatedName) + ".mapping"


}

private fun String.shortName() = split("/").last()


fun splitPackageAndName(rawName: String): Pair<String?, String> {
    val lastSlashIndex = rawName.lastIndexOf('/')
    return if (lastSlashIndex == -1) null to rawName
    else rawName.splitOn(lastSlashIndex)
}
