package cloak.idea

import cloak.idea.util.CloakAction
import cloak.idea.util.IdeaProjectWrapper
import cloak.idea.util.editor
import com.intellij.openapi.actionSystem.AnActionEvent

class SubmitAction : CloakAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = IdeaProjectWrapper(e.project ?: return, e.editor)

    }

}