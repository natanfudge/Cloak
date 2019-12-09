package cloak.idea.fileTypes

import cloak.idea.util.CommonIcons
import cloak.idea.util.FabricIcons
import com.intellij.json.JsonFileType
import com.intellij.json.JsonLanguage

object FabricModConfigFileType : JsonFileType(JsonLanguage.INSTANCE) {
    override fun getName() = "Fabric Mod Config"
    override fun getDescription() = "Fabric Mod Config json file"
    override fun getDefaultExtension() = ""
    override fun getIcon() = FabricIcons.Fabric
}