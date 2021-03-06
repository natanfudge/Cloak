package cloak.idea


import cloak.format.rename.shortName
import cloak.idea.actions.isPartOfMinecraft
import cloak.idea.platformImpl.IdeaPlatform
import cloak.idea.util.asNameOrNull
import cloak.platform.ActiveMappings
import cloak.platform.ExtendedPlatform
import cloak.util.buildList
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.HighlightInfoType.HighlightInfoTypeImpl
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.newvfs.impl.FsRoot
import com.intellij.psi.*
import com.intellij.ui.JBColor
import java.awt.Font
import java.io.File
import kotlin.test.assertNotNull


private val HighlightStatusKey =
    TextAttributesKey.createTextAttributesKey("RENAME_HIGHLIGHTING")
private val HighlightInfoType: HighlightInfoType =
    HighlightInfoTypeImpl(HighlightSeverity.ERROR, HighlightStatusKey)

private fun highlighting(red: Int, green: Int, blue: Int) = highlighting(staticColor(red, green, blue))

private fun highlighting(color: JBColor) = TextAttributes(
    null, null,
    color, EffectType.BOLD_DOTTED_LINE, Font.PLAIN
)

private val NamedWithDocStyle = highlighting(JbColors.Green)
private val NamedWithoutDocStyle = highlighting(167, 218, 29)
private val UnnamedStyle = highlighting(JbColors.Red)
private val NamedInLaterVersionStyle = highlighting(80, 80, 240)
private val AutoGeneratedStyle = highlighting(128, 128, 128)
private val ExternalSymbolStyle = highlighting(78, 78, 78)


class NamingProgressHighlighterFactory : TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass {
        return NamingProgressHighlighter(file.project, file, editor)
    }

    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        registrar.registerTextEditorHighlightingPass(this, null, null, true, -1)
    }
}

enum class NamingProgress {
    Unnamed, NamedWithoutJavadoc, NamedWithJavadoc, AutoGenerated, NamedInLaterVersion, NotMinecraft
}


class NamingProgressHighlighter(
    project: Project,
    private val file: PsiFile,
    editor: Editor
) :
    TextEditorHighlightingPass(project, editor.document, true) {
    private val platform = IdeaPlatform(project, editor)

    override fun doCollectInformation(progress: ProgressIndicator) {}
    override fun doApplyInformationToEditor() {
        if (file !is PsiJavaFile) return

        if (!isPartOfMinecraft(file)) return

        val highlights = if (!ActiveMappings.areActive()) listOf<HighlightInfo>()
        else buildList {
            WalkingVisitor(this, platform, file.getContainingJar()).visitFile(file)
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

    private fun PsiFile.getContainingJar(): File {
        var currentFile = this.originalFile.virtualFile
        while (currentFile !is FsRoot) {
            assertNotNull(currentFile) {
                "The minecraft classes are always contained in a jar, " +
                        "which means it will reach a FsRoot before reaching null"
            }
            currentFile = currentFile.parent
        }
        return File(currentFile.path.removeSuffix("!/"))
    }

}


private class WalkingVisitor(
    val highlights: MutableList<HighlightInfo>,
    val platform: ExtendedPlatform,
    val mcJar: File
) : JavaRecursiveElementWalkingVisitor() {

    private fun highlight(element: PsiElement, range: TextRange) {
        val name = element.asNameOrNull() ?: return

        val progress = if (isPartOfMinecraft(element)) {
            ActiveMappings.getProgressOf(name, platform, mcJar)
        } else NamingProgress.NotMinecraft

        val builder = HighlightInfo.newHighlightInfo(HighlightInfoType)

        // Sometimes the element captures too much so we only take the part that contains the name itself
        val actualRange = TextRange(range.endOffset - name.shortName.length, range.endOffset)
        builder.range(actualRange)

        builder.textAttributes(
            when (progress) {
                NamingProgress.Unnamed -> UnnamedStyle
                NamingProgress.NamedWithoutJavadoc -> NamedWithoutDocStyle
                NamingProgress.NamedWithJavadoc -> NamedWithDocStyle
                NamingProgress.NamedInLaterVersion -> NamedInLaterVersionStyle
                NamingProgress.AutoGenerated -> AutoGeneratedStyle
                NamingProgress.NotMinecraft -> ExternalSymbolStyle
            }
        )

        val text = when (progress) {
            NamingProgress.Unnamed -> "Not named yet"
            NamingProgress.NamedWithoutJavadoc -> "Named without documentation"
            NamingProgress.NamedWithJavadoc -> "Named with documentation"
            NamingProgress.NamedInLaterVersion -> "Renamed in a later yarn version"
            NamingProgress.AutoGenerated -> "Auto-generated name"
            NamingProgress.NotMinecraft -> "Not part of Minecraft"
        }

        builder.descriptionAndTooltip(text)

        highlights.add(builder.createUnconditionally())
    }

    private fun highlight(element: PsiNameIdentifierOwner) {
        highlight(element, element.nameIdentifier?.textRange ?: return)
    }


//    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
//        reference.resolve()
//            .let {
//                if (it is PsiNameIdentifierOwner && it.getTopLevelClass()?.qualifiedName in shownClasses){
//                    highlight(it, reference.textRange ?: return)
//                }
//            }
//        super.visitReferenceElement(reference)
//    }

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
        if (!method.isConstructor) highlight(method)
    }

    override fun visitParameter(parameter: PsiParameter) {
        super.visitParameter(parameter)
        highlight(parameter)
    }


}
