package cloak.format.descriptor

import cloak.format.descriptor.PrimitiveType.Boolean
import cloak.format.descriptor.PrimitiveType.Byte
import cloak.format.descriptor.PrimitiveType.Char
import cloak.format.descriptor.PrimitiveType.Double
import cloak.format.descriptor.PrimitiveType.Float
import cloak.format.descriptor.PrimitiveType.Int
import cloak.format.descriptor.PrimitiveType.Long
import cloak.format.descriptor.PrimitiveType.Short

fun FieldDescriptorCompanion.read(descriptor: String): FieldDescriptor {
    baseTypesMap[descriptor]?.let { return it }
    require(descriptor.isNotEmpty()) { "A descriptor cannot be an empty string" }
    if (descriptor[0] == '[') return ArrayType(read(descriptor.substring(1)))
    require(descriptor[0] == 'L' && descriptor.last() == ';')
    { "'$descriptor' is not a descriptor: A field descriptor must be basic, an array ([), or a class (L)" }

    // I am unhappy substring copies the entire string
    return ObjectType(descriptor.substring(1, descriptor.length - 1))
}

private val baseTypesMap = mapOf(
    Byte.classFileName to Byte,
    Char.classFileName to Char,
    Double.classFileName to Double,
    Float.classFileName to Float,
    Int.classFileName to Int,
    Long.classFileName to Long,
    Short.classFileName to Short,
    Boolean.classFileName to Boolean
)

private val baseTypesCharMap = baseTypesMap.mapKeys { it.key[0] }

fun MethodDescriptor.Companion.read(descriptor: String): MethodDescriptor {
    require(descriptor[0] == '(') { "A method descriptor must begin with a '('" }
    val parameterDescriptors = mutableListOf<FieldType>()
    var parameterStartPos = 1
    var endPos: kotlin.Int? = null
    var inClassName = false
    for (i in 1 until descriptor.length) {
        val c = descriptor[i]

        var descriptorTerminated = false

        if (inClassName) {
            if (c == ';') {
                descriptorTerminated = true
                inClassName = false
            }
        } else {
            if (baseTypesCharMap[c] != null) descriptorTerminated = true
        }

        if (c == 'L') inClassName = true

        if (descriptorTerminated) {
            parameterDescriptors.add(FieldDescriptor.read(descriptor.substring(parameterStartPos, i + 1)))
            parameterStartPos = i + 1
        }

        if (c == ')') {
            require(!inClassName) { "Class name was not terminated" }
            endPos = i
            break
        }

    }

    requireNotNull(endPos) { "The parameter list of a method descriptor must end with a ')'" }
    require(endPos < descriptor.length - 1) { "A method descriptor must have a return type" }
    val returnDescriptorString = descriptor.substring(endPos + 1, descriptor.length)
    val returnDescriptor = if (returnDescriptorString == ReturnDescriptor.Void.classFileName) ReturnDescriptor.Void
    else FieldDescriptor.read(returnDescriptorString)

    return MethodDescriptor(parameterDescriptors, returnDescriptor)

}