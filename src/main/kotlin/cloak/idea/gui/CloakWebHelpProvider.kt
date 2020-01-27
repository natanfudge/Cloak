package cloak.idea.gui

import com.intellij.openapi.help.WebHelpProvider

class CloakWebHelpProvider : WebHelpProvider() {
    override fun getHelpPageUrl(helpTopicId: String): String {
        return "https://github.com/natanfudge/Cloak/wiki/${helpTopicId.removePrefix("Cloak.")}"
    }

}