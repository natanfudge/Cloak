package cloak.idea.actions

import cloak.actions.RenameAction
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.*
import cloak.util.StringSuccess
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

//TODO: list of default ignored auto-imports
// "new project" dialog: Kotlin/Java, include publishing block
// turn on "autoscroll from source" by default
// give cool icon to fabric.mod.json and modid.mixin.json
// inspections for fabric stuff?
// special colors for @SideOnly

//TODO: when we have yarn that can work on any version:
// - Have an option to use mappings from the newest version,
// have a blue button thing to remap the jar and optionally generate sources again.
// You can also choose to "auto update", which will essentially do that after every rename.
// This button also converts the mappings from yarn to tiny and puts them in the project folder, so it can be checked
// into source control. when this is done all the green marks reset.
// The green marks reset if the jar gets remapped by some other way as well.
// - The user must first use the correct mappings that will be generated, by using an intention that copies the correct
// build.gradle line to clipboard.


fun isMinecraftPackageName(packageName: String) = packageName.startsWith("net.minecraft")

class RenameIdeaAction : CloakAction() {
    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
        val element = event.psiElement ?: return false
        // Only allow minecraft classes
        if (!isMinecraftPackageName(element.packageName)) return false


        return when (element) {
            is PsiClass, is PsiField, is PsiParameter -> true
            is PsiMethod -> !element.isConstructor
            else -> false
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val element = event.psiElement ?: return
        val isTopLevelClass = element is PsiClass && !element.isInnerClass
        val nameBeforeRenames = element.asName()

        val platform = IdeaPlatform(event.project ?: return, event.editor ?: return)
        GlobalScope.launch {
            RenameAction.rename(platform, nameBeforeRenames, isTopLevelClass)
        }

    }


}

