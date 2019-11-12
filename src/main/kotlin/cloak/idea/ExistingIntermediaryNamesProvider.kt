package cloak.idea

import cloak.fabric.Intermediary.fetchExistingNames
import cloak.util.IdeaCache
import cloak.util.IdeaCacheGetter
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import kotlinx.serialization.Serializable

@Serializable
data class LatestIntermediaryNames(
    val classNames: Set<String> = HashSet(),
    /** Map from intermediary name to descriptor*/
    val methodNames: Map<String, String> = HashMap(),
    val fieldNames: Map<String, String> = HashMap(),
    var currentVersion: String? = null
)


@State(
    name = "ExistingIntermediaryNames",
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
)
class ExistingIntermediaryNamesProvider :
    IdeaCache<LatestIntermediaryNames>(LatestIntermediaryNames()) {

    //TODO: adjust to be able to fetch descriptors and stuff
    /**
     * Must be called whenever switching to a new version
     */
    fun update(newVersion: String) {
        println("Updating intermediary names")
        state = fetchExistingNames(newVersion)
        state.currentVersion = newVersion
    }

    fun getNames(version: String): LatestIntermediaryNames {
        if (state.currentVersion != version) update(version)
        return state
    }

    companion object : IdeaCacheGetter<ExistingIntermediaryNamesProvider>(ExistingIntermediaryNamesProvider::class.java)

}