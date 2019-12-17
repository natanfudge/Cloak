//package cloak.idea.actions
//
//import cloak.actions.DeleteBranchesAction
//import cloak.idea.platformImpl.IdeaPlatform
//import cloak.idea.util.CloakAction
//import cloak.idea.util.editor
//import cloak.platform.saved.allBranches
//import com.intellij.openapi.actionSystem.AnActionEvent
//
//
//class DeleteBranchesIdeaAction : CloakAction() {
//    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
//        val platform = IdeaPlatform(event.project ?: return false, event.editor ?: return false)
//        return platform.allBranches.size >= 1
//    }
//    override fun actionPerformed(e: AnActionEvent) {
//        DeleteBranchesAction.delete(IdeaPlatform(e.project ?: return, e.editor))
//    }
//}
//
