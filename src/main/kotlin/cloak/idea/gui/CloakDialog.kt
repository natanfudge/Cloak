package cloak.idea.gui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper

abstract class CloakDialog(project: Project?, _title: String, private val helpId: String? = null) :
    DialogWrapper(project, true) {
    init {
        this.title = _title
    }

    override fun getHelpId(): String? {
        return "Cloak.$helpId"
    }
}

