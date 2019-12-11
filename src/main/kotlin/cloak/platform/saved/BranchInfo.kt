package cloak.platform.saved

import cloak.format.rename.Name
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.mutableMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class NewName(val newName: String, val newPackageName: String?) {
    override fun toString() = if (newPackageName != null) "$newPackageName/$newName" else newName
}

@Serializable
private data class BranchInfo(
    val renames: Map<Name, NewName>,
    val javadocs: Map<Name, String>,
    val minecraftVersion: String
)

private typealias Branch = String

private val ExtendedPlatform.branchInfo: MutableMap<Branch, BranchInfo> by SavedState(
    mutableMapOf(), "BranchInfo",
    (Branch.serializer() to BranchInfo.serializer()).mutableMap
)

//TODO: make rename and add javadoc return rename results that then mutate. 
//data class RenameResult(val)