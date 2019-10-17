package cloak.mapping

import cloak.mapping.*
import cloak.mapping.ParameterName
import cloak.mapping.mappings.*


//fun MessageContext.acceptRaw(keyWord: KeyWord, message: String) {
//    val sentence = message.split(" ").filter { it.isNotBlank() }.map { it.trim() }.toMutableList()
//    // Remove the command prefix
//    sentence.removeAt(0)
//
//    UserInput.checkForError(keyWord, sentence).let { return reply(it) }
//
//    val oldName = sentence[0]
//    val newName = when (keyWord) {
//        KeyWord.Name -> sentence[1].removeSuffix(":")
//        KeyWord.Rename -> sentence[2]
//    }
//
//    val explanation = when (keyWord) {
//        KeyWord.Rename -> {
//            if (sentence.size < 5) null
//            else sentence[4]
//        }
//        KeyWord.Name -> {
//            if (sentence.size < 3) null
//            else sentence[2]
//        }
//    }
//
//    when (val result = parseRename(keyWord, oldName, newName, explanation)) {
//        is StringSuccess -> tryRename(result.value, oldName)
//        is StringError -> return reply(result.value)
//    }
//
//}
//
//
//fun Mapping.type(): String = when (this) {
//    is ClassMapping -> "class"
//    is MethodMapping -> "method"
//    is FieldMapping -> "field"
//    is ParameterMapping -> "parameter"
//}
//
//fun Name<*>.type(): String = when (this) {
//    is ClassName -> "class"
//    is MethodName -> "method"
//    is FieldName -> "field"
//    is ParameterName -> "parameter"
//}
//
//fun Mapping.typePlural(): String = when (this) {
//    is ClassMapping -> "classes"
//    is MethodMapping -> "methods"
//    is FieldMapping -> "fields"
//    is ParameterMapping -> "parameters"
//}
//
//private fun <M : Mapping> MessageContext.tryRename(rename: Rename<M>, oldNameInputString: String) {
////    val repo = YarnRepo.getRawGit()
//    profile("Switched to branch $branchNameOfSender") {
//        repo.switchToBranch(branchNameOfSender)
//    }
//
//
//    val matchingMappingsFiles = YarnRepo.walkMappingsDirectory()
//        .mapNotNull { rename.findRenameTarget(it) }
//        .toList()
//
//    when {
//        matchingMappingsFiles.isEmpty() -> {
//            return profile("replied") {
//                val type = rename.originalName.name.type()
//                reply(
//                    if (rename.byObfuscated) "No intermediary $type name '$oldNameInputString' or the $type has already been named."
//                    else "No $type named '$oldNameInputString'."
//                )
//            }
//        }
//        matchingMappingsFiles.size > 1 -> {
//            val typePlural = matchingMappingsFiles[0].typePlural()
//            val type = matchingMappingsFiles[0].type()
//            val options = matchingMappingsFiles.joinToString("\n") { it.humanReadableName(rename.byObfuscated) }
//            return reply(
//                "There are multiple $typePlural with this name: \n$options\n" +
//                        "Prefix the **original** $type name with its enclosing package name followed by a '/'."
//            )
//        }
//        else -> {
//            rename(rename, matchingMappingsFiles[0])
//        }
//    }
//
//}
//
//val Mapping.filePath get() = (root.deobfuscatedName ?: root.obfuscatedName) + ".mapping"
//
//private fun <M : Mapping> MessageContext.rename(rename: Rename<M>, renameTarget: M) {
//    val oldPath = renameTarget.filePath
//    val oldName = renameTarget.humanReadableName(false)
//    val result = rename.rename(renameTarget)
//    if (result is StringError) {
//        reply(result.value)
//    }
//    val newPath = renameTarget.filePath
//    val newName = renameTarget.humanReadableName(false)
//
//    reply("Renamed $oldName to $newName")
//
//    if (oldPath != newPath) {
//        repo.remove(YarnRepo.pathOfMappingFromGitRoot(oldPath))
//    }
//
//    renameTarget.root.writeTo(YarnRepo.getMappingsFile(newPath))
//    repo.stageChanges(YarnRepo.pathOfMappingFromGitRoot(newPath))
//    repo.commit(author = YarnRepo.TemporaryAuthor, commitMessage = "$oldName -> $newName")
//    YarnRepo.push(repo)
//    println("Changes pushed successfully!")
//
//
//}