package cloak.idea

import cloak.idea.util.Action
import cloak.idea.util.psiElement
import cloak.idea.util.showInputDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter


class RemapAction : Action("Hello") {
    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
//        val editor = context.editor ?: return false
        val element = event.psiElement ?: return false

        return when(element){
            is PsiClass, is PsiField, is PsiParameter -> true
            is PsiMethod -> !element.isConstructor
            else -> false
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val input = showInputDialog(message = "PUT YOUR CREDIT NUMBER HERE", title = "NOT SCAM")

    }
}