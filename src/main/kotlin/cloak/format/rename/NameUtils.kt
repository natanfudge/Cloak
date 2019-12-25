package cloak.format.rename

import cloak.format.descriptor.MethodDescriptor
import cloak.format.descriptor.ReturnDescriptor
import cloak.format.mappings.ConstructorName
import cloak.util.splitOn

private inline fun <T> flatten(seed: T, getNext: T.() -> T?): List<T> {
    val result = mutableListOf<T>()
    var next: T? = seed.getNext()
    while (next != null) {
        result.add(next)
        next = next.getNext()
    }
    return result
}

inline fun <T> flattenWithSelf(seed: T, getNext: T.() -> T?): List<T> {
    val result = mutableListOf<T>()
    var next: T? = seed
    while (next != null) {
        result.add(next)
        next = next.getNext()
    }
    return result
}


// This goes from top to bottom. Does not include self.
fun ClassName.getParents(): List<ClassName> = flatten(this) { this.classIn }.reversed()

// Includes self
fun ClassName.getParentsAndSelf(): List<ClassName> = flattenWithSelf(this) { this.classIn }.reversed()

val Name.isTopLevelClass get() = this.parent == null

val Name.parent: Name?
    get() {
        return when (this) {
            is ClassName -> classIn
            is FieldName -> classIn
            is MethodName -> classIn
            is ParamName -> methodIn
        }
    }

fun splitPackageAndName(rawName: String): Pair<String?, String> {
    val lastSlashIndex = rawName.lastIndexOf('/')
    return if (lastSlashIndex == -1) null to rawName
    else rawName.splitOn(lastSlashIndex)
}


fun Name.getOwnName() = if (this is ClassName && isTopLevelClass) "$packageName/$className"
else shortName


val Name.shortName
    get() = when (this) {
        is ClassName -> className
        is FieldName -> fieldName
        is MethodName -> methodName
        is ParamName -> paramName
    }

fun MethodName.toDescriptor(returnType: ReturnDescriptor): MethodDescriptor =
    MethodDescriptor(parameterTypes, returnType)

val Name.isConstructor get() = this is MethodName &&  this.methodName == ConstructorName