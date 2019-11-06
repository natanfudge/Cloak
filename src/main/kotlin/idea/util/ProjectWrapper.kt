package cloak.idea.util


import ClassNameProvider
import cloak.mapping.rename.GitUser
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.refactoring.util.CommonRefactoringUtil
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import javax.swing.Icon
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class RenameInput(val newName: String, val explanation: String?)
interface ProjectWrapper {
//    fun showInputDialog(
//        message: String, title: String, icon: Icon = CommonIcons.Question, initialValue: String? = null,
//        allowEmptyString: Boolean = false,
//        validator: ((String) -> String?)? = null
//    ): String?

    fun requestRenameInput(newNameValidator: (String) -> String?): RenameInput?

    fun showMessageDialog(message: String, title: String, icon: Icon = CommonIcons.Info)

    fun showErrorPopup(message: String, title: String)

    fun getUserInput(
        title: String,
        message: String,
        allowEmptyString: Boolean = false,
        validator: ((String) -> String?)? = null
    ): String?

    // Returns null if the user cancels dialog
    fun getGitUser(): GitUser?

    val yarnRepoDir: File

    /**
     * Needed to find methods that have intermediary descriptors while the user sees named descriptors
     * You just need to save this, it will be filled if it's empty while it's being consumed
     */
    fun getIntermediaryClassNames(): MutableMap<String, String>

    suspend fun <T> asyncWithText(title: String, action: suspend () -> T): T

    fun inUiThread(action: () -> Unit)

    suspend fun <T> getFromUiThread(input: () -> T): T

}

class IdeaProjectWrapper(private val project: Project, private val editor: Editor?) : ProjectWrapper {

    companion object IdeaStorage {
        private const val StorageDirectory = "cloak"
        private const val YarnRepositoryDirectory = "yarn"
        private val YarnRepoDir = getGlobalStorage(YarnRepositoryDirectory)
        private fun getGlobalStorage(path: String): File =
            Paths.get(PathManager.getSystemPath(), StorageDirectory, path).toFile()

        private const val GithubUserKey = "github_user"
        private const val GithubEmailKey = "github_email"
    }

    override val yarnRepoDir = IdeaStorage.YarnRepoDir
    override fun getIntermediaryClassNames(): MutableMap<String, String> =
        ClassNameProvider.getInstance().state.namedToIntermediary

    override suspend fun <T> asyncWithText(title: String, action: suspend () -> T): T =
        suspendCoroutine { cont ->
            ProgressManager.getInstance().run(object : Backgroundable(project, title) {
                override fun run(progressIndicator: ProgressIndicator) { // start your process
                    runBlocking { cont.resume(action()) }
                }
            })
        }

    private class InputValidatorWrapper(
        private val allowEmptyString: Boolean = false,
        private val validator: ((String) -> String?)? = null
    ) : InputValidatorEx {
        override fun checkInput(inputString: String): Boolean {
            return allowEmptyString || inputString.any { !it.isWhitespace() }
        }

        override fun getErrorText(inputString: String): String? = validator?.invoke(inputString)

        override fun canClose(inputString: String?) = true
    }

    override fun requestRenameInput(newNameValidator: (String) -> String?): RenameInput? {
        val (newName, explanation) = showTwoInputsDialog(
            project,
            message = null, title = "Rename", descriptionA = "New Name", descriptionB = "Explanation",
            validatorA = InputValidatorWrapper(allowEmptyString = false, validator = newNameValidator),
            validatorB = InputValidatorWrapper(allowEmptyString = true)
        ) ?: return null

        return RenameInput(newName, if (explanation == "") null else explanation)
    }


    override fun showMessageDialog(message: String, title: String, icon: Icon) =
        Messages.showMessageDialog(project, message, title, icon)

    override fun showErrorPopup(message: String, title: String) {
        inUiThread {
            CommonRefactoringUtil.showErrorHint(
                project,
                editor,
                message,
                title,
                null
            )
        }
    }

    override fun getUserInput(
        title: String,
        message: String,
        allowEmptyString: Boolean,
        validator: ((String) -> String?)?
    ): String? {
        return Messages.showInputDialog(
            project,
            message,
            title,
            CommonIcons.Question,
            null,
            InputValidatorWrapper(allowEmptyString, validator)
        )
    }

    private fun getNewGitUser(): GitUser? {
        val (username, email) = showTwoInputsDialog(
            project, "Enter your Github user to attribute contributions to", "Identification",
            "Username", "Email", validatorA = InputValidatorWrapper(), validatorB = InputValidatorWrapper()
        ) ?: return null

        PropertiesComponent.getInstance().setValue(GithubUserKey, username)
        PropertiesComponent.getInstance().setValue(GithubEmailKey, email)
        return GitUser(username, email)
    }

    override fun getGitUser(): GitUser? {
        val properties = PropertiesComponent.getInstance()
        val username = properties.getValue(GithubUserKey)
        val email = properties.getValue(GithubEmailKey)
        return if (username != null && email != null) GitUser(username, email = email)
        else getNewGitUser()
    }

    override fun inUiThread(action: () -> Unit) = ApplicationManager.getApplication().invokeAndWait(action)
    override suspend fun <T> getFromUiThread(input: () -> T): T = suspendCoroutine { cont ->
        ApplicationManager.getApplication().invokeAndWait {
            cont.resume(input())
        }
    }

}