package cloak.util

import cloak.git.YarnRepo
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.File
import kotlin.test.assertEquals

fun getTestResource(path: String): File = File("src/test/resources/$path")
val TestYarnRepo = YarnRepo.at(File("caches/yarn"), TestPlatform(Pair("", null)))

inline fun <reified T,E> Result<T,E>.assertSucceeds(): T = when (this) {
    is Ok<T> -> this.value
    is Err<E> -> throw AssertionError("Expected success, but instead an error occurred: '$error'")
}

inline fun <reified T,E> Result<T,E>.assertFails(): E = when (this) {
    is Ok<T> -> throw AssertionError("Expected Error, but instead it was a success: '$value'")
    is Err<E> -> error
}

fun assertEqualsIgnoreLineBreaks(expected: String, actual: String) = assertEquals(
    expected.replace("\r\n", "\n"), actual.replace("\r\n", "\n")
)


//fun getOrCloneGit() : GitRepository {
//    YarnRepo.cloneIfMissing()
//    return GitRepository(YarnRepo.getRawGit())
//}