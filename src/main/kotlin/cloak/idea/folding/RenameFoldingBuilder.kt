//package cloak.idea.folding
//
//import cloak.format.rename.shortName
//import cloak.idea.actions.isMinecraftPackageName
//import cloak.idea.platformImpl.IdeaPlatform
//import cloak.idea.util.asNameOrNull
//import cloak.idea.util.isInnerClass
//import cloak.idea.util.packageName
//import cloak.platform.ExtendedPlatform
//import cloak.platform.saved.anythingWasRenamed
//import cloak.platform.saved.getRenamedTo
//import cloak.platform.saved.nothingWasRenamed
//import com.intellij.lang.ASTNode
//import com.intellij.lang.folding.CustomFoldingBuilder
//import com.intellij.lang.folding.FoldingDescriptor
//import com.intellij.openapi.editor.Document
//import com.intellij.openapi.util.TextRange
//import com.intellij.psi.*
//
//
//private fun PsiPackageStatement.getTopLevelClass(): PsiClass? {
//    return (containingFile as? PsiJavaFile)?.classes?.find { !it.isInnerClass }
//}
//
//private fun PsiElement.getDefinitionElement(): PsiElement? = when (this) {
//    is PsiClass, is PsiField, is PsiParameter, is PsiMethod -> this
//    is PsiJavaCodeReferenceElement -> resolve()
//    else -> null
//}
//
//class RenameFoldingBuilder : CustomFoldingBuilder() {
//
//    override fun isDumbAware() = false
//
//    private fun PsiElement.isCollapsed(platform: ExtendedPlatform) =
//        asNameOrNull()?.let { platform.getRenamedTo(it) } != null
//
//
//    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
//        val psi = node.psi
//
//        if (!isMinecraftPackageName(psi.containingFile.packageName)) return false
//        val platform = IdeaPlatform(psi.project)
//
//        if (!platform.anythingWasRenamed()) return false
//        return if (node is PsiPackageStatement) {
//            node.getTopLevelClass()?.asNameOrNull()?.let { platform.getRenamedTo(it) }?.newPackageName != null
//        } else psi.getDefinitionElement()?.isCollapsed(platform) == true
//    }
//
//    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String {
//        val element = node.psi
//        val targetElement =
//            if (element is PsiPackageStatement) element.getTopLevelClass() else element.getDefinitionElement()
//        val name = targetElement?.asNameOrNull() ?: return node.text
//        val renamedTo = IdeaPlatform(element.project).getRenamedTo(name) ?: return node.text
//
//        return if (element is PsiPackageStatement) renamedTo.newPackageName?.replace('/', '.') ?: node.text
//        else renamedTo.newName
//    }
//
//    override fun buildLanguageFoldRegions(
//        descriptors: MutableList<FoldingDescriptor>,
//        root: PsiElement,
//        document: Document,
//        quick: Boolean
//    ) {
//        val file = root as? PsiJavaFile ?: return
//        if (!isMinecraftPackageName(file.packageName)) return
//        val platform = IdeaPlatform(file.project)
//
//        if (platform.nothingWasRenamed()) return
//
//        root.accept(WalkingVisitor(descriptors, platform))
//        val x = 2
//    }
//
//
//    private class WalkingVisitor(
//        val foldRegions: MutableList<FoldingDescriptor>,
//        val platform: ExtendedPlatform
//    ) : JavaRecursiveElementWalkingVisitor() {
//
//        override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
//            reference.resolve()
//                .let {
//                    if (it is PsiNameIdentifierOwner) foldElement(
//                        it,
//                        reference.textRange ?: return,
//                        astNode = reference.node
//                    )
//                }
//            super.visitReferenceElement(reference)
//        }
//
//        override fun visitClass(psiClass: PsiClass) {
//            super.visitClass(psiClass)
//            fold(psiClass)
//        }
//
//        override fun visitField(field: PsiField) {
//            super.visitField(field)
//            fold(field)
//        }
//
//        override fun visitMethod(method: PsiMethod) {
//            super.visitMethod(method)
//            fold(method)
//        }
//
//        override fun visitParameter(parameter: PsiParameter) {
//            super.visitParameter(parameter)
//            fold(parameter)
//        }
//
//
//        override fun visitPackageStatement(statement: PsiPackageStatement) {
//            statement.getTopLevelClass()?.let { element ->
//                val oldName = element.asNameOrNull() ?: return
//                val newName = platform.getRenamedTo(oldName) ?: return
//                if (newName.newPackageName?.replace('/', '.') == statement.packageName) return
//
//                foldRegions.add(FoldingDescriptor(statement.node ?: return, statement.packageReference.textRange))
//            }
//        }
//
//        private fun fold(element: PsiNameIdentifierOwner) {
//            foldElement(element, element.nameIdentifier?.textRange ?: return)
//        }
//
//        private fun foldElement(element: PsiElement, range: TextRange, astNode: ASTNode? = element.node) {
//            val oldName = element.asNameOrNull() ?: return
//            platform.getRenamedTo(oldName) ?: return
//
//            // Sometimes the element captures too much so we only take the part that contains the name itself
//            val actualRange = TextRange(range.endOffset - oldName.shortName.length, range.endOffset)
//
//            foldRegions.add(FoldingDescriptor(astNode ?: return, actualRange, null, mutableSetOf(), true))
//
//        }
//
//        private fun foldJavadoc(element: PsiNameIdentifierOwner) {
//            platform.getRenamedTo(element.asNameOrNull() ?: return) ?: return
//
//
//        }
//
//    }
//
//}