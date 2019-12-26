package cloak.idea.actions

import cloak.actions.ModifyJavadocAction
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.asName
import cloak.idea.util.editor
import cloak.idea.util.psiElement
import com.intellij.openapi.actionSystem.AnActionEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ModifyJavadocIdeaAction : CloakAction() {
    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
        val psiduck = event.psiElement ?: return false
        return canBeRenamed(psiduck)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val element = event.psiElement ?: return

        val platform = IdeaPlatform(event.project ?: return, event.editor ?: return)

        val nameBeforeRenames = element.asName()
        GlobalScope.launch {
            if (!ModifyJavadocAction.modify(platform, nameBeforeRenames)) {
                println("Could not modify javadoc!")
            }
        }

    }

}