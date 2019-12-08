package cloak.idea.actions

import cloak.actions.RenameAction
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.*
import cloak.util.StringSuccess
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// inspections for fabric stuff?

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

//TODO: validation:
// - Moving packages with protected/package private fields
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

    //TODO: sort parameters
    //TODO: shift constructor parameters


    override fun actionPerformed(event: AnActionEvent) {
        val element = event.psiElement ?: return
        val editor = event.editor ?: return
        val isTopLevelClass = element is PsiClass && !element.isInnerClass
        val nameBeforeRenames = element.asName()

        val platform = IdeaPlatform(event.project ?: return, editor)
        GlobalScope.launch {
            val rename = RenameAction.rename(platform, nameBeforeRenames, isTopLevelClass)

            if (rename is StringSuccess) {
                platform.inUiThread {
                    val identifier = when (val caretElement = event.elementAtCaret) {
                        is PsiNameIdentifierOwner -> caretElement.nameIdentifier
                        is PsiIdentifier -> caretElement
                        else -> return@inUiThread
                    }

                    CodeFoldingManager.getInstance(event.project)
                        .updateFoldRegionsAsync(editor, true)?.run()

                    val range = identifier?.textRange ?: return@inUiThread

                    // Manually fold because idea is a pos (this took like 3+ hours to figure out this code)
                    event.editor?.foldingModel?.runBatchFoldingOperation {
                        val foldRegion = editor.foldingModel.getFoldRegion(range.startOffset, range.endOffset)
                            ?: return@runBatchFoldingOperation
                        foldRegion.isExpanded = false
                    }
                }
            }
        }

    }


}

