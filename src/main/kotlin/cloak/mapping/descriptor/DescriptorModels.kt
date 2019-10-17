package cloak.mapping.descriptor

// Comes directly from the spec https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.2
typealias FieldDescriptor = FieldType

typealias FieldDescriptorCompanion = FieldType.Companion

sealed class Descriptor(val classFileName: String) {
    override fun equals(other: Any?) = other is Descriptor && other.classFileName == classFileName
    override fun toString() = "Descriptor(classFileName=$classFileName)"
    override fun hashCode() = classFileName.hashCode()
}

sealed class ReturnDescriptor(classFileName: String) : Descriptor(classFileName) {
    object Void : ReturnDescriptor("V")
}

sealed class FieldType(classFileName: String) : ReturnDescriptor(classFileName) {
    companion object
    object Byte : FieldType("B")
    object Char : FieldType("C")
    object Double : FieldType("D")
    object Float : FieldType("F")
    object Int : FieldType("I")
    object Long : FieldType("J")
    object Short : FieldType("S")
    object Boolean : FieldType("Z")
}

data class ObjectType(val className: String) : FieldType("L$className;")
data class ArrayType(val componentType: FieldType) : FieldType("[" + componentType.classFileName)

data class MethodDescriptor(
    val parameterDescriptors: List<FieldType>,
    val returnDescriptor: ReturnDescriptor
) : Descriptor("(${parameterDescriptors.joinToString("") { it.classFileName }})${returnDescriptor.classFileName}") {
    companion object
}


