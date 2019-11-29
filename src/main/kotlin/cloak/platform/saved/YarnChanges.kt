package cloak.platform.saved

import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.mutableList
import cloak.util.mutableMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.StringSerializer

//TODO: have a big "branch" storage that has all branch-specific data. It should also include the target minecraft version info.
// (instead of MCVERSION.txt)

@Serializable
data class Change(val oldName: String, val newName: String, val explanation: String?)

// Mapped per branch
private val ExtendedPlatform.yarnChanges: MutableMap<String, MutableList<Change>> by SavedState(
    mutableMapOf(),
    "YarnChangelog",
    serializer = (StringSerializer to Change.serializer().mutableList).mutableMap
)

fun ExtendedPlatform.appendYarnChange(branch: String, change: Change) {
    val changesInBranch = yarnChanges[branch]
    if (changesInBranch == null) yarnChanges[branch] = mutableListOf()
    yarnChanges[branch]!!.add(change)
}

fun ExtendedPlatform.deleteYarnChangesOfBranch(branch: String) {
    yarnChanges.remove(branch)
}

fun ExtendedPlatform.migrateYarnChangeList(oldBranch: String, newBranch: String) {
    yarnChanges[oldBranch]?.let { yarnChanges[newBranch] = it }
    yarnChanges.remove(oldBranch)
}


fun ExtendedPlatform.allChangesOfBranch(branch: String): List<Change> {
    return yarnChanges[branch] ?: listOf()
}
