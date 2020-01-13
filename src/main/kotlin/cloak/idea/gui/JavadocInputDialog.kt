package cloak.idea.gui

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.impl.source.PsiCodeFragmentImpl
import com.intellij.psi.impl.source.tree.JavaElementType
import com.intellij.ui.EditorTextField
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import org.apache.log4j.Level
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.border.CompoundBorder
import kotlin.math.round

fun getIdeaJavadocInput(
    project: Project,
    title: String,
    existingJavadoc: String
): String? {
    val dialog = JavadocInputDialog(project, title, existingJavadoc)
    try {
        dialog.show()
    } catch (ignored: ClassCastException) {
    }

    if (!dialog.isOK) return null
    val text = dialog.getText()
    // Remove the /** * */ stuff that is needed for autocompletion
    val fixedText = text.removePrefix("/**\n")
        .removeSuffix("*/")
        .removeSuffix(" ")
        .removeSuffix("\n")
        .split("\n")
        .map { it.removePrefix(" *").removePrefix(" ") }

    return fixedText.joinToString("\n")
}


private fun <T> stfu(loggerName: String, whileDoing: () -> T): T {
    val logger = Logger.getInstance(loggerName)
    logger.setLevel(Level.OFF)
    try {
        return whileDoing()
    } finally {
        logger.setLevel(null)
    }
}

class JavadocInputDialog(
    private val project: Project,
    title: String,
    private val existingJavadoc: String
) :
    DialogWrapper(true) {

    private lateinit var field: EditorTextField

    override fun getPreferredFocusedComponent(): JComponent = field

    override fun createCenterPanel(): JComponent = panel {
        row {

            val javadocTextWithoutClosingTag = "/**\n" + existingJavadoc.split("\n").joinToString("\n") { " * $it" }

            val text = "$javadocTextWithoutClosingTag\n */"
            // IDEA will log angrily about this but this is the only way we get correct behavior so we must make it stfu.
            val codeFragment = stfu("#com.intellij.psi.impl.source.PsiFileImpl") {
                PsiCodeFragmentImpl(
                    project, JavaElementType.CLASS, true, "fragment.java",
                    text, PsiElementFactory.getInstance(project).createCodeBlock()
                )
            }
            val document = PsiDocumentManager.getInstance(project).getDocument(codeFragment)
                ?: error("Could not create fake document")
            field = object : EditorTextField(
                document, project, JavaFileType.INSTANCE, false, false
            ) {
                override fun createEditor(): EditorEx {
                    val editor = super.createEditor()
                    editor.setHorizontalScrollbarVisible(true)
                    editor.setVerticalScrollbarVisible(true)
                    editor.settings.isUseSoftWraps = true
                    editor.settings.lineCursorWidth = EditorUtil.getDefaultCaretWidth()
                    editor.colorsScheme.editorFontName = font.fontName
                    editor.colorsScheme.editorFontSize = round(font.size * 1.5).toInt()
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

            field.setCaretPosition(javadocTextWithoutClosingTag.length)
            field()
        }
    }

    fun getText(): String = field.text

    init {
        init()
        this.title = title
    }
}