//package cloak.idea
//
//import com.intellij.ide.util.PropertiesComponent
//import com.intellij.openapi.util.Key
//import com.intellij.psi.PsiElement
//
//private val elementRenamedKey = Key<String>("renamedTo")
//private val elementsRenamedWithoutRefreshKey = "anyRenamed"
//fun PsiElement.markRenamedTo(newName: String) {
//    putUserData(elementRenamedKey, newName)
//    renamedElementsWithoutRefresh[this] = newName
//    elementsWereRenamedWithoutRefresh = true
//}
//
//fun PsiElement.getRenameMarkUserData() : String? = getUserData(elementRenamedKey)
//fun PsiElement.getRenameMarkMap() : String? = renamedElementsWithoutRefresh[this]
//
//// To clean the elements of the mark when we refresh
//private val renamedElementsWithoutRefresh : MutableMap<PsiElement,String> = mutableMapOf()
//
//
///**
// * Whether or not the user has recently renamed an element, without refreshing the mappings.
// * We use this to automatically refresh mappings on first call to the highlighter when needed.
// */
//var elementsWereRenamedWithoutRefresh: Boolean
//    get() = PropertiesComponent.getInstance()
//        .getBoolean(elementsRenamedWithoutRefreshKey, false)
//    set(value) = PropertiesComponent.getInstance().setValue(elementsRenamedWithoutRefreshKey, value)