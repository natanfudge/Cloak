package cloak.idea.gui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class CheckboxListDialog(project : Project, private val options: List<String>, title: String) : CloakDialog(project,title) {
    private var checkedList: MutableList<Boolean> = MutableList(options.size) { false }

    override fun createCenterPanel(): JComponent? = panel {
        options.forEachIndexed { i, option ->
            row {
                checkBox(option, setter = { checkedList[i] = it }, getter = { checkedList[i] })
            }
        }
    }

    init {
        init()
    }

    fun getChosenOptions() = options.filterIndexed { i, _ -> checkedList[i] }
}