package cloak.mapping

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import kotlin.system.measureTimeMillis


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