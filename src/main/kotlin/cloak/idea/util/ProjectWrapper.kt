package cloak.idea.util


import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import java.io.File
import java.nio.file.Paths
import javax.swing.Icon

interface ProjectWrapper {
    fun showInputDialog(
        message: String, title: String, icon: Icon = CommonIcons.Question, initialValue: String? = null,
        validator: ((String) -> String?)? = null
    ): String?

    fun showMessageDialog(message: String, title: String, icon: Icon = CommonIcons.Info)

    val yarnRepoDir: File
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

    private class InputValidatorWrapper(val validator: (String) -> String?) : InputValidatorEx {
        override fun checkInput(inputString: String) = true
        override fun getErrorText(inputString: String): String? = validator(inputString)

        override fun canClose(inputString: String?) = true
    }

    override fun showInputDialog(
        message: String, title: String, icon: Icon, initialValue: String?, validator: ((String) -> String?)?
    ): String? =
        Messages.showInputDialog(
            project,
            message,
            title,
            icon,
            initialValue,
            validator?.let { InputValidatorWrapper(it) })

    override fun showMessageDialog(message: String, title: String, icon: Icon) =
        Messages.showMessageDialog(project, message, title, icon)

}