package cloak.platform.saved

import cloak.format.rename.Name
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.mutableMap
import kotlinx.serialization.Serializable

@Serializable
data class NewName(val newName: String, val newPackageName: String?) {
    override fun toString() = if (newPackageName != null) "$newPackageName/$newName" else newName
}

 val ExtendedPlatform.renamedNames : MutableMap<Name, NewName> by SavedState(mutableMapOf(),
    (Name.serializer() to NewName.serializer()).mutableMap)



