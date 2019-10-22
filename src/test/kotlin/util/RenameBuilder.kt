package util

import cloak.mapping.*
import cloak.mapping.descriptor.ParameterDescriptor
import cloak.mapping.rename.*
import cloak.mapping.rename.ParameterName


fun className(
    name: String,
    init: (ClassBuilder.() -> NameBuilder<*>)? = null
): Name<*> {
    val builder = ClassBuilder(listOf(name))
    val result = init?.invoke(builder) ?: builder
    return result.build()
}


interface NameBuilder<T : Name<*>> {
    fun build(): T
}


class ClassBuilder(private val innerClasses: List<String>) : NameBuilder<ClassName> {
    fun innerClass(className: String) = ClassBuilder(innerClasses + className)
    fun field(fieldName: String) = FieldBuilder(build(), fieldName)
    fun method(methodName: String, vararg parameterTypes: ParameterDescriptor) = MethodBuilder(
        build(), methodName, parameterTypes.toList()
    )

    // Kind of crap code but idc it does the job
    override fun build(): ClassName {
        val (packageName, topLevelClassName) = innerClasses.first().let { it.splitOn(it.lastIndexOf("/")) }
        val innerMostClass = innerClasses.last()
        var classNameHolder = ClassName(
            className =
            if (innerClasses.size == 1) topLevelClassName else innerMostClass,
            packageName = packageName,
            innerClass = null
        )
        if (innerClasses.size >= 2) {
            for (className in innerClasses.subList(1).reversed().subList(1)) {
                classNameHolder = ClassName(
                    className = className, packageName = packageName, innerClass = classNameHolder
                )
            }

            classNameHolder =
                ClassName(
                    className = topLevelClassName,
                    innerClass = classNameHolder,
                    packageName = packageName
                )
        }


        return classNameHolder
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
    fun parameter(index: Int) = ParameterName(index, build())
}


private fun <E> List<E>.subList(fromIndex: Int) = subList(fromIndex, size)

