package cloak.mapping.rename

import cloak.git.GitRepository
import cloak.git.YarnRepo
import cloak.idea.providerUtils.NewName
import cloak.idea.util.ProjectWrapper
import cloak.mapping.*
import cloak.mapping.descriptor.remap
import cloak.mapping.mappings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.eclipse.jgit.lib.PersonIdent
import java.io.File
import javax.lang.model.SourceVersion

data class GitUser(val name: String, val email: String) {
    val jgit = PersonIdent(name, email)
    val branchName = name
}

val PersonIdent.cloakUser get() = GitUser(this.name, this.emailAddress)

object Renamer {

    /** User input is in named but the repo is in intermediary */
    private fun <T> T.remapParameterDescriptors(namedToIntermediary: MutableMap<String, String>): T = when (this) {
        is MethodName -> copy(
            parameterTypes = parameterTypes.map { it.remap(namedToIntermediary) }
        ) as T
        is ParamName -> copy(index, methodIn.remapParameterDescriptors(namedToIntermediary)) as T
        else -> this
    }

    /**
     * Returns the new name
     */
    suspend fun rename(
        project: ProjectWrapper,
        name: Name,
        isTopLevelClass: Boolean
    ): Errorable<NewName> = with(project) {
        coroutineScope {
            val user = getFromUiThread { getGitUser() }
                ?: return@coroutineScope fail<NewName>("User didn't provide cloak.git info")

            val (git, namedToIntermediaryClasses, matchingMapping) = asyncWithText("Preparing rename...") {
                val git = getOrCloneGit(gitUser = user, yarnRepo = yarnRepoDir)
                val namedToIntermediaryClasses = getClassNamesMap(YarnRepo.at(yarnRepoDir))
                val yarn = YarnRepo.at(yarnRepoDir)

                val oldName = name.remapParameterDescriptors(namedToIntermediaryClasses)

                Triple(
                    git, namedToIntermediaryClasses,
                    oldName.getMatchingMappingIn(yarn, project = this@with, namedToInt = namedToIntermediaryClasses)
                )
            }

            if (matchingMapping == null) {
                showErrorPopup(
                    "This was already renamed or doesn't exist in a newer version."
                    , title = "Cannot rename"
                )
                return@coroutineScope fail<NewName>("Can't find matching mappings")

            }

            val (input, explanation) = getFromUiThread {
                requestRenameInput { validate(it, isTopLevelClass) }
            } ?: return@coroutineScope fail<NewName>("User didn't input a new name")

            asyncWithText("Renaming...") {
                val result = tryRename(
                    oldName = name,
                    newName = input,
                    yarnRepo = yarnRepoDir,
                    explanation = explanation,
                    gitUser = user,
                    git = git,
                    namedToIntermediary = namedToIntermediaryClasses,
                    matchingMapping = matchingMapping
                )

                if (result is StringError) showErrorPopup(message = result.value, title = "Rename Error")

                result
            }

        }


    }


    /**
     * Call this while the user is busy (typing the new name) to prevent lag later on.
     * This method will be executed asynchronously so it will return immediately and do the work in the background.
     */
    private suspend fun getOrCloneGit(gitUser: GitUser, yarnRepo: File) = withContext(Dispatchers.IO) {
        val yarn = YarnRepo.at(yarnRepo)
        yarn.switchToBranch(gitUser.branchName)
        yarn.getOrCloneGit()
    }

    private suspend fun ProjectWrapper.getClassNamesMap(yarnRepo: YarnRepo) = withContext(Dispatchers.IO) {
        val map = getIntermediaryClassNames()
        if (map.isEmpty()) {
            for (relativePath in yarnRepo.getMappingsFilesLocations()) {
                MappingsFile.read(yarnRepo.getMappingsFile("$relativePath$MappingsExtension")).visitClasses { mapping ->
                    mapping.getFullMapping()?.let { (obf, deobf) -> map[deobf] = obf }
                }
            }
        }
        map
    }

    private fun ClassMapping.getFullMapping(): Pair<String, String>? {
        val fullPath = mutableListOf<ClassMapping>()
        var next: ClassMapping? = this@getFullMapping
        while (next != null) {
            if (next.deobfuscatedName == null) return null
            fullPath.add(next)
            next = next.parent
        }

        fullPath.reverse()

        return Pair(fullPath.joinToString(Joiner.InnerClass) { it.obfuscatedName },
            fullPath.joinToString(Joiner.InnerClass) {
                it.deobfuscatedName ?: error("It's checked earlier that deobfuscatedName != null")
            }
        )
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
     * @param yarnRepo A place to store the yarn cloak.git repository.
     * @param gitUser Git identifier of the user who made the mappings
     *
     *
     * @return The new name if successful, and a failure message if it failed
     */
    private fun tryRename(
        matchingMapping: Mapping,
        oldName: Name,
        newName: String,
        explanation: String?,
        yarnRepo: File,
        git: GitRepository,
        gitUser: GitUser,
        namedToIntermediary: MutableMap<String, String>
    )
            : Errorable<NewName> {
        val (packageName, newClassName) = splitPackageAndName(newName)
        if (packageName != null && (oldName !is ClassName || oldName.classIn != null)) {
            return fail("Changing the package name can only be done on top level classes.")
        }

        val yarn = YarnRepo.at(yarnRepo)

        val rename = Rename(
            originalName = oldName, newName = newClassName, explanation = explanation,
            newPackageName = packageName
        )

        val result = applyRename(rename, matchingMapping, yarn, git, gitUser, namedToIntermediary)
        return if (result is StringError) result.map { NewName("", null) }
        else NewName(newClassName, packageName).success

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
        val newMappingLocation = yarn.getMappingsFile(newPath)


        if (renameTarget.duplicatesAnotherMapping(newMappingLocation)) {
            return fail("There's another ${renameTarget.typeName()} named that way already.")
        }
        val presentableNewName = renameTarget.toString()


        if (oldPath != newPath) {
            git.remove(yarn.pathOfMappingFromGitRoot(oldPath))
        }

        renameTarget.root.writeTo(newMappingLocation)
        git.stageChanges(yarn.pathOfMappingFromGitRoot(newPath))
        git.commit(author = user.jgit, commitMessage = "$presentableOldName -> $presentableNewName")

        println("Changes commited successfully!")

        return renameTarget.deobfuscatedName!!.shortName().success
    }

//TODO: push changes
// repo.actuallyPush(YarnRepo.RemoteUrl, UsernamePasswordCredentialsProvider(YarnRepo.GithubUsername, YarnRepo.GithubPassword))

    private val Mapping.filePath get() = (root.deobfuscatedName ?: root.obfuscatedName) + ".mapping"


}

private fun Mapping.duplicatesAnotherMapping(newMappingLocation: File): Boolean = when (this) {
    is ClassMapping -> if (parent == null) newMappingLocation.exists() else parent.innerClasses.anythingElseHasTheSameObfName()
    // With methods you can overload the same name as long as the descriptor is different
    is MethodMapping -> parent.methods.any { it !== this && it.deobfuscatedName == deobfuscatedName && it.descriptor == descriptor }
    is FieldMapping -> parent.fields.anythingElseHasTheSameObfName()
    is ParameterMapping -> parent.parameters.anythingElseHasTheSameObfName()
}


private fun String.shortName() = split("/").last()


fun splitPackageAndName(rawName: String): Pair<String?, String> {
    val lastSlashIndex = rawName.lastIndexOf('/')
    return if (lastSlashIndex == -1) null to rawName
    else rawName.splitOn(lastSlashIndex)
}
