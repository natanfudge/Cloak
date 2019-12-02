package cloak.idea.actions

import cloak.actions.SwitchBranchAction
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import cloak.platform.saved.allBranches
import com.intellij.openapi.actionSystem.AnActionEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SwitchBranchIdeaAction : CloakAction() {
    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
        val platform = IdeaPlatform(event.project ?: return false, event.editor ?: return false)
        return platform.allBranches.size >= 1
    }

    override fun actionPerformed(e: AnActionEvent) {
        val platform = IdeaPlatform(e.project ?: return, e.editor ?: return)

        SwitchBranchAction.switch(platform)
    }
}