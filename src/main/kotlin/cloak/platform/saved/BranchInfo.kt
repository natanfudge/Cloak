package cloak.platform.saved

import cloak.format.rename.Name
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.mutableMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class NewName(val name: String, val packageName: String?, val explanation: String?) {
    override fun toString(): String {
        var string = name
        if (packageName != null) string = "$packageName/$string"
        if (explanation != null) string = "$string because $explanation"
        return string
    }
}

@Serializable
private data class BranchInfo(
    val minecraftVersion: String,
    val renames: MutableMap<Name, NewName> = mutableMapOf(),
    // Note: the actual content is not used right now
    val javadocs: MutableMap<Name, String> = mutableMapOf()
)


class BranchInfoApi(private val platform: ExtendedPlatform) {
    private suspend fun getBranch(): BranchInfo =
        platform.branchInfo.computeIfAbsent(platform.yarnRepo.getCurrentBranch()) {
            BranchInfo(platform.yarnRepo.defaultBranch)
        }

    private fun getBranchOrNull(): BranchInfo? {
        return platform.branchInfo.computeIfAbsent(platform.yarnRepo.getCurrentBranchOrNull() ?: return null) {
            BranchInfo(platform.yarnRepo.defaultBranch)
        }
    }

    fun getRenamedTo(name: Name): NewName? = getBranchOrNull()?.renames?.get(name)


    suspend fun acceptRenamedName(oldName: Name, newName: NewName) {
        getBranch().renames[oldName] = newName
    }

    fun createBranch(branchName: String, minecraftVersion: String) {
        if (platform.branchInfo[branchName] == null) platform.branchInfo[branchName] = BranchInfo(minecraftVersion)
    }

    suspend fun getMinecraftVersion(): String = getBranch().minecraftVersion
    val all: Set<String> get() = platform.branchInfo.keys
    suspend fun getRenames() = getBranch().renames

    fun deleteBranch(branchName: String) {
        platform.branchInfo.remove(branchName)
    }

    fun migrateInfo(oldBranch: String, newBranch: String) {
        platform.branchInfo[oldBranch]?.let { platform.branchInfo[newBranch] = it }
        platform.branchInfo.remove(oldBranch)
    }


    suspend fun acceptJavadoc(forName: Name, javadoc: String) {
        getBranch().javadocs[forName] = javadoc
    }

    fun anythingWasAdded() = platform.branchInfo.isNotEmpty()
            && getBranchOrNull()?.let { it.renames.isNotEmpty() || it.javadocs.isNotEmpty() } == true

}

private typealias Branch = String

private val ExtendedPlatform.branchInfo: MutableMap<Branch, BranchInfo> by SavedState(
    mutableMapOf(), "BranchInfo",
    (Branch.serializer() to BranchInfo.serializer()).mutableMap
)

fun ExtendedPlatform.cleanBranchInfo() {
    branchInfo.clear()
}

typealias RenameResult = DualResult<NewName, String>
typealias DualResult<V, E> = com.github.michaelbull.result.Result<V, E>
typealias ExplainedResult<V> = DualResult<V, String>


