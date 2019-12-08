package cloak.idea.util

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader

object CommonIcons {
    val Error = Messages.getErrorIcon()
    val Warning = Messages.getWarningIcon()
    val Info = Messages.getInformationIcon()
    val Question = Messages.getQuestionIcon()
}

object FabricIcons {
    val Fabric = IconLoader.getIcon("/assets/fabric_logo_16.png")
    val Mixin = IconLoader.getIcon("/assets/mixin_config.svg")
}
