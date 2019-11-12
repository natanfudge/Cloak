package cloak.util

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import kotlinx.serialization.KSerializer

//interface KSerializable<T : KSerializable<T>> {
//    fun serializer() : KSerializer<T>
//}

abstract class IdeaCache<T>(private var cached: T) : PersistentStateComponent<T>/*, Cache<T> */{
    override fun getState(): T = cached
    fun setState(newState : T){
        cached = newState
    }
    override fun loadState(state: T) {
        this.cached = state
    }
}

abstract class IdeaCacheGetter<T>(private val componentClass: Class<T>) {
    val instance get() = ServiceManager.getService(componentClass)
}