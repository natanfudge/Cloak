package cloak.mapping.mappings

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

 fun Mapping.typeName() = when(this){
     is ClassMapping -> "class"
     is MethodMapping -> "method"
     is FieldMapping -> "field"
     is ParameterMapping -> "parameter"
 }

const val MappingsExtension = ".mapping"
const val ConstructorName = "<init>"
val MethodMapping.isConstructor get() = obfuscatedName == ConstructorName