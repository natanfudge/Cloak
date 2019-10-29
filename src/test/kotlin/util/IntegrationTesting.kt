package util

import cloak.idea.util.ProjectWrapper
import cloak.mapping.doesNotExist
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.map
import kotlinx.serialization.serializer
import java.io.File
import javax.swing.Icon

private val intermediaryMapFileCache = File("intermediaryNames.json")
private val intermediaryMapMemoryCache = mutableMapOf<String, String>()

private val StringMapSerializer = (String.serializer() to String.serializer()).map
private val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true))

fun saveIntermediaryMap() = intermediaryMapFileCache.writeText(json.stringify(StringMapSerializer,
    intermediaryMapMemoryCache))

class TestProjectWrapper(private val userInput: String?) : ProjectWrapper {
    private val messages = mutableListOf<String>()
    override  fun showInputDialog(
        message: String,
        title: String,
        icon: Icon,
        initialValue: String?,
        validator: ((String) -> String?)?
    ): String? = userInput

    override  fun showMessageDialog(message: String, title: String, icon: Icon) {
        messages.add(message)
    }

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

    override suspend fun <T> asyncWithProgressBar(title: String, action: suspend () -> T): T = action()
    override fun inUiThread(action: () -> Unit)  = action()
    override suspend fun <T> getFromUiThread(input: () -> T): T  = input()
}

fun testProject(input: String? = null) = TestProjectWrapper(input)