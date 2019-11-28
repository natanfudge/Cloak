package cloak.platform.saved

import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.mutableList
import cloak.util.mutableMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.StringSerializer

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

//TODO: need some way to clean up old branches and their changes data
fun ExtendedPlatform.migrateYarnChangeList(oldBranch: String, newBranch: String) {
    yarnChanges[oldBranch]?.let { yarnChanges[newBranch] = it }
    yarnChanges.remove(oldBranch)
}


fun ExtendedPlatform.allChangesOfBranch(branch: String): List<Change> {
    return yarnChanges[branch] ?: listOf()
}
