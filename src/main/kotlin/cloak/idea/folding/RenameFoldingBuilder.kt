package cloak.idea.folding

import cloak.format.rename.shortName
import cloak.idea.actions.isPartOfMinecraft
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.asNameOrNull
import cloak.idea.util.isInnerClass
import cloak.idea.util.packageName
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

data class Fold(val range: TextRange, val text: String)

interface Folder {
    fun fold(element: PsiElement): Fold?
    fun shouldFoldFile(file: PsiFile): Boolean
}

fun PsiElement.getTopLevelClass(): PsiClass? {
    return (containingFile as? PsiJavaFile)?.classes?.find { !it.isInnerClass }
}

private fun PsiElement.getDefinitionElement(): PsiNameIdentifierOwner? = when (this) {
    is PsiClass, is PsiField, is PsiParameter, is PsiMethod -> this
    is PsiJavaCodeReferenceElement -> resolve()
    else -> null
} as? PsiNameIdentifierOwner

abstract class EasyFoldingBuilder(private val folder: Folder) : CustomFoldingBuilder() {
    override fun isDumbAware(): Boolean = false

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean = true
    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String =
        folder.fold(node.psi)?.text ?: node.text

    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
        val file = root as? PsiJavaFile ?: return

        if (folder.shouldFoldFile(file)) root.accept(FoldingVisitor(descriptors, folder))
    }

    private class FoldingVisitor(
        val foldRegions: MutableList<FoldingDescriptor>,
        val folder: Folder
    ) : JavaRecursiveElementWalkingVisitor() {

        override fun visitElement(element: PsiElement) {
            val fold = folder.fold(element)
            if (fold != null) {
                val astNode = element.node
                if (astNode != null) {
                    foldRegions.add(FoldingDescriptor(astNode, fold.range, null, mutableSetOf(), true))
                }
            }
            super.visitElement(element)
        }
    }
}

private object RenameFolder : Folder {
    private fun PsiElement.getRange(expectedLength: Int): TextRange? {
        if (this is PsiPackageStatement) return packageReference.textRange
        val baseRange = (if (this is PsiNameIdentifierOwner) nameIdentifier?.textRange else textRange) ?: return null

        // Sometimes the element captures too much so we only take the part that contains the name itself
        return TextRange(baseRange.endOffset - expectedLength, baseRange.endOffset)
    }

    override fun fold(element: PsiElement): Fold? {
        when (element) {
            is PsiPackageStatement, is PsiClass, is PsiMethod, is PsiField, is PsiParameter, is PsiJavaCodeReferenceElement -> {

                val definition =
                    if (element is PsiPackageStatement) element.getTopLevelClass() else element.getDefinitionElement()

                val name = definition?.asNameOrNull() ?: return null

                val renamedTo = IdeaPlatform(element.project).branch.getRenamedTo(name) ?: return null
                val range = element.getRange(expectedLength = name.shortName.length) ?: return null

                val foldText =
                    if (element is PsiPackageStatement) renamedTo.packageName?.replace('/', '.') ?: return null
                    else renamedTo.name

                return Fold(range = range, text = foldText)
            }
            else -> return null
        }

    }

    override fun shouldFoldFile(file: PsiFile): Boolean {
        if (!isPartOfMinecraft(file)) return false
        return IdeaPlatform(file.project).branch.anythingWasAdded()
    }

}

class RenameFoldingBuilder : EasyFoldingBuilder(RenameFolder)
