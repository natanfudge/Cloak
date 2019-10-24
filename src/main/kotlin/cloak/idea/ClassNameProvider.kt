package cloak.idea

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

// So we can find methods that have intermediary descriptors while the user sees named descriptors
@State(name = "IntermediaryToNamed", storages = [Storage(StoragePathMacros.CACHE_FILE)])
data class ClassNameProvider(
    val namedToIntermediary: MutableMap<String, String> = mutableMapOf()
)
    : PersistentStateComponent<ClassNameProvider?> {

    override fun getState(): ClassNameProvider? {
        return this
    }

    override fun loadState(state: ClassNameProvider) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val Instance: ClassNameProvider
            get() = ServiceManager.getService(ClassNameProvider::class.java)
    }
}