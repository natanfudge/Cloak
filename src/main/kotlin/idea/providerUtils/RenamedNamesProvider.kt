package cloak.idea.providerUtils

import RenamedNamesProvider
import cloak.idea.providerUtils.ObjWrapper.NameSerializer
import cloak.idea.providerUtils.ObjWrapper.NewNameSerializer
import cloak.mapping.rename.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.modules.SerializersModule


object ObjWrapper {
    private val nameModule = SerializersModule {
        polymorphic(Name::class) {
            ClassName::class with ClassName.serializer()
            MethodName::class with MethodName.serializer()
            FieldName::class with FieldName.serializer()
            ParamName::class with ParamName.serializer()
        }
    }
    val NamesJson = Json(JsonConfiguration.Stable, context = nameModule)
    // Odd that I need to cast here
    val NameSerializer = PolymorphicSerializer(Name::class) as KSerializer<Name>

    val NewNameSerializer = NewName.serializer()
}

@Serializable
data class NewName(val newName: String, val newPackageName: String?) {
    override fun toString() = if (newPackageName != null) "$newPackageName/$newName" else newName
}

fun loadState(state: RenamedNamesProvider.State, renamedNames: MutableMap<Name, NewName>) {
    val json = ObjWrapper.NamesJson
    for((k,v) in state.renamedNamesJson){
        renamedNames[json.parse(NameSerializer, k)] = json.parse(NewNameSerializer, v)
    }

}