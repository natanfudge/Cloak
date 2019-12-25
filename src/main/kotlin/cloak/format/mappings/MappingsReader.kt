package cloak.format.mappings

import cloak.format.descriptor.MethodDescriptor
import cloak.format.descriptor.read
import java.io.File


private fun <T> MutableList<T>.put(index: Int, item: T) {
    if (index < size) set(index, item) else add(index, item)
}

fun ClassMapping.Companion.read(file: File): MappingsFile {
    val classesIn = mutableListOf<ClassMapping>()
    var methodIn: MethodMapping? = null
    var mappingIn: Mapping? = null

    file.bufferedReader().use { reader ->
        reader.lines().forEach { line ->
            var indentCount = 0
            val lineWithoutIndentBuilder = StringBuilder()
            for (c in line) {
                if (c == '\t') indentCount++
                else lineWithoutIndentBuilder.append(c)
            }

            val lineWithoutIndent = lineWithoutIndentBuilder.toString()

            val tokens = lineWithoutIndent.split(" ")
            when (tokens[0]) {
                Prefix.Class -> {
                    val nestingLevel = indentCount - NaturalIndent.Class
                    val parentClass = if (nestingLevel == 0) null else classesIn[nestingLevel - 1]
                    parseClass(tokens, parentClass).let {
                        parentClass?.innerClasses?.add(it)
                        classesIn.put(nestingLevel, it)
                        mappingIn = it
                    }
                }
                Prefix.Field -> {
                    val parent = classesIn.getOrNull(indentCount - NaturalIndent.Field)
                        ?: error("Missing parent class of field")
                    parseField(tokens, parent).let {
                        parent.fields.add(it)
                        mappingIn = it
                    }
                }
                Prefix.Method -> {
                    val parent = classesIn.getOrNull(indentCount - NaturalIndent.Method)
                        ?: error("Missing parent class of method")
                    parseMethod(tokens, parent).let {
                        parent.methods.add(it)
                        methodIn = it
                        mappingIn = it
                    }
                }
                Prefix.Parameter -> {
                    val parent = methodIn ?: error("Missing parent method of parameter")
                    parseParameter(tokens, parent).let {
                        parent.parameters.add(it)
                        mappingIn = it
                    }

                }
                Prefix.Comment -> {
                    val targetMapping = mappingIn ?: error("Comment line without a parent: ${tokens[0]}")
                    targetMapping.comment.add(tokens.subList(1, tokens.size).joinToString(" "))
                }
                else -> error("Unknown token '${tokens[0]}'")
            }
        }
    }


    return classesIn.getOrNull(0) ?: error("No class found in file")
}


fun parseClass(tokens: List<String>, parent: ClassMapping?) =
    ClassMapping(
        obfuscatedName = tokens[1],
        deobfuscatedName = tokens.getOrNull(2),
        parent = parent
    )

fun parseField(tokens: List<String>, parent: ClassMapping): FieldMapping {
    val (deobfName, descriptor) = when (val tokenAmount = tokens.size) {
        3 -> null to tokens[2]
        4 -> tokens[2] to tokens[3]
        else -> error("Invalid field declaration, got $tokenAmount tokens.")
    }
    return FieldMapping(
        obfuscatedName = tokens[1],
        deobfuscatedName = deobfName,
        descriptor = descriptor,
        parent = parent
    )
}


fun parseMethod(
    tokens: List<String>,
    parent: ClassMapping
): MethodMapping {
    val (deobfName, descriptor) = when (val tokenAmount = tokens.size) {
        3 -> null to tokens[2]
        4 -> tokens[2] to tokens[3]
        else -> error("Invalid method declaration, got $tokenAmount tokens.")
    }

    return MethodMapping(
        obfuscatedName = tokens[1],
        deobfuscatedName = deobfName,
        descriptor = MethodDescriptor.read(descriptor),
        parameters = mutableListOf(),
        parent = parent
    )
}

fun parseParameter(tokens: List<String>, parent: MethodMapping): ParameterMapping =
    ParameterMapping(
        index = tokens[1].toInt(),
        deobfuscatedName = tokens[2],
        parent = parent
    )
