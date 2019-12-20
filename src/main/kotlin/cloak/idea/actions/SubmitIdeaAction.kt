package cloak.idea.actions

import cloak.actions.SubmitAction
import cloak.git.inSubmittedBranch
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import cloak.platform.ExtendedPlatform
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.actionSystem.AnActionEvent

fun ExtendedPlatform.anythingWasRenamed() = branch.renames.isNotEmpty()

class SubmitIdeaAction : CloakAction() {
    override fun isEnabled(event: AnActionEvent): Boolean {
        return IdeaPlatform(event.project ?: return false, event.editor).anythingWasRenamed()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val platform = IdeaPlatform(event.project ?: return, event.editor)
        SubmitAction.submit(platform).invokeOnCompletion {
            platform.inUiThread {
                CodeFoldingManager.getInstance(event.project)
                    .updateFoldRegionsAsync(event.editor ?: return@inUiThread, true)?.run()
            }
        }
    }

}