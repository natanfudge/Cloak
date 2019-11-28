package cloak.idea.util

import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.psi.PsiClass
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import java.awt.Component
import java.awt.Font
import javax.swing.JList
import javax.swing.ListCellRenderer

class NiceDropdownList : SimpleColoredComponent(),
    ListCellRenderer<Any?> {
    private val FONT: Font
    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        clear()
        if (value is PsiClass) {
            if (value.qualifiedName != null) {
                val attributes: SimpleTextAttributes = if (value.isDeprecated) {
                    SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, null)
                } else {
                    SimpleTextAttributes.REGULAR_ATTRIBUTES
                }
                append(value.qualifiedName!!, attributes)
            }
        } else {
            val qName = value as String?
            append(qName!!, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
        font = FONT
        if (isSelected) {
            background = list.selectionBackground
            foreground = list.selectionForeground
        } else {
            background = list.background
            foreground = list.foreground
        }
        return this
    }

    companion object {
        private val LOG =
            Logger.getInstance("#com.intellij.ide.util.FQNameCellRenderer")
    }

    init {
        val scheme = EditorColorsManager.getInstance().globalScheme
        FONT = Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
        isOpaque = true
    }
}