package cloak.idea.platformImpl

import cloak.git.CloakRepository
import cloak.git.JGit
import cloak.idea.git.IdeaGitRepository
import cloak.idea.gui.*
import cloak.idea.util.*
import cloak.platform.*
import cloak.platform.saved.BranchInfoApi
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


class IdeaPlatform(val project: Project, val editor: Editor? = null) : ExtendedPlatform {

    override val branch = BranchInfoApi(this)

    companion object {
        private val StorageDirectory = "cloak"
    }

    fun inUiThread(action: () -> Unit) = ApplicationManager.getApplication().invokeAndWait(action)
    suspend fun <T> getFromUiThread(input: () -> T): T = suspendCoroutine { cont ->
        ApplicationManager.getApplication().invokeLater {
            cont.resume(input())
        }
    }


    override fun getStorageDirectory(): Path = Paths.get(PathManager.getSystemPath(), StorageDirectory)



    override val persistentSaver = IdeaPersistentSaver
    override suspend fun getTwoInputs(
        message: String?,
        request: UserInputRequest,
        helpId : String? ,
        inputA : InputFieldData,
        inputB : InputFieldData
    ) = getFromUiThread {
        showTwoInputsDialog(project, message, request.title, CommonIcons.Question,helpId, inputA, inputB)
            ?.run { Pair(first, if (second == "") null else second) }
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
            val dialog = CheckboxListDialog(project,options, title)
            dialog.show()
            dialog.getChosenOptions()
        }

    override suspend fun getJavadocInput(title: String, oldJavadoc: String): String? {
        return getFromUiThread { getIdeaJavadocInput(project, title, oldJavadoc) }
    }


    override suspend fun <T> asyncWithText(title: String, action: suspend (AsyncContext) -> T): T =
        suspendCoroutine { cont ->
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, title) {
                override fun run(progressIndicator: ProgressIndicator) { // start your process
                    //TODO: it's bad to use runBlocking here, means we can't run it alongside other things.
                    runBlocking { cont.resume(action(IdeaAsyncContext(progressIndicator))) }
                }
            })
        }


    private fun getAccount(): GithubAccount {
        val auth = GithubAuthenticationManager.getInstance()
        return auth.getSingleOrDefaultAccount(project) ?: auth.requestNewAccount(project)
        ?: throw UserNotAuthenticatedException()
    }

    private fun getGitExecutor(): GithubApiRequestExecutor {
        val account = getAccount()
        return GithubApiRequestExecutorManager.getInstance().getExecutor(account, project)
            ?: throw UserNotAuthenticatedException()
    }


    override fun createPullRequest(
        repositoryName: String,
        requestingUser: String,
        requestingBranch: String,
        targetBranch: String,
        targetUser: String,
        title: String,
        body: String
    ): PullRequestResponse {
        val response = try {
            getGitExecutor().execute(
                GithubApiRequests.Repos.PullRequests.create(
                    GithubServerPath.DEFAULT_SERVER,
                    username = targetUser,
                    title = title,
                    base = targetBranch,
                    head = "$requestingUser:$requestingBranch",
                    description = body,
                    repoName = repositoryName
                )
            )
        } catch (e: UserNotAuthenticatedException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(
                "Could not send pull request with repositoryName = $repositoryName, requestingUser = $requestingUser, " +
                        "requestingBranch = $requestingBranch, targetBranch = $targetBranch, targetUser = $targetUser," +
                        "title = $title, body = $body", e
            )
        }
        return PullRequestResponse(response.htmlUrl)
    }


    override fun forkRepository(repositoryName: String, forkedUser: String, forkingUser: String): ForkResult {
        getGitExecutor().execute(
            GithubApiRequests.Repos.Forks.create(
                GithubServerPath.DEFAULT_SERVER,
                username = forkedUser,
                repoName = repositoryName
            )
        )
        return ForkResult.Success
    }


    override suspend fun getAuthenticatedUser(): GitUser {
        return getFromUiThread { GitUser(getAccount().name) }
    }

    override fun createGit(git: JGit, path: File): CloakRepository {
        return IdeaGitRepository(project, git, path)
    }


}