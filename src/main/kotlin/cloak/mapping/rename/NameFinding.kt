package cloak.mapping.rename

import cloak.mapping.YarnRepo
import cloak.mapping.mappings.Mapping
import cloak.mapping.mappings.MappingsExtension
import cloak.mapping.mappings.MappingsFile
import cloak.mapping.mappings.read
import java.io.File
import java.nio.file.Paths

fun Name.findMatchingMapping(relativePath: String,yarnRepo: YarnRepo): Mapping? {
    val topLevelClassCache = topLevelClass
    if (!relativePath.endsWith(topLevelClassCache.className)) return null
    if(!relativePath.startsWith(topLevelClassCache.packageName)) return null

    return findRenameTarget(MappingsFile.read(yarnRepo.getMappingsFile("$relativePath$MappingsExtension")))
}