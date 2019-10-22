package cloak.mapping.rename

import cloak.mapping.Errorable
import cloak.mapping.fail
import cloak.mapping.mappings.*
import cloak.mapping.success

fun rename(mapping: Mapping, newName: String): Errorable<Unit> = when (mapping) {
    is ClassMapping -> renameClass(mapping, newName)
    is MethodMapping -> renameMethod(mapping, newName)
    is FieldMapping -> renameField(mapping, newName)
    is ParameterMapping -> renameParameter(mapping, newName)
}

private fun Mapping.clashesWith(name: String) = nonNullName == name

private fun renameClass(mapping: ClassMapping, newName: String): Errorable<Unit> {
    if (mapping.parent != null && mapping.parent.innerClasses.any { it.clashesWith(newName) }) {
        return fail("There already a class named $newName inside ${mapping.parent.nonNullName}")
    }
    val packageName = (mapping.deobfuscatedName ?: mapping.obfuscatedName).split("/")
        .let { it.subList(0, it.size - 1).joinToString("/") }

    mapping.deobfuscatedName = "${if (packageName.isNotEmpty()) "$packageName/" else ""}$newName"

    return success()
}

private fun renameField(mapping: FieldMapping, newName: String): Errorable<Unit> {
    if (mapping.parent.methods.any { it.deobfuscatedName == newName }) {
        return fail("There's already a field named $newName inside ${mapping.parent.nonNullName}")
    }
    mapping.deobfuscatedName = newName
    return success()
}


private fun renameMethod(mapping: MethodMapping, newName: String): Errorable<Unit> {
    if (mapping.parent.methods.any { it.deobfuscatedName == newName && it.descriptor == mapping.descriptor }) {
        return fail("There's already a method named $newName(TODO PARSE DESCRIPTOR) inside ${mapping.parent.nonNullName}")
    }
    mapping.deobfuscatedName = newName
    return success()
}


private fun renameParameter(mapping: ParameterMapping, newName: String): Errorable<Unit> {
    if (mapping.parent.parameters.any { it.deobfuscatedName == newName }) {
        return fail("There's already a parameter named $newName inside ${mapping.parent.nonNullName}")
    }
    mapping.deobfuscatedName = newName
    return success()
}


