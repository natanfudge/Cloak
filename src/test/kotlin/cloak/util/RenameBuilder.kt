package cloak.util

import cloak.mapping.descriptor.ParameterDescriptor
import cloak.mapping.rename.*
import cloak.mapping.splitOn


fun className(
    name: String,
    init: (ClassBuilder.() -> NameBuilder<*>)? = null
): Name {
    val (packageName, topLevelClassName) = splitPackageAndName(name)
    val builder = ClassBuilder(listOf(topLevelClassName), packageName ?: "")
    val result = init?.invoke(builder) ?: builder
    return result.build()
}


interface NameBuilder<T : Name> {
    fun build(): T
}


class ClassBuilder(private val classChain: List<String>, private val packageName: String) : NameBuilder<ClassName> {
    fun innerClass(className: String) = ClassBuilder(classChain + className, packageName)
    fun field(fieldName: String) = FieldBuilder(build(), fieldName)
    fun method(methodName: String, vararg parameterTypes: ParameterDescriptor) = MethodBuilder(
        build(), methodName, parameterTypes.toList()
    )

    // Kind of crap code but idc it does the job
    override fun build(): ClassName {
        var nextClass: ClassName? = null
        for (innerClass in classChain) {
            nextClass = ClassName(
                className = innerClass,
                classIn = nextClass,
                packageName = packageName
            )
        }
        return nextClass!!
    }

}

class FieldBuilder(private val className: ClassName, private val field: String) :
    NameBuilder<FieldName> {
    override fun build() = FieldName(field, className)
}

class MethodBuilder(
    private val className: ClassName, private val method: String,
    private val parameterTypes: List<ParameterDescriptor>
) : NameBuilder<MethodName> {
    override fun build() = MethodName(method, className, parameterTypes)
    fun parameter(index: Int) = ParamBuilder(ParamName(index, build()))
}

class ParamBuilder(private val param: ParamName) : NameBuilder<ParamName> {
    override fun build() = param
}



