package cloak.format.mappings

import cloak.format.descriptor.MethodDescriptor
import cloak.format.descriptor.read
import cloak.util.put
import java.io.File


fun MappingsFileCompanion.read(file: File): MappingsFile {
    val classesIn = mutableListOf<ClassMapping>()
    var methodIn: MethodMapping? = null

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
                    }
                }
                Prefix.Field -> {
                    val parent = classesIn.getOrNull(indentCount - NaturalIndent.Field)
                        ?: error("Missing parent class of field")
                    parent.fields.add(parseField(tokens, parent))

                }
                Prefix.Method -> {
                    val parent = classesIn.getOrNull(indentCount - NaturalIndent.Method)
                        ?: error("Missing parent class of method")
                    parseMethod(tokens, parent).let {
                        methodIn = it
                        parent.methods.add(it)
                    }
                }
                Prefix.Parameter -> {
                    val parent = methodIn ?: error("Missing parent method of parameter")
                    parent.parameters.add(parseParameter(tokens, parent))
                }
                else -> error("Unknown token '${tokens[0]}'")
            }
        }
    }


    return classesIn.getOrNull(0) ?: error("No class found in file")
}


fun parseClass(tokens: List<String>, parent: ClassMapping?) = ClassMapping(
    obfuscatedName = tokens[1],
    deobfuscatedName = tokens.getOrNull(2),
    fields = mutableListOf(), innerClasses = mutableListOf(), methods = mutableListOf(), parent = parent
)

fun parseField(tokens: List<String>, parent: ClassMapping) = FieldMapping(
    obfuscatedName = tokens[1],
    deobfuscatedName = tokens[2],
    descriptor = tokens[3],
    parent = parent
)

fun parseMethod(
    tokens: List<String>,
    parent: ClassMapping
): MethodMapping {
    var deobfName: String? = null
    val descriptor: String
    when (val tokenAmount = tokens.size) {
        3 -> descriptor = tokens[2]
        4 -> {
            deobfName = tokens[2]
            descriptor = tokens[3]
        }
        else -> error("Invalid method declaration, got $tokenAmount tokens.")
    }

    return MethodMapping(
        obfuscatedName = tokens[1],
        deobfuscatedName = deobfName,
        descriptor = MethodDescriptor.read(descriptor) ,
        parameters = mutableListOf(),
        parent = parent
    )
}

fun parseParameter(tokens: List<String>, parent: MethodMapping): ParameterMapping = ParameterMapping(
    index = tokens[1].toInt(),
    deobfuscatedName = tokens[2],
    parent = parent
)
