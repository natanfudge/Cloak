package cloak.platform.saved

import cloak.format.rename.Name
import cloak.git.YarnRepo
import cloak.git.currentBranch
import cloak.git.currentBranchOrNull
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.mutableMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.StringSerializer

@Serializable
data class NewName(val newName: String, val newPackageName: String?) {
    override fun toString() = if (newPackageName != null) "$newPackageName/$newName" else newName
}

// Mapped per branch
private val ExtendedPlatform.renamedNames: MutableMap<String, MutableMap<Name, NewName>> by SavedState(
    mutableMapOf(),
    "RenamedNames",
    (StringSerializer to (Name.serializer() to NewName.serializer()).mutableMap).mutableMap
)

fun ExtendedPlatform.thisIsAMethodForTestToNotLongerRenamesNamesBetweenTestsDontUseItThanks() = renamedNames.clear()

fun ExtendedPlatform.getRenamedTo(name: Name): NewName? = renamedNames[currentBranch]?.get(name)
fun ExtendedPlatform.setRenamedTo(name: Name, newName: NewName) {
    if (renamedNames[currentBranch] == null) renamedNames[currentBranch] = mutableMapOf()
    renamedNames[currentBranch]!![name] = newName
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