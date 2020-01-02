package cloak.platform.saved

import cloak.format.mappings.MappingsFile
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
    private val branch: BranchInfo
        get() = platform.branchInfo.computeIfAbsent(platform.yarnRepo.currentBranch) {
            BranchInfo(platform.yarnRepo.defaultBranch)
        }

    fun getRenamedTo(name: Name): NewName? = branch.renames[name]


    fun acceptRenamedName(oldName: Name, newName: NewName) {
        branch.renames[oldName] = newName
    }

    fun createBranch(branchName: String, minecraftVersion: String) {
        if (platform.branchInfo[branchName] == null) platform.branchInfo[branchName] = BranchInfo(minecraftVersion)
    }

    val minecraftVersion: String get() = branch.minecraftVersion
    val all: Set<String> get() = platform.branchInfo.keys
    val renames get() = branch.renames

    fun deleteBranch(branchName: String) {
        platform.branchInfo.remove(branchName)
    }

    fun migrateInfo(oldBranch: String, newBranch: String) {
        platform.branchInfo[oldBranch]?.let { platform.branchInfo[newBranch] = it }
        platform.branchInfo.remove(oldBranch)
    }


    fun acceptJavadoc(forName: Name, javadoc: String) {
        branch.javadocs[forName] = javadoc
    }

    fun anythingWasAdded() = branch.let { it.renames.isNotEmpty() || it.javadocs.isNotEmpty() }

}

private typealias Branch = String

private val ExtendedPlatform.branchInfo: MutableMap<Branch, BranchInfo> by SavedState(
    mutableMapOf(), "BranchInfo",
    (Branch.serializer() to BranchInfo.serializer()).mutableMap
)

typealias RenameResult = DualResult<NewName, String>
typealias DualResult<V, E> = com.github.michaelbull.result.Result<V, E>
typealias ExplainedResult<V> = DualResult<V, String>


