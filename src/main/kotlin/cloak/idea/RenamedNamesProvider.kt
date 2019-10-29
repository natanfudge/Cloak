package cloak.idea

import cloak.mapping.rename.*
import com.intellij.openapi.components.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule


object ObjWrapper{
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
}


//// So we can find methods that have intermediary descriptors while the user sees named descriptors
//@State(name = "RenamedNames", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)])
//data class RenamedNamesProvider(
////    var stateValue : String? = null
//     val renamedNamesJson: MutableMap<String, String> = mutableMapOf()
//) : PersistentStateComponent<RenamedNamesProvider> {
//
//    private val renamedNames = mutableMapOf<Name, String>()
//
//    fun addRenamedName(name: Name, renamedTo: String) {
//        renamedNames[name] = renamedTo
//        renamedNamesJson[NamesJson.stringify(NameSerializer, name)] = renamedTo
//    }
//
//    fun clearNames() {
//        renamedNames.clear()
//        renamedNamesJson.clear()
//    }
//
//    fun anythingWasRenamed() = renamedNames.isNotEmpty()
//
//    fun getRenameOf(name : Name) : String? = renamedNames[name]
//
//
//    override fun getState(): RenamedNamesProvider {
//        return this
//    }
//
//    override fun loadState(state: RenamedNamesProvider) {
//        XmlSerializerUtil.copyBean(state, this)
//        for ((k, v) in renamedNamesJson) renamedNames[NamesJson.parse(NameSerializer, k)] = v
//    }
//
//    companion object {
//        val Instance: RenamedNamesProvider
//            get() = ServiceManager.getService(RenamedNamesProvider::class.java)
//    }
//}