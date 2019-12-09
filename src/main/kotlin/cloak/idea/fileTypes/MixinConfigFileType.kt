package cloak.idea.fileTypes

import cloak.idea.util.FabricIcons
import com.intellij.json.JsonFileType
import com.intellij.json.JsonLanguage

object MixinConfigFileType : JsonFileType(JsonLanguage.INSTANCE) {
    override fun getName() = "Mixin Config"
    override fun getDescription() = "Mixin Config json file"
    override fun getDefaultExtension() = ""
    override fun getIcon() = FabricIcons.Mixin
}