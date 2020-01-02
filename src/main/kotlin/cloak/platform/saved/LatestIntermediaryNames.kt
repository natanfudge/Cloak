package cloak.platform.saved

import cloak.fabric.Intermediary
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import kotlinx.serialization.Serializable

@Serializable
data class LatestIntermediaryNames(
    val classNames: Set<String> = HashSet(),
    /** Map from intermediary name to descriptor*/
    val methodNames: Map<String, String> = HashMap(),
    val fieldNames: Map<String, String> = HashMap(),
    var currentVersion: String? = null
)

private var ExtendedPlatform.latestIntermediaryNames: LatestIntermediaryNames by SavedState(
    LatestIntermediaryNames(),
    "LatestIntermediaries",
    LatestIntermediaryNames.serializer()
)

private fun ExtendedPlatform.getIntermediaryNamesOfVersion(version: String): LatestIntermediaryNames {
    if (latestIntermediaryNames.currentVersion != version) updateIntermediaryNamesToVersion(version)
    return latestIntermediaryNames
}

fun ExtendedPlatform.getIntermediaryNamesOfYarnVersion(): LatestIntermediaryNames  = getIntermediaryNamesOfVersion(branch.minecraftVersion)


fun ExtendedPlatform.updateIntermediaryNamesToVersion(newVersion: String) {
    println("Updating intermediary names to $newVersion")
    latestIntermediaryNames = Intermediary.fetchExistingNames(newVersion)
    latestIntermediaryNames.currentVersion = newVersion
}

