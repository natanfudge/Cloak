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

private fun renameClass(mapping: ClassMapping, newName: String): Result<Unit, String> {
    if (mapping.parent != null && mapping.parent!!.innerClasses.any { it.clashesWith(newName) }) {
        return fail("There already a class named $newName inside ${mapping.parent!!.displayedName}")
    }
    val packageName = (mapping.deobfuscatedName ?: mapping.obfuscatedName).split("/")
        .let { it.subList(0, it.size - 1).joinToString("/") }

    mapping.deobfuscatedName = "${if (packageName.isNotEmpty()) "$packageName/" else ""}$newName"

    return success()
}

private fun renameField(mapping: FieldMapping, newName: String): Result<Unit, String> {
    if (mapping.parent.methods.any { it.deobfuscatedName == newName }) {
        return fail("There's already a field named $newName inside ${mapping.parent.displayedName}")
    }
    mapping.deobfuscatedName = newName
    return success()
}


private fun renameMethod(mapping: MethodMapping, newName: String): Result<Unit, String> {
    if (mapping.parent.methods.any { it.deobfuscatedName == newName && it.descriptor == mapping.descriptor }) {
        return fail("There's already a method named $newName(${mapping.descriptor.parameterDescriptors.joinToString(", ")}) inside ${mapping.parent.displayedName}")
    }
    mapping.deobfuscatedName = newName
    return success()
}


private fun renameParameter(mapping: ParameterMapping, newName: String): Result<Unit, String> {
    if (mapping.parent.parameters.any { it.deobfuscatedName == newName }) {
        return fail("There's already a parameter named $newName inside ${mapping.parent.displayedName}")
    }
    mapping.deobfuscatedName = newName
    return success()
}


