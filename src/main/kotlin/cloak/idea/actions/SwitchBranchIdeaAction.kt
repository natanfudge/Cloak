package cloak.idea.actions

import cloak.actions.SwitchBranchAction
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.actionSystem.AnActionEvent

class SwitchBranchIdeaAction : CloakAction() {
    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
        val platform = IdeaPlatform(event.project ?: return false, event.editor ?: return false)
        return platform.branch.all.isNotEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val platform = IdeaPlatform(e.project ?: return, e.editor ?: return)

        SwitchBranchAction.switch(platform).invokeOnCompletion {
            platform.inUiThread {
                CodeFoldingManager.getInstance(e.project)
                    .updateFoldRegionsAsync(e.editor ?: return@inUiThread, true)?.run()
            }
        }
    }
}