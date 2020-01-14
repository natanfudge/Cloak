package cloak.idea.gui

import com.intellij.openapi.ui.DialogWrapper

abstract class CloakDialog(_title: String) : DialogWrapper(true) {
    init {
        this.title = _title
    }
}

