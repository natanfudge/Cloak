package cloak.idea.actions

import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import cloak.platform.ActiveMappings
import com.intellij.openapi.actionSystem.AnActionEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ToggleMappingHighlightingIdeaAction : CloakAction() {
    override fun getText(event: AnActionEvent): String? =
        if (ActiveMappings.areActive()) "Deactivate mapping highlighting"
        else "Activate mapping highlighting"

    override fun actionPerformed(e: AnActionEvent) {
        if (ActiveMappings.areActive()) ActiveMappings.deactivate()
        else {
            val platform = IdeaPlatform(e.project ?: return, e.editor)
            GlobalScope.launch {
                platform.asyncWithText("Loading mappings..."){
                    ActiveMappings.refresh(platform)
                }
            }
        }
    }

}