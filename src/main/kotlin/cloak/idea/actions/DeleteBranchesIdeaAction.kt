package cloak.idea.actions

import cloak.actions.DeleteBranchesAction
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import com.intellij.openapi.actionSystem.AnActionEvent


//TODO, checkboxes.
class DeleteBranchesIdeaAction : CloakAction() {
    override fun actionPerformed(e: AnActionEvent) {
        DeleteBranchesAction.delete(IdeaPlatform(e.project ?: return, e.editor))
    }
}

