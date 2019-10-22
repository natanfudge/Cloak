package cloak.idea.util

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import javax.swing.Icon


//TODO: make this use the project



abstract class Action(text: String? = null) : AnAction(text) {
    open fun isEnabledAndVisible(event: AnActionEvent): Boolean = true
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isEnabledAndVisible(event)
//        event.presentation.isEnabledAndVisible = true
    }
}

val AnActionEvent.editor get() = getData(CommonDataKeys.EDITOR)
val AnActionEvent.psiElement get() = getData(CommonDataKeys.PSI_ELEMENT)
val AnActionEvent.document get() = getData(PlatformDataKeys.EDITOR)?.document
val AnActionEvent.file get() = getData(CommonDataKeys.VIRTUAL_FILE)