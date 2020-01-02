package cloak.idea.actions

import cloak.actions.RenameAction
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.*
import com.github.michaelbull.result.Ok
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.lang.Language
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.actions.BaseRefactoringAction
import com.intellij.refactoring.rename.RenameHandlerRegistry
import com.intellij.refactoring.rename.inplace.InplaceRefactoring
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.MemberInplaceRenamer
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.containers.NotNullList
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

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

fun canBeRenamed(psiduck: PsiElement): Boolean {
    // Only allow minecraft classes
    if (!isMinecraftPackageName(psiduck.packageName)) return false

    return when (psiduck) {
        is PsiClass, is PsiField, is PsiParameter -> true
        is PsiMethod -> !psiduck.isConstructor
        else -> false
    }
}


fun tryGetIdentifierElement(element: PsiElement?): PsiElement? {
    if (element == null) return null
    return when (element) {
        is PsiNameIdentifierOwner -> element
        is PsiIdentifier -> element
        else -> null
    }
}

fun IdeaPlatform.foldElement(event: AnActionEvent) {
    inUiThread {
        val identifier = tryGetIdentifierElement(event.elementAtCaret)
        // In some cases the caret element misses the mark so we need to use the event element
            ?: tryGetIdentifierElement(event.psiElement) ?: return@inUiThread

        CodeFoldingManager.getInstance(project)
            .updateFoldRegionsAsync(editor ?: return@inUiThread, true)?.run()

        val range = identifier.textRange

        // Manually fold because idea is a pos (this took like 3+ hours to figure out this code)
        editor.foldingModel.runBatchFoldingOperation {
            val foldRegion = editor.foldingModel.getFoldRegion(range.startOffset, range.endOffset)
                ?: return@runBatchFoldingOperation
            foldRegion.isExpanded = false
        }
    }

}

//TODO: validation:
// - Moving packages with protected/package private fields
class RenameIdeaAction : CloakAction() {
    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
        val element = event.psiElement ?: return false
        return canBeRenamed(element)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val element = event.psiElement ?: return
        val isTopLevelClass = element is PsiClass && !element.isInnerClass
        val nameBeforeRenames = element.asName()
        val project = event.project ?: return
        val editor = event.editor ?: return

        val platform = IdeaPlatform(project, editor)
        GlobalScope.launch {
            val rename = RenameAction.rename(platform, nameBeforeRenames, isTopLevelClass)

            if (rename is Ok<*>) {
                platform.foldElement(event)
            } else {
                println("Problem renaming: $rename")
            }
        }

    }

}

