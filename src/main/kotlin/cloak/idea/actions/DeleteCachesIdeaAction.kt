package cloak.idea.actions

import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import cloak.platform.saved.cleanAutoGeneratedFieldNames
import cloak.platform.saved.cleanBranchInfo
import cloak.platform.saved.cleanLatestIntermediaryNmames
import com.intellij.openapi.actionSystem.AnActionEvent

class DeleteCachesIdeaAction : CloakAction() {
    override fun actionPerformed(e: AnActionEvent) {
        with(IdeaPlatform(e.project ?: return, e.editor)) {
            cleanBranchInfo()
            cleanAutoGeneratedFieldNames()
            cleanLatestIntermediaryNmames()
        }
    }

}