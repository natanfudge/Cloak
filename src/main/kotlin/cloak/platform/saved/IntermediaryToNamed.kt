package cloak.platform.saved

import cloak.format.mappings.*
import cloak.git.yarnRepo
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.buildMap
import cloak.util.mutableMap
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.internal.nullable

private var ExtendedPlatform.namedToIntermediaryStore: MutableMap<String, String>? by SavedState(
    null,
    (StringSerializer to StringSerializer).mutableMap.nullable
)

fun ExtendedPlatform.setIntermediaryName(named: String, intermediary: String) {
    namedToIntermediaryStore?.set(named, intermediary)
}


fun ExtendedPlatform.getNamedToIntermediary(): Map<String, String> {
    if (namedToIntermediaryStore == null) {
        namedToIntermediaryStore = buildMap {
            for (relativePath in yarnRepo.getMappingsFilesLocations()) {
                MappingsFile.read(yarnRepo.getMappingsFile("$relativePath$MappingsExtension")).visitClasses { mapping ->
                    mapping.getAsKeyValue()?.let { (obf, deobf) -> put(deobf, obf) }
                }
            }
        }
    }
    return namedToIntermediaryStore!!
}


private fun ClassMapping.getAsKeyValue(): Pair<String, String>? {
    val fullPath = mutableListOf<ClassMapping>()
    var next: ClassMapping? = this@getAsKeyValue
    while (next != null) {
        if (next.deobfuscatedName == null) return null
        fullPath.add(next)
        next = next.parent
    }

    fullPath.reverse()

    return Pair(fullPath.joinToString(Joiner.InnerClass) { it.obfuscatedName },
        fullPath.joinToString(Joiner.InnerClass) {
            it.deobfuscatedName ?: error("It's checked earlier that deobfuscatedName != null")
        }
    )
}