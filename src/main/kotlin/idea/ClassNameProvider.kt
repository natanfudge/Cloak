package cloak.idea

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

//// So we can find methods that have intermediary descriptors while the user sees named descriptors
//@State(name = "IntermediaryToNamed", storages = [Storage(StoragePathMacros.CACHE_FILE)])
// class ClassNameProvider : PersistentStateComponent<ClassNameProvider.State> {
//    private var state = State()
//
//    data class State @JvmOverloads constructor(val namedToIntermediary: MutableMap<String, String> = mutableMapOf())
//
//    override fun getState(): State {
//        return state
//    }
//
//    override fun loadState(state: State) {
//        this.state = state
//    }
//
//    companion object {
//        val Instance: ClassNameProvider
//            get() = ServiceManager.getService(ClassNameProvider::class.java)
//    }
//}