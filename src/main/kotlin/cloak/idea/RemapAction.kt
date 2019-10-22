package cloak.idea

import cloak.idea.util.Action
import cloak.idea.util.IdeaProjectWrapper
import cloak.idea.util.isInnerClass
import cloak.idea.util.psiElement
import cloak.mapping.descriptor.FieldType
import cloak.mapping.descriptor.parsePresentableTypeName
import cloak.mapping.rename.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType


class RemapAction : Action("Hello") {
    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
//        val editor = context.editor ?: return false
        val element = event.psiElement ?: return false
        // Only allow minecraft classes
        if (!element.packageName.startsWith("net.minecraft")) return false


        return when (element) {
            is PsiClass, is PsiField, is PsiParameter -> true
            is PsiMethod -> !element.isConstructor
            else -> false
        }
    }
    //TODO: allow going back to already submitted branches to fix after review

    //TODO: bigger screen that can fit a reason
    //TODO: use bigger screen to get git username and email input
    override fun actionPerformed(event: AnActionEvent) {
        val element = event.psiElement ?: return
        val isTopLevelClass = element is PsiClass && !element.isInnerClass
        Renamer.rename(IdeaProjectWrapper(event.project ?: return), getNameFrom(element), isTopLevelClass)
    }


}


private fun getNameFrom(psiDuck: PsiElement): Name<*> = when (psiDuck) {
    is PsiClass -> getClassNameFrom(psiDuck)
    is PsiField -> getFieldNameFrom(psiDuck)
    is PsiMethod -> getMethodNameFrom(psiDuck)
    is PsiParameter -> TODO()
    else -> error("This is only possible on classes, fields, params and methods.")
}

private val PsiElement.packageName: String
    get() = (this.containingFile as PsiJavaFile).packageName


private fun getClassNameFrom(psiClass: PsiClass): ClassName {
    val packageName = psiClass.packageName.replace(".", "/")

    var name = ClassName(
        className = psiClass.name!!,
        innerClass = null,
        packageName = packageName
    )
    var outerClass = psiClass.parent
    while (outerClass is PsiClass) {
        name = ClassName(
            outerClass.name ?: error("Wow, a class inside an anonymous class. Guess I'll die."),
            innerClass = name,
            packageName = packageName
        )
        outerClass = outerClass.parent
    }

    return name
}

private fun getFieldNameFrom(psiField: PsiField) = FieldName(
    fieldName = psiField.name, classIn = getClassNameFrom(psiField.parent as PsiClass)
)

private fun getMethodNameFrom(psiMethod: PsiMethod) = MethodName(
    methodName = psiMethod.name,
    classIn = getClassNameFrom(psiMethod.parent as PsiClass),
    parameterTypes = psiMethod.getSignature(PsiSubstitutor.EMPTY).parameterTypes
        .map {
            it as PsiClassReferenceType
            //TODO: actually check if it's generic
            FieldType.parsePresentableTypeName(it.rawType().canonicalText, isGenericType = false)
        }
)

