package cloak.util

import cloak.format.descriptor.FieldDescriptor
import cloak.format.descriptor.FieldType
import cloak.format.descriptor.MethodDescriptor
import cloak.format.descriptor.ReturnDescriptor
import cloak.format.mappings.*


@DslMarker
annotation class MappingsBuilderDsl

fun mappingsFile(topLevelObf: String, topLevelDeobf: String? = null, init: ClassMappingsBuilder.() -> Unit) =
    ClassMappingsBuilder(topLevelObf, topLevelDeobf, null).apply(init).build()

interface MappingsBuilder<T : Mapping> {
    val mapping: T
}

fun <T : Mapping> MappingsBuilder<T>.build() = mapping
fun <T : Mapping> MappingsBuilder<T>.comment(comment: String) = mapping.comment.add(comment)


@MappingsBuilderDsl
class ClassMappingsBuilder(obfuscatedName: String, deobfuscatedName: String?, parent: ClassMapping?) :
    MappingsBuilder<ClassMapping> {
    override val mapping = ClassMapping(
        obfuscatedName, deobfuscatedName, mutableListOf(), mutableListOf(),
        mutableListOf(), parent
    )

    fun innerClass(obfName: String, deobfName: String? = null, init: ClassMappingsBuilder.() -> Unit = {}) {
        mapping.innerClasses.add(ClassMappingsBuilder(obfName, deobfName, mapping).apply(init).build())
    }

    fun method(
        obfName: String,
        deobfName: String? = null,
        returnType: ReturnDescriptor = ReturnDescriptor.Void,
        vararg parameterTypes: FieldType,
        init: MethodMappingsBuilder.() -> Unit = {}
    ) = mapping.methods.add(
        MethodMappingsBuilder(obfName, deobfName, MethodDescriptor(parameterTypes.toList(), returnType), mapping)
            .apply(init).build()
    )

    fun field(obfName: String, deobfName: String?, type: FieldDescriptor,init: FieldMappingsBuilder.() -> Unit = {}) = mapping.fields.add(
        FieldMappingsBuilder(obfName, deobfName, type, mapping).apply(init).build()
    )

}

@MappingsBuilderDsl
class MethodMappingsBuilder(
    obfuscatedName: String, deobfuscatedName: String?, descriptor: MethodDescriptor, parent: ClassMapping
) : MappingsBuilder<MethodMapping> {

    override val mapping = MethodMapping(
        obfuscatedName, deobfuscatedName, descriptor,
        mutableListOf(), parent
    )

    fun param(index: Int, name: String, init: ParameterMappingsBuilder.() -> Unit = {}) =
        mapping.parameters.add(ParameterMappingsBuilder(index, name, mapping).apply(init).build())
}

@MappingsBuilderDsl
class ParameterMappingsBuilder(index: Int, name: String, parent: MethodMapping) : MappingsBuilder<ParameterMapping> {
    override val mapping = ParameterMapping(index, name, parent)
}
@MappingsBuilderDsl
class FieldMappingsBuilder(obfName: String, deobfName: String?, type: FieldDescriptor,parent: ClassMapping) : MappingsBuilder<FieldMapping> {
    override val mapping = FieldMapping(obfName, deobfName, type.classFileName, parent)
}