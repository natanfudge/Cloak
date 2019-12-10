package cloak.idea.platformImpl

import cloak.git.CloakRepository
import cloak.git.JGit
import cloak.idea.git.IdeaGitRepository
import cloak.idea.util.*
import cloak.platform.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.refactoring.util.CommonRefactoringUtil
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.GithubApiRequestExecutorManager
import org.jetbrains.plugins.github.api.GithubApiRequests
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IdeaPlatform(private val project: Project, private val editor: Editor? = null) : ExtendedPlatform {

    companion object {
        private const val StorageDirectory = "cloak"
    }

    fun inUiThread(action: () -> Unit) = ApplicationManager.getApplication().invokeAndWait(action)
    suspend fun <T> getFromUiThread(input: () -> T): T = suspendCoroutine { cont ->
        ApplicationManager.getApplication().invokeAndWait {
            cont.resume(input())
        }
    }


    override val storageDirectory: Path = Paths.get(PathManager.getSystemPath(), StorageDirectory)

    private class InputValidatorWrapper(private val validator: PlatformInputValidator) : InputValidatorEx {
        override fun checkInput(inputString: String): Boolean {
            return validator.allowEmptyString || inputString.any { !it.isWhitespace() }
        }

        override fun getErrorText(inputString: String): String? = validator.tester?.invoke(inputString)

        override fun canClose(inputString: String?) = true
    }

    override val persistentSaver = IdeaPersistentSaver
    override suspend fun getTwoInputs(
        message: String?,
        request: UserInputRequest,
        descriptionA: String?,
        descriptionB: String?,
        initialValueA: String?,
        initialValueB: String?,
        defaultSelectionA: IntRange?,
        defaultSelectionB: IntRange?,
        validatorA: PlatformInputValidator?,
        validatorB: PlatformInputValidator?
    ) = getFromUiThread {
        showTwoInputsDialog(
            project,
            message,
            request.title,
            CommonIcons.Question,
            InputFieldData(
                descriptionA,
                initialValueA,
                defaultSelectionA,
                validatorA?.let { InputValidatorWrapper(it) }),
            InputFieldData(
                descriptionB,
                initialValueB,
                defaultSelectionB,
                validatorB?.let { InputValidatorWrapper(it) })
        )?.run { Pair(first, if (second == "") null else second) }
    }

    override suspend fun showMessageDialog(message: String, title: String) =
        inUiThread { Messages.showMessageDialog(project, message, title, CommonIcons.Info) }

    override suspend fun showErrorPopup(message: String, title: String) = inUiThread {
        CommonRefactoringUtil.showErrorHint(
            project,
            editor,
            message,
            title,
            null
        )
    }

    override suspend fun getUserInput(
        title: String,
        message: String,
        validator: PlatformInputValidator?
    ): String? = getFromUiThread {
        Messages.showInputDialog(
            project,
            message,
            title,
            CommonIcons.Question,
            null,
            validator?.let { InputValidatorWrapper(it) }
        )
    }

    override suspend fun getMultilineInput(
        title: String,
        message: String,
        validator: PlatformInputValidator?,
        initialValue: String?
    ) = getFromUiThread {
        Messages.showMultilineInputDialog(
            project,
            message,
            title,
            initialValue,
            CommonIcons.Question,
            validator?.let { InputValidatorWrapper(it) }
        )
    }

    override suspend fun getChoiceBetweenOptions(title: String, options: List<String>): String =
        suspendCoroutine { cont ->
            inUiThread {
                JBPopupFactory.getInstance().createPopupChooserBuilder(options).setTitle(title)
                    .setNamerForFiltering { it }
                    .setItemChosenCallback { cont.resume(it) }
                    .setMovable(true)
                    .setRenderer(NiceDropdownList())
                    .createPopup()
                    .showCenteredInCurrentWindow(project)
            }

        }

    override suspend fun getMultipleChoicesBetweenOptions(title: String, options: List<String>): List<String> =
        getFromUiThread {
            val dialog = CheckboxListDialog(options, title)
            dialog.show()
            dialog.getChosenOptions()
        }


    override suspend fun <T> asyncWithText(title: String, action: suspend () -> T): T =
        suspendCoroutine { cont ->
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, title) {
                override fun run(progressIndicator: ProgressIndicator) { // start your process
                    //TODO: it's bad to use runBlocking here, means we can't run it alongside other things.
                    runBlocking { cont.resume(action()) }
                }
            })
        }


    private fun getAccount(): GithubAccount? {
        val auth = GithubAuthenticationManager.getInstance()
        return auth.getSingleOrDefaultAccount(project) ?: auth.requestNewAccount(project)
    }

    private fun getGitExecutor(): GithubApiRequestExecutor? {
        val account = getAccount() ?: return null
        return GithubApiRequestExecutorManager.getInstance().getExecutor(account, project)
    }

    override fun createPullRequest(
        repositoryName: String,
        requestingUser: String,
        requestingBranch: String,
        targetBranch: String,
        targetUser: String,
        title: String,
        body: String
    ): PullRequestResponse? {
        val response = getGitExecutor()?.execute(
            GithubApiRequests.Repos.PullRequests.create(
                GithubServerPath.DEFAULT_SERVER,
                username = requestingUser,
                title = title,
                base = targetBranch,
                head = "$requestingUser:$requestingBranch",
                description = body,
                repoName = repositoryName
            )
        )

        return response?.htmlUrl?.let { PullRequestResponse(it) }
    }


    override fun forkRepository(repositoryName: String, forkedUser: String, forkingUser: String): ForkResult =
        when (getGitExecutor()?.execute(
            GithubApiRequests.Repos.Forks.create(
                GithubServerPath.DEFAULT_SERVER,
                username = forkedUser,
                repoName = repositoryName
            )
        )) {
            null -> ForkResult.Canceled
            else -> ForkResult.Success
        }

    override suspend fun getAuthenticatedUsername(): String? {
        return getFromUiThread { getAccount()?.name }
    }

    override fun createGit(git: JGit, path: File): CloakRepository {
        return IdeaGitRepository(project, git, path)
    }


}