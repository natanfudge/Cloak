package cloak.idea.util

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile


abstract class CloakAction(text: String? = null) : AnAction(text) {
    open fun isEnabledAndVisible(event: AnActionEvent): Boolean = true
    open fun isEnabled(event: AnActionEvent): Boolean = true
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isEnabledAndVisible(event)
        event.presentation.isEnabled = isEnabled(event)
    }
}

val AnActionEvent.editor: Editor? get() = getData(CommonDataKeys.EDITOR)
val AnActionEvent.psiElement: PsiElement? get() = getData(CommonDataKeys.PSI_ELEMENT)
val AnActionEvent.document: Document? get() = getData(PlatformDataKeys.EDITOR)?.document
val AnActionEvent.file: VirtualFile? get() = getData(CommonDataKeys.VIRTUAL_FILE)
val AnActionEvent.psiFile: PsiFile? get() = getData(LangDataKeys.PSI_FILE)
val AnActionEvent.caret: Caret? get() = getData(CommonDataKeys.CARET)
val AnActionEvent.elementAtCaret: PsiElement? get() = editor?.caretModel?.offset?.let { psiFile?.findElementAt(it) }

