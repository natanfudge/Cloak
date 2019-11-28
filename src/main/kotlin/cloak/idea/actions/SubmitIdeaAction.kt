package cloak.idea.actions

import cloak.actions.SubmitAction
import cloak.git.inSubmittedBranch
import cloak.idea.RenamedIdentifierHighlighter
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import cloak.platform.saved.anythingWasRenamed
import com.intellij.openapi.actionSystem.AnActionEvent

//TODO: clean caches command
class SubmitIdeaAction : CloakAction() {
    override fun isEnabled(event: AnActionEvent): Boolean {
        return IdeaPlatform(event.project ?: return false, event.editor).anythingWasRenamed()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val platform = IdeaPlatform(event.project ?: return, event.editor)
        SubmitAction.submit(platform)
//        RenamedIdentifierHighlighter.rerun(event)
    }

}