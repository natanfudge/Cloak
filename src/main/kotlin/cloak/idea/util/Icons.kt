package cloak.idea.util

import com.intellij.openapi.ui.Messages

object Icons {
    object Message{
        val Error = Messages.getErrorIcon()
        val Warning = Messages.getWarningIcon()
        val Info = Messages.getInformationIcon()
        val Question = Messages.getQuestionIcon()
    }
}