package cloak.idea


import RenamedNamesProvider
import cloak.idea.actions.isMinecraftPackageName
import cloak.idea.util.asNameOrNull
import cloak.idea.util.editor
import cloak.idea.util.psiFile
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.HighlightInfoType.HighlightInfoTypeImpl
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import java.awt.Font


private val HighlightStatusKey =
    TextAttributesKey.createTextAttributesKey("RENAME_HIGHLIGHTING")
private val HighlightInfoType: HighlightInfoType =
    HighlightInfoTypeImpl(HighlightSeverity.ERROR, HighlightStatusKey)

private val RenamedStyle = TextAttributes(JbColors.Green, null, null, null, Font.PLAIN)

class RenamedIdentifierHighlighterFactory(registrar: TextEditorHighlightingPassRegistrar) :
    TextEditorHighlightingPassFactory {
    init {
        registrar.registerTextEditorHighlightingPass(this, null, null, true, -1);
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass {
        return RenamedIdentifierHighlighter(file.project, file, editor);
    }

}


class RenamedIdentifierHighlighter(project: Project, private val file: PsiFile, editor: Editor) :
    TextEditorHighlightingPass(project, editor.document, true) {

    companion object {
        fun rerun(event: AnActionEvent) {
            ApplicationManager.getApplication().invokeLater {
                RenamedIdentifierHighlighter(
                    event.project ?: return@invokeLater,
                    event.psiFile ?: return@invokeLater,
                    event.editor ?: return@invokeLater
                ).doApplyInformationToEditor()
            }
        }
    }

    override fun doCollectInformation(progress: ProgressIndicator) {}
    override fun doApplyInformationToEditor() {
        if (file !is PsiJavaFile) return

        if (!RenamedNamesProvider.getInstance().anythingWasRenamed()) return
        if (!isMinecraftPackageName(file.packageName)) return

        val highlights = mutableListOf<HighlightInfo>()
        if (file is PsiCompiledElement) {
            CompiledVisitor(highlights).visitFile(file)
        } else {
            WalkingVisitor(highlights).visitFile(file)
        }


        UpdateHighlightersUtil.setHighlightersToEditor(
            myProject,
            myDocument!!,
            0,
            file.textLength,
            highlights,
            colorsScheme,
            id
        )
    }

}

private interface IVisitor {
    val highlights: MutableList<HighlightInfo>
    val renamedProvider: RenamedNamesProvider
}

private fun IVisitor.highlight(element: PsiElement, range: TextRange) {
    val rename = renamedProvider.getRenameOf(element.asNameOrNull() ?: return) ?: return
    val builder = HighlightInfo.newHighlightInfo(HighlightInfoType)
    builder.range(range)

    builder.textAttributes(RenamedStyle)
    builder.descriptionAndTooltip("Renamed to $rename")

    highlights.add(builder.createUnconditionally())
}

private fun IVisitor.highlight(element: PsiNameIdentifierOwner) {
    highlight(element, element.nameIdentifier?.textRange ?: return)
}

private class WalkingVisitor(override val highlights: MutableList<HighlightInfo>) :
    JavaRecursiveElementWalkingVisitor(), IVisitor {
    override val renamedProvider: RenamedNamesProvider = RenamedNamesProvider.getInstance()

    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        reference.resolve().let { if (it is PsiNameIdentifierOwner) highlight(it, reference.textRange ?: return) }
        super.visitReferenceElement(reference)
    }

    override fun visitClass(psiClass: PsiClass) {
        super.visitClass(psiClass)
        highlight(psiClass)
    }

    override fun visitField(field: PsiField) {
        super.visitField(field)
        highlight(field)
    }

    override fun visitMethod(method: PsiMethod) {
        super.visitMethod(method)
        highlight(method)
    }

    override fun visitParameter(parameter: PsiParameter) {
        super.visitParameter(parameter)
        highlight(parameter)
    }


}

private class CompiledVisitor(override val highlights: MutableList<HighlightInfo>) : JavaRecursiveElementVisitor(),
    IVisitor {
    override val renamedProvider: RenamedNamesProvider = RenamedNamesProvider.getInstance()

    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        reference.resolve().let { if (it is PsiNameIdentifierOwner) highlight(it, reference.textRange ?: return) }
        super.visitReferenceElement(reference)
    }

    override fun visitClass(psiClass: PsiClass) {
        super.visitClass(psiClass)
        highlight(psiClass)
    }

    override fun visitField(field: PsiField) {
        super.visitField(field)
        highlight(field)
    }

    override fun visitMethod(method: PsiMethod) {
        super.visitMethod(method)
        highlight(method)
    }

    override fun visitParameter(parameter: PsiParameter) {
        super.visitParameter(parameter)
        highlight(parameter)
    }


}