package cloak.util

import cloak.mapping.Errorable
import cloak.mapping.StringError
import cloak.mapping.StringSuccess
import cloak.git.YarnRepo
import java.io.File
import kotlin.test.assertEquals

fun getTestResource(path: String): File = File("src/test/resources/$path")
val TestYarnRepo = YarnRepo.at(File("yarn"))

inline fun <reified T> Errorable<T>.assertSucceeds(): T = when (this) {
    is StringSuccess -> this.value
    is StringError -> throw AssertionError("Expected success, but instead an error occurred: '${this.value}'")
}

fun assertEqualsIgnoreLineBreaks(expected: String, actual: String) = assertEquals(
    expected.replace("\r\n", "\n"), actual.replace("\r\n", "\n")
)


//fun getOrCloneGit() : GitRepository {
//    YarnRepo.cloneIfMissing()
//    return GitRepository(YarnRepo.getRawGit())
//}