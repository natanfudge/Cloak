package cloak.platform.saved

import cloak.format.rename.Name
import cloak.git.currentBranch
import cloak.git.currentBranchOrNull
import cloak.platform.DebugDump
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.DebugJson
import cloak.util.mutableMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.StringSerializer

@Serializable
data class NewName(val newName: String, val newPackageName: String?) {
    override fun toString() = if (newPackageName != null) "$newPackageName/$newName" else newName
}

private val serializer = (StringSerializer to (Name.serializer() to NewName.serializer()).mutableMap).mutableMap
// Mapped per branch
private val ExtendedPlatform.renamedNames: MutableMap<String, MutableMap<Name, NewName>> by SavedState(
    mutableMapOf(), "RenamedNames", serializer
)

fun DebugDump.renamedNamesDump() = DebugJson.stringify(serializer, platform.renamedNames)

fun ExtendedPlatform.thisIsAMethodForTestToNotLongerRenamesNamesBetweenTestsDontUseItThanks() = renamedNames.clear()

fun ExtendedPlatform.getRenamedTo(name: Name): NewName? = renamedNames[currentBranch]?.get(name)
fun ExtendedPlatform.setRenamedTo(name: Name, newName: NewName) {
    if (renamedNames[currentBranch] == null) renamedNames[currentBranch] = mutableMapOf()
    renamedNames[currentBranch]!![name] = newName
}

fun ExtendedPlatform.deleteRenamesNamesOfBranch(branch: String) {
    renamedNames.remove(branch)
}

fun ExtendedPlatform.migrateRenamedNamesBranch(oldBranch: String, newBranch: String) {
    renamedNames[oldBranch]?.let { renamedNames[newBranch] = it }
    renamedNames.remove(oldBranch)
}

fun ExtendedPlatform.nothingWasRenamed(): Boolean {
    return currentBranchOrNull?.let { renamedNames[it]?.isEmpty() } != false
}

fun ExtendedPlatform.anythingWasRenamed() = !nothingWasRenamed()

val ExtendedPlatform.allBranches get() = renamedNames.keys
