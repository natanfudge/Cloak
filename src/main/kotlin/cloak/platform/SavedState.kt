package cloak.platform

import cloak.util.createDirectories
import cloak.util.exists
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KProperty

private const val SavedDirectory = "saved"

abstract class PersistentSaver {

    abstract fun registerProjectCloseCallback(callback: () -> Unit)

    private val needSaving: MutableList<SavedState<*>> = mutableListOf()
    fun markDirty(state: SavedState<*>) {
        if (needSaving.isEmpty()) rememberToSave()
        needSaving.add(state)
    }

    private fun rememberToSave() {
        registerProjectCloseCallback {
            for (state in needSaving) {
                val storagePath = state.storagePath
                storagePath.parent.createDirectories()
                println("Saving $storagePath")
                Files.write(
                    storagePath,
                    // We know that each SavedState instance as the correct serializer for the memory cache,
                    // but kotlin doesn't know that, so we need to cast
                    Cbor.plain.dump(state.serializer as KSerializer<Any?>, state.memoryCache)
                )
            }
        }
    }

}

/**
 * Make sure the property name is unique
 */
class SavedState<T : Any?>(defaultValue: T, private val key : String, internal val serializer: KSerializer<T>) {
    internal var memoryCache: T = defaultValue
    internal lateinit var storagePath: Path

    operator fun getValue(platform: ExtendedPlatform, property: KProperty<*>): T {
        if (!::storagePath.isInitialized) {
            storagePath = Paths.get(platform.storageDirectory.toString(), SavedDirectory, "$key.cbor")
            platform.persistentSaver.markDirty(this)

            if (!storagePath.exists()) {
                // Wasn't saved yet, will be saved later
                return memoryCache
            }

            memoryCache = Cbor.plain.load(serializer, Files.readAllBytes(storagePath))
        }

        return memoryCache
    }

    operator fun setValue(thisRef: ExtendedPlatform, property: KProperty<*>, value: T) {
        memoryCache = value
    }
}

