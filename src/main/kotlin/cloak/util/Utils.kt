package cloak.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import kotlinx.serialization.map
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.measureTimeMillis

fun <K, V> buildMap(init: MutableMap<K, V>.() -> Unit) = mutableMapOf<K, V>().apply(init)

val <K, V> Pair<KSerializer<K>, KSerializer<V>>.mutableMap: KSerializer<MutableMap<K, V>>
    get() = map as KSerializer<MutableMap<K, V>>

val <T> KSerializer<T>.mutableList: KSerializer<MutableList<T>>
    get() = list as KSerializer<MutableList<T>>


fun Path.exists(): Boolean = Files.exists(this)

fun Path.createDirectories(): Path {
    // symlink or existing regular file - Java SDK do this check, but with as `isDirectory(dir, LinkOption.NOFOLLOW_LINKS)`, i.e. links are not checked
    if (!Files.isDirectory(this)) {
        try {
            doCreateDirectories(toAbsolutePath())
        } catch (ignored: FileAlreadyExistsException) {
            // toAbsolutePath can return resolved path or file exists now
        }
    }
    return this
}

private fun doCreateDirectories(path: Path) {
    path.parent?.let {
        if (!Files.isDirectory(it)) {
            doCreateDirectories(it)
        }
    }
    Files.createDirectory(path)
}

fun <T> MutableList<T>.put(index: Int, item: T) {
    if (index < size) set(index, item) else add(index, item)
}

const val Profile = true
inline fun <T> profile(sectionName: String, code: () -> T): T {
    if (Profile) {
        var result: T? = null
        val time = measureTimeMillis {
            result = code()
        }
        println("$sectionName in $time millis")
        return result!!
    } else return code()
}

fun String.splitOn(index: Int) = Pair(substring(0, index), substring(index + 1))
fun String.splitOn(char: Char) = splitOn(indexOf(char))

val File.doesNotExist get() = !exists()

fun <K> Map<K, K>.getOrKey(key: K): K = getOrDefault(key, key)

val NormalJson = Json(JsonConfiguration.Stable)

operator fun <T> T.plus(list: List<T>): List<T> {
    val result = ArrayList<T>(list.size + 1)
    result.add(this)
    result.addAll(list)
    return result
}