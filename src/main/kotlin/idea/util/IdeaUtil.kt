package cloak.idea.util

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine




abstract class CloakAction(text: String? = null) : AnAction(text) {
    open fun isEnabledAndVisible(event: AnActionEvent): Boolean = true
    open fun isEnabled(event: AnActionEvent) : Boolean = true
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isEnabledAndVisible(event)
        event.presentation.isEnabled = isEnabled(event)
    }
}

val AnActionEvent.editor get() = getData(CommonDataKeys.EDITOR)
val AnActionEvent.psiElement get() = getData(CommonDataKeys.PSI_ELEMENT)
val AnActionEvent.document get() = getData(PlatformDataKeys.EDITOR)?.document
val AnActionEvent.file get() = getData(CommonDataKeys.VIRTUAL_FILE)
fun Project.executeCommand(name: String? = null, groupId: Any? = null, command: () -> Unit) =
    CommandProcessor.getInstance().executeCommand(this, command, name, groupId)

