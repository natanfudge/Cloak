package cloak.idea.actions

import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.*
import com.intellij.openapi.actionSystem.AnActionEvent

class ModifyJavadocIdeaAction : CloakAction() {
    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
        return true
//        val psiduck = event.psiElement ?: return false
//        return canBeRenamed(psiduck)
    }


    override fun actionPerformed(event: AnActionEvent) {
//        val element = event.psiElement ?: return
//        val nameBeforeRenames = element.asName()
        val project = event.project ?: return

        val platform = IdeaPlatform(event.project ?: return, event.editor ?: return)

        val actualElement = event.elementAtCaret ?: return
        val dialog = JavadocInputDialog(project, title = "Foo bar baz", targetElement = actualElement.parent)
        dialog.show()

//        GlobalScope.launch {
////            ModifyJavadocAction.modify(platform, nameBeforeRenames)
//        }

    }

}