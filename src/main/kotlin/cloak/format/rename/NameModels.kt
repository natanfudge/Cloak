package cloak.format.rename

import cloak.format.descriptor.ParameterDescriptor
import cloak.format.descriptor.ParameterDescriptorSerializer
import cloak.format.mappings.ClassMapping
import cloak.format.mappings.Joiner
import cloak.format.mappings.Mapping
import cloak.util.success
import kotlinx.serialization.Serializable


@Serializable
sealed class Name {
    abstract val topLevelClass: ClassName
}

@Serializable
data class ClassName(val className: String, val packageName: String, val classIn: ClassName?) :
    Name() {
    override val topLevelClass: ClassName
        get() {
            var next = this
            while (next.classIn != null) {
                next = next.classIn!!
            }
            return next
        }

    override fun toString() = fullyQualifiedName()

}

fun ClassName.fullyQualifiedName() = "$packageName/" + getParentsAndSelf().joinToString("\$") { it.className }


@Serializable
data class FieldName(val fieldName: String, val classIn: ClassName) : Name() {
    override val topLevelClass = classIn.topLevelClass
    override fun toString() = "$classIn${Joiner.Field}$fieldName"
}

@Serializable
data class MethodName(
    val methodName: String, val classIn: ClassName,
    val parameterTypes: List<@Serializable(with = ParameterDescriptorSerializer::class) ParameterDescriptor>
) : Name() {
    override val topLevelClass = classIn.topLevelClass
    override fun toString() = "$classIn${Joiner.Method}$methodName(${parameterTypes.joinToString(", ")})"
}

@Serializable
data class ParamName(val index: Int,val paramName : String, val methodIn: MethodName) : Name() {
    override val topLevelClass = methodIn.topLevelClass
    override fun toString() = "$methodIn[$index]"
}
