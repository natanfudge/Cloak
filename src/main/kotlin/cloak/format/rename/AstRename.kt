package cloak.format.rename

import cloak.format.mappings.*
import cloak.util.fail
import cloak.util.success
import com.github.michaelbull.result.Result


fun rename(mapping: Mapping, newName: String): Result<Unit, String> = when (mapping) {
    is ClassMapping -> renameClass(mapping, newName)
    is MethodMapping -> renameMethod(mapping, newName)
    is FieldMapping -> renameField(mapping, newName)
    is ParameterMapping -> renameParameter(mapping, newName)
}

private fun Mapping.clashesWith(name: String) = displayedName == name

private fun nameTaken(name: String, mapping: Mapping) =
    fail("In the latest yarn version there's already a ${mapping.typeName()} named $name inside ${mapping.parent!!.displayedName}")

private fun renameClass(mapping: ClassMapping, newName: String): Result<Unit, String> {
    if (mapping.parent != null && mapping.parent.innerClasses.any { it.clashesWith(newName) }) {
        return nameTaken(newName, mapping)
    }
    val packageName = (mapping.deobfuscatedName ?: mapping.obfuscatedName).split("/")
        .let { it.subList(0, it.size - 1).joinToString("/") }

    mapping.deobfuscatedName = "${if (packageName.isNotEmpty()) "$packageName/" else ""}$newName"

    return success()
}

private fun renameField(mapping: FieldMapping, newName: String): Result<Unit, String> {
    if (mapping.parent.methods.any { it.deobfuscatedName == newName }) {
        return nameTaken(newName, mapping)
    }
    mapping.deobfuscatedName = newName
    return success()
}


private fun renameMethod(mapping: MethodMapping, newName: String): Result<Unit, String> {
    if (mapping.parent.methods.any { it.deobfuscatedName == newName && it.descriptor == mapping.descriptor }) {
        return nameTaken(newName, mapping)
    }
    mapping.deobfuscatedName = newName
    return success()
}


private fun renameParameter(mapping: ParameterMapping, newName: String): Result<Unit, String> {
    if (mapping.parent.parameters.any { it.deobfuscatedName == newName }) {
        return nameTaken(newName, mapping)
    }
    mapping.deobfuscatedName = newName
    return success()
}

private val MethodMapping.readableDisplayedName: String get() = if (isConstructor) "the constructor" else displayedName
