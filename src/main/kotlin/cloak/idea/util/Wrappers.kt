package cloak.idea.util

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import javax.swing.Icon

fun showInputDialog(message: String, title: String, icon: Icon = Icons.Message.Question) : String? =
    Messages.showInputDialog(message, title, icon)

abstract class Action(text : String? = null) : AnAction(text){
    open fun isEnabledAndVisible(event: AnActionEvent) : Boolean = true
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isEnabledAndVisible(event)
//        event.presentation.isEnabledAndVisible = true
    }
}

val AnActionEvent.editor get() = getData(CommonDataKeys.EDITOR)
val AnActionEvent.psiElement get() = getData(CommonDataKeys.PSI_ELEMENT)