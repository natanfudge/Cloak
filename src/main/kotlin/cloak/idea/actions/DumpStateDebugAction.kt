package cloak.idea.actions

import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.CloakAction
import cloak.idea.util.editor
import com.intellij.openapi.actionSystem.AnActionEvent
import java.lang.management.ManagementFactory

class DumpStateDebugAction : CloakAction() {
    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
        return ManagementFactory.getRuntimeMXBean().inputArguments.toString().indexOf("jdwp") >= 0
    }

    override fun actionPerformed(e: AnActionEvent) {
        with(IdeaPlatform(e.project ?: return, e.editor ?: return)) {
//            println(
//                """
//____________DUMP:_______________________________________________
//
//            CURRENT BRANCH: $currentBranch
//            RENAMED NAMES: ${debug.renamedNamesDump()}
//            GIT USER: ${debug.gitUserDump()}
//            YARN CHANGES: ${debug.yarnChangesDump()}
//            SHOWED NOTE ABOUT LICENSE: $showedNoteAboutLicense
//
//________________________________________________________________
//        """
//            )
        }

    }

}