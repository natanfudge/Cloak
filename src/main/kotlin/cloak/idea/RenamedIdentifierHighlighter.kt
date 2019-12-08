//package cloak.idea
//
//
//import cloak.format.rename.getOwnName
//import cloak.format.rename.shortName
//import cloak.idea.actions.isMinecraftPackageName
//import cloak.idea.platformImpl.IdeaPlatform
//import cloak.idea.util.asNameOrNull
//import cloak.idea.util.isInnerClass
//import cloak.idea.util.packageName
//import cloak.platform.ExtendedPlatform
//import cloak.platform.saved.getRenamedTo
//import cloak.platform.saved.nothingWasRenamed
//import cloak.util.buildList
//import com.intellij.codeHighlighting.TextEditorHighlightingPass
//import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
//import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
//import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
//import com.intellij.codeInsight.daemon.impl.HighlightInfo
//import com.intellij.codeInsight.daemon.impl.HighlightInfoType
//import com.intellij.codeInsight.daemon.impl.HighlightInfoType.HighlightInfoTypeImpl
//import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
//import com.intellij.lang.annotation.HighlightSeverity
//import com.intellij.openapi.components.ServiceManager
//import com.intellij.openapi.editor.Editor
//import com.intellij.openapi.editor.colors.TextAttributesKey
//import com.intellij.openapi.editor.markup.TextAttributes
//import com.intellij.openapi.progress.ProgressIndicator
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.util.TextRange
//import com.intellij.psi.*
//import java.awt.Font
//
//
//private val HighlightStatusKey =
//    TextAttributesKey.createTextAttributesKey("RENAME_HIGHLIGHTING")
//private val HighlightInfoType: HighlightInfoType =
//    HighlightInfoTypeImpl(HighlightSeverity.ERROR, HighlightStatusKey)
//
//private val RenamedStyle = TextAttributes(JbColors.Green, null, null, null, Font.PLAIN)
//
//
////// Thank you IDEA
////class RenamedIdentifierHighlighterFactoryRegistrar :  {
////    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
////        RenamedIdentifierHighlighterFactory(registrar)
////    }
////
////}
//
//class RenamedIdentifierHighlighterFactory : TextEditorHighlightingPassFactory,TextEditorHighlightingPassFactoryRegistrar {
//
//    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass {
//        return RenamedIdentifierHighlighter(file.project, file, editor)
//    }
//
//    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
//        registrar.registerTextEditorHighlightingPass(this, null, null, true, -1)
//    }
//}
//
//
//class RenamedIdentifierHighlighter(
//    project: Project,
//    private val file: PsiFile,
//    editor: Editor
//) :
//    TextEditorHighlightingPass(project, editor.document, true) {
//    private val platform = IdeaPlatform(project, editor)
//
//    override fun doCollectInformation(progress: ProgressIndicator) {}
//    override fun doApplyInformationToEditor() {
//        if (file !is PsiJavaFile) return
//
//        if (!isMinecraftPackageName(file.packageName)) return
//
//        val highlights = if (platform.nothingWasRenamed()) listOf<HighlightInfo>()
//        else buildList {
//            if (file is PsiCompiledElement) {
//                CompiledVisitor(this, platform).visitFile(file)
//            } else {
//                WalkingVisitor(this, platform).visitFile(file)
//            }
//        }
//
//        UpdateHighlightersUtil.setHighlightersToEditor(
//            myProject,
//            myDocument!!,
//            0,
//            file.textLength,
//            highlights,
//            colorsScheme,
//            id
//        )
//    }
//
//}
//
//private interface IVisitor {
//    val highlights: MutableList<HighlightInfo>
//    val platform: ExtendedPlatform
//}
//
//private fun IVisitor.highlight(element: PsiElement, range: TextRange) {
//    val oldName = element.asNameOrNull() ?: return
//    val rename = platform.getRenamedTo(oldName) ?: return
//    val builder = HighlightInfo.newHighlightInfo(HighlightInfoType)
//
//    // Sometimes the element captures too much so we only take the part that contains the name itself
//    val actualRange = TextRange(range.endOffset - oldName.shortName.length, range.endOffset)
//    builder.range(actualRange)
//
//    builder.textAttributes(RenamedStyle)
//    // Only show the new class name if the package name didn't change
//    val newName = if (element is PsiClass && !element.isInnerClass && element.packageName.replace('.','/') == rename.newPackageName) {
//        rename.newName
//    } else rename.toString()
//    builder.descriptionAndTooltip("Renamed to $newName")
//
//    highlights.add(builder.createUnconditionally())
//}
//
//private fun IVisitor.highlight(element: PsiNameIdentifierOwner) {
//    highlight(element, element.nameIdentifier?.textRange ?: return)
//}
//
//private class WalkingVisitor(
//    override val highlights: MutableList<HighlightInfo>,
//    override val platform: ExtendedPlatform
//) : JavaRecursiveElementWalkingVisitor(), IVisitor {
//
//    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
//        reference.resolve()
//            .let {
//                if (it is PsiNameIdentifierOwner) highlight(it, reference.textRange ?: return)
//            }
//        super.visitReferenceElement(reference)
//    }
//
//    override fun visitClass(psiClass: PsiClass) {
//        super.visitClass(psiClass)
//        highlight(psiClass)
//    }
//
//    override fun visitField(field: PsiField) {
//        super.visitField(field)
//        highlight(field)
//    }
//
//    override fun visitMethod(method: PsiMethod) {
//        super.visitMethod(method)
//        highlight(method)
//    }
//
//    override fun visitParameter(parameter: PsiParameter) {
//        super.visitParameter(parameter)
//        highlight(parameter)
//    }
//
//
//}
//
//private class CompiledVisitor(
//    override val highlights: MutableList<HighlightInfo>,
//    override val platform: ExtendedPlatform
//) : JavaRecursiveElementVisitor(), IVisitor {
//
//    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
//        reference.resolve()
//            .let { if (it is PsiNameIdentifierOwner) highlight(it, reference.textRange ?: return) }
//        super.visitReferenceElement(reference)
//    }
//
//    override fun visitClass(psiClass: PsiClass) {
//        super.visitClass(psiClass)
//        highlight(psiClass)
//    }
//
//    override fun visitField(field: PsiField) {
//        super.visitField(field)
//        highlight(field)
//    }
//
//    override fun visitMethod(method: PsiMethod) {
//        super.visitMethod(method)
//        highlight(method)
//    }
//
//    override fun visitParameter(parameter: PsiParameter) {
//        super.visitParameter(parameter)
//        highlight(parameter)
//    }
//
//
//}