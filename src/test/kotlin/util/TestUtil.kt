package util

import cloak.mapping.*
import java.io.File

    fun getTestResource(path: String): File = File("src/test/resources/$path")
 val TestYarnRepo = YarnRepo(File("yarn"))

inline fun <reified T> Errorable<T>.assertSucceeds(): T = when (this) {
    is StringSuccess -> this.value
    is StringError -> throw AssertionError("Expected success, but instead an error occurred: '${this.value}'")
}

//fun getOrCloneGit() : GitRepository {
//    YarnRepo.cloneIfMissing()
//    return GitRepository(YarnRepo.getRawGit())
//}