package cloak.idea.util


import ClassNameProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import javax.swing.Icon
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface ProjectWrapper {
    fun showInputDialog(
        message: String, title: String, icon: Icon = CommonIcons.Question, initialValue: String? = null,
        validator: ((String) -> String?)? = null
    ): String?

    fun showMessageDialog(message: String, title: String, icon: Icon = CommonIcons.Info)

    val yarnRepoDir: File

    /**
     * Needed to find methods that have intermediary descriptors while the user sees named descriptors
     * You just need to save this, it will be filled if it's empty while it's being consumed
     */
    fun getIntermediaryClassNames(): MutableMap<String, String>

    suspend fun <T> asyncWithProgressBar(title: String, action: suspend () -> T): T

    fun inUiThread(action: () -> Unit)

    suspend fun <T> getFromUiThread(input: () -> T): T

}

class IdeaProjectWrapper(private val project: Project) : ProjectWrapper {

    companion object IdeaStorage {
        private const val StorageDirectory = "cloak"
        private const val YarnRepositoryDirectory = "yarn"
        private val YarnRepoDir = getGlobalStorage(YarnRepositoryDirectory)
        private fun getGlobalStorage(path: String): File =
            Paths.get(PathManager.getSystemPath(), StorageDirectory, path).toFile()
    }

    override val yarnRepoDir = IdeaStorage.YarnRepoDir
    override fun getIntermediaryClassNames(): MutableMap<String, String> =
        ClassNameProvider.getInstance().state.namedToIntermediary

    override suspend fun <T> asyncWithProgressBar(title: String, action: suspend () -> T): T =
        suspendCoroutine { cont ->
            ProgressManager.getInstance().run(object : Backgroundable(project, title) {
                override fun run(progressIndicator: ProgressIndicator) { // start your process
                    runBlocking { cont.resume(action()) }
                }
            })
        }

    private class InputValidatorWrapper(val validator: (String) -> String?) : InputValidatorEx {
        override fun checkInput(inputString: String) = true
        override fun getErrorText(inputString: String): String? = validator(inputString)

        override fun canClose(inputString: String?) = true
    }

    override fun showInputDialog(
        message: String, title: String, icon: Icon, initialValue: String?, validator: ((String) -> String?)?
    ): String? = Messages.showInputDialog(
        project,
        message,
        title,
        icon,
        initialValue,
        validator?.let { InputValidatorWrapper(it) })


    override fun showMessageDialog(message: String, title: String, icon: Icon) =
        Messages.showMessageDialog(project, message, title, icon)

    override fun inUiThread(action: () -> Unit) = ApplicationManager.getApplication().invokeAndWait(action)
    override suspend fun <T> getFromUiThread(input: () -> T): T = suspendCoroutine { cont ->
        ApplicationManager.getApplication().invokeAndWait {
            cont.resume(input())
        }
    }

}