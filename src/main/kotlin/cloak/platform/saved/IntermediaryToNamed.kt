package cloak.platform.saved

import cloak.git.YarnRepo
import cloak.format.mappings.*
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.buildMap
import cloak.util.mutableMap
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.internal.nullable

private var ExtendedPlatform.namedToIntermediary: MutableMap<String, String>? by SavedState(
    mutableMapOf(),
    (StringSerializer to StringSerializer).mutableMap.nullable
)

fun ExtendedPlatform.setIntermediaryName(named: String, intermediary: String) {
    namedToIntermediary?.set(named, intermediary)
}


fun ExtendedPlatform.getNamedToIntermediary(yarnRepo: YarnRepo): Map<String, String> {
    if (namedToIntermediary == null) {
        namedToIntermediary = buildMap {
            for (relativePath in yarnRepo.getMappingsFilesLocations()) {
                MappingsFile.read(yarnRepo.getMappingsFile("$relativePath$MappingsExtension")).visitClasses { mapping ->
                    mapping.getAsKeyValue()?.let { (obf, deobf) -> put(deobf, obf) }
                }
            }
        }
    }
    return namedToIntermediary!!
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