package cloak.idea.platformImpl

import cloak.idea.util.*
import cloak.platform.ExtendedPlatform
import cloak.platform.PlatformInputValidator
import cloak.platform.UserInputRequest
import com.intellij.openapi.actionSystem.AnActionEvent
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
                    //TODO: it's probably bad to pass a suspend() -> T and use runBlocking.
                    runBlocking { cont.resume(action()) }
                }
            })
        }


}