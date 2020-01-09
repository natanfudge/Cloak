package cloak.idea.actions

import cloak.actions.SubmitAction
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.actionSystem.AnActionEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class SubmitIdeaAction : CloakAction() {
    override fun isEnabled(event: AnActionEvent): Boolean {
        return IdeaPlatform(event.project ?: return false, event.editor).branch.anythingWasAdded()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val platform = IdeaPlatform(event.project ?: return, event.editor)
        GlobalScope.launch {
            SubmitAction.submit(platform)

            platform.inUiThread {
                CodeFoldingManager.getInstance(event.project)
                    .updateFoldRegionsAsync(event.editor ?: return@inUiThread, true)?.run()
            }

        }

    }

}