package cloak.idea.util

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import kotlin.reflect.KProperty

object PersistentState {
    fun getBoolean(key: String) = PropertiesComponent.getInstance().getBoolean(key)
    fun setBoolean(key: String, value: Boolean) = PropertiesComponent.getInstance().setValue(key, value)
}

class PsiElementDataDelegate<T>(name: String, private val defaultValue: T) {
    private val key = Key<T>(name)
    operator fun getValue(thisRef: PsiFile, property: KProperty<*>): T = thisRef.getUserData(key) ?: defaultValue
    operator fun setValue(thisRef: PsiFile, property: KProperty<*>, value: T) {
        thisRef.putUserData(key, value)
    }
}

fun <T> psiData(name: String, defaultValue: T) = PsiElementDataDelegate(name, defaultValue)