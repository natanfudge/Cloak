package cloak.idea.util

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.JavaCodeFragmentFactory
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.ui.EditorTextField
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.border.CompoundBorder

class JavadocInputDialog(private val project: Project, title: String, private val targetElement: PsiElement) :
    DialogWrapper(true) {

    override fun createCenterPanel(): JComponent = panel {
        row {
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val emptyComment = elementFactory.createDocCommentFromText("/** */")
            emptyComment.addAfter(targetElement,emptyComment)
//            emptyComment.
//            targetElement.addbe
//            .addBefore(emptyComment, null)

            val codeFragment = JavaCodeFragmentFactory.getInstance(project)
                .createExpressionCodeFragment("", emptyComment, null, true)
            val document = PsiDocumentManager.getInstance(project).getDocument(codeFragment)
                ?: error("Could not create fake document")
            val field : EditorTextField = object : EditorTextField(
                document, project, JavaFileType.INSTANCE, false, false
            ) {
                override fun createEditor(): EditorEx {
                    val editor = super.createEditor()
                    editor.setHorizontalScrollbarVisible(true)
                    editor.setVerticalScrollbarVisible(true)
                    editor.settings.isUseSoftWraps = true
                    editor.settings.lineCursorWidth = EditorUtil.getDefaultCaretWidth()
                    editor.colorsScheme.editorFontName = font.fontName
                    editor.colorsScheme.editorFontSize = font.size
                    editor.contentComponent.border = CompoundBorder(
                        editor.contentComponent.border,
                        JBUI.Borders.emptyLeft(2)
                    )
                    editor.contextMenuGroupId = "XDebugger.Evaluate.Code.Fragment.Editor.Popup"

                    return editor
                }

                override fun getData(dataId: String): Any? {
                    if (LangDataKeys.CONTEXT_LANGUAGES.name == dataId) {
                        return arrayOf<Language>(JavaLanguage.INSTANCE)
                    } else if (CommonDataKeys.PSI_FILE.name == dataId) {
                        return PsiDocumentManager.getInstance(project).getPsiFile(document)
                    }
                    return super.getData(dataId)
                }
            }
            WindowManager.getInstance().getFrame(project)?.size?.let {
                field.preferredSize = Dimension(it.width / 2, it.height / 2)
            }
            field()
        }
    }

//
//    private fun decorate(component: JComponent?): JComponent {
//        val panel = JBUI.Panels.simplePanel()
//        val factoryPanel: JPanel = JBUI.Panels.simplePanel()
//        panel.add(factoryPanel, BorderLayout.WEST)
//        panel.addToCenter(component!!)
//        val adLabel = JBLabel("foo", SwingConstants.RIGHT)
//        adLabel.componentStyle = UIUtil.ComponentStyle.SMALL
//        adLabel.fontColor = UIUtil.FontColor.BRIGHTER
//        panel.addToBottom(adLabel)
//
//        return panel
//    }

    init {
        init()
        this.title = title
    }
}