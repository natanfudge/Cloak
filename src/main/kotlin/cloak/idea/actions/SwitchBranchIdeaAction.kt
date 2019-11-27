package cloak.idea.actions

import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import com.intellij.ide.scratch.LRUPopupBuilder
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory

class SwitchBranchIdeaAction : CloakAction() {

    override fun actionPerformed(e: AnActionEvent) {
//        LRUPopupBuilder.forFileLanguages(
//            project,
//            ActionsBundle.message("action.NewScratchFile.text.with.new"),
//            null,
//            consumer
//        ).showCenteredInCurrentWindow(project)

        //TODO: bigger font
        //TODO: figure out search (createActionGroupPopup (https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/popups.html))
        JBPopupFactory.getInstance().createPopupChooserBuilder(listOf("the first option is good", "the second option is better", "but third time's the charm"))
            .setTitle("Choose Branch")
            .setMovable(true)
            .createPopup()
            .showInBestPositionFor(e.editor ?: return)
    }
}