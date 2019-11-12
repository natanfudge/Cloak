package cloak.util

import GitTests
import cloak.fabric.Intermediary
import cloak.idea.LatestIntermediaryNames
import cloak.idea.util.ProjectWrapper
import cloak.idea.util.RenameInput
import cloak.mapping.doesNotExist
import cloak.mapping.rename.cloakUser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.map
import kotlinx.serialization.serializer
import java.io.File
import java.nio.file.Files
import javax.swing.Icon

private val intermediaryMapFileCache = File("intermediaryNames.json")
private val intermediaryMapMemoryCache = mutableMapOf<String, String>()

private val StringMapSerializer = (String.serializer() to String.serializer()).map
private val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true))

fun saveIntermediaryMap() = intermediaryMapFileCache.writeText(
    json.stringify(
        StringMapSerializer,
        intermediaryMapMemoryCache
    )
)

//TODO: use test cache for others
private data class TestCache<T, K>(
    val fileCache: File, var memoryCache: T? = null,
    val serializer: KSerializer<T>, val getter: (K) -> T
) {
    fun get(key: K): T {
        if (memoryCache == null) {
            if (fileCache.doesNotExist) {
                Files.createDirectories(fileCache.parentFile.toPath())
                fileCache.writeText(json.stringify(serializer, getter(key)))
            }
            memoryCache = json.parse(serializer, fileCache.readText())
        }
        return memoryCache!!
    }
}

private val LatestIntermediaries: TestCache<LatestIntermediaryNames, String> = TestCache(
    fileCache = File("caches/latest_int.json"),
    getter = Intermediary::fetchExistingNames,
    serializer = LatestIntermediaryNames.serializer()
)



//TODO: generic "cache" implementation
class TestProjectWrapper(private val userInput: RenameInput?) : ProjectWrapper {
    private val messages = mutableListOf<String>()

    override fun requestRenameInput(newNameValidator: (String) -> String?): RenameInput? {
        return userInput
    }

    override fun showMessageDialog(message: String, title: String, icon: Icon) {
        messages.add(message)
    }

    override fun showErrorPopup(message: String, title: String) {

    }

    override fun getUserInput(
        title: String,
        message: String,
        allowEmptyString: Boolean,
        validator: ((String) -> String?)?
    ): String? {
        return null
    }


    override fun getGitUser() = GitTests.TestAuthor.cloakUser


    override val yarnRepoDir = File(System.getProperty("user.dir") + "/yarn")


    override fun getIntermediaryClassNames(): MutableMap<String, String> {
        if (intermediaryMapMemoryCache.isEmpty()) {
            if (intermediaryMapFileCache.doesNotExist) {
                intermediaryMapFileCache.writeText("{}")
            }
            intermediaryMapMemoryCache.putAll(json.parse(StringMapSerializer, intermediaryMapFileCache.readText()))
        }
        return intermediaryMapMemoryCache
    }

    override fun getLatestIntermediaryNames(version: String): LatestIntermediaryNames {
        return LatestIntermediaries.get(version)
    }

    override suspend fun <T> asyncWithText(title: String, action: suspend () -> T): T = action()
    override fun inUiThread(action: () -> Unit) = action()
    override suspend fun <T> getFromUiThread(input: () -> T): T = input()
}

