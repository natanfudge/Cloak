package util

import cloak.mapping.descriptor.ParameterDescriptor
import cloak.mapping.rename.*
import cloak.mapping.splitOn


fun className(
    name: String,
    init: (ClassBuilder.() -> NameBuilder<*>)? = null
): Name {
    val builder = ClassBuilder(listOf(name))
    val result = init?.invoke(builder) ?: builder
    return result.build()
}


interface NameBuilder<T : Name> {
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
        val (packageName, topLevelClassName) = splitPackageAndName(innerClasses.first())
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

    private fun splitPackageAndName(rawName: String): Pair<String?, String> {
        val lastSlashIndex = rawName.lastIndexOf('/')
        return if (lastSlashIndex == -1) null to rawName
        else rawName.splitOn(lastSlashIndex)
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
class ParamBuilder(private val param : ParamName) : NameBuilder<ParamName>{
    override fun build() = param
}




private fun <E> List<E>.subList(fromIndex: Int) = subList(fromIndex, size)

