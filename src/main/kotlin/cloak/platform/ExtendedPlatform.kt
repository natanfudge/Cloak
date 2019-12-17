package cloak.platform

import cloak.git.CloakRepository
import cloak.git.JGit
import cloak.platform.saved.BranchInfoApi
import java.io.File
import java.nio.file.Path

interface ExtendedPlatform {
    val storageDirectory: Path

    val persistentSaver: PersistentSaver

    val branch : BranchInfoApi get() = BranchInfoApi(this)

    suspend fun getTwoInputs(
        message: String?,
        request: UserInputRequest,
        descriptionA: String?,
        descriptionB: String?,
        initialValueA: String? = null,
        initialValueB: String? = null,
        defaultSelectionA: IntRange? = null,
        defaultSelectionB: IntRange? = null,
        validatorA: PlatformInputValidator? = null,
        validatorB: PlatformInputValidator? = null
    ): Pair<String, String?>?

    suspend fun getUserInput(
        title: String,
        message: String,
        validator: PlatformInputValidator? = null
    ): String?

    suspend fun getMultilineInput(
        title: String,
        message: String,
        validator: PlatformInputValidator? = null,
        initialValue: String? = null
    ): String?

    suspend fun getChoiceBetweenOptions(title: String, options: List<String>): String

    suspend fun getMultipleChoicesBetweenOptions(title: String, options: List<String>): List<String>

    suspend fun showMessageDialog(message: String, title: String)

    suspend fun showErrorPopup(message: String, title: String)

    suspend fun <T> asyncWithText(title: String, action: suspend () -> T): T

    fun createPullRequest(
        repositoryName: String,
        requestingUser: String,
        requestingBranch: String,
        targetBranch: String,
        targetUser: String,
        title: String,
        body: String
    ): PullRequestResponse?

    fun forkRepository(repositoryName: String, forkedUser: String, forkingUser: String): ForkResult

    /**
     * @throws UserNotAuthenticatedException
     */
    suspend fun getAuthenticatedUser(): GitUser

    fun createGit(git: JGit, path : File): CloakRepository

}