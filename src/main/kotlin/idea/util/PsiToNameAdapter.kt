package cloak.idea.util

import cloak.mapping.descriptor.FieldType
import cloak.mapping.descriptor.parsePresentableTypeName
import cloak.mapping.mappings.ConstructorName
import cloak.mapping.mappings.Joiner
import cloak.mapping.rename.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.parents

/**
 * Returns null when:
 * - This is a parameter in a lambda
 * - This is an anonymous class
 */
fun PsiElement.asNameOrNull(): Name? = when (this) {
    is PsiClass -> getClassName()
    is PsiField -> getFieldName()
    is PsiMethod -> getMethodName()
    // avoid getting parameter of lambdas
    is PsiParameter -> if (parent.parent is PsiMethod) getParameterName() else null
    else -> null
}

fun PsiElement.asName(): Name =
    asNameOrNull() ?: error("This is only possible on named classes, fields, params in normal methods and methods.")

val PsiElement.packageName: String
    get() = (this.containingFile as PsiJavaFile).packageName


private fun PsiClass.getClassName(): ClassName? {
    val packageName = packageName.replace(".", "/")

    var name = ClassName(
        className = name ?: return null,
        innerClass = null,
        packageName = packageName
    )
    var outerClass = parent
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

private fun PsiField.getFieldName() = (parent as PsiClass).getClassName()?.let {
    FieldName(fieldName = name, classIn = it)
}


private fun PsiMethod.getMethodName(): MethodName? {
    val superMethods = findSuperMethods()
    val methodOwner = (superMethods.firstOrNull()?.parent ?: this.parent) as PsiClass
    return methodOwner.getClassName()?.let { className ->
        MethodName(
            methodName = if(this.isConstructor) ConstructorName else name,
            classIn = className,
            parameterTypes = getSignature(PsiSubstitutor.EMPTY).parameterTypes.map {
                val name = (if (it is PsiClassReferenceType) it.resolveName() else it.canonicalText) ?: return null
                FieldType.parsePresentableTypeName(name)
            }
        )
    }
}



private fun PsiClassReferenceType.resolveName(): String? {
    val resolved = rawType().resolve() ?: error("Could not resolve type: $this")
    val parents = resolved.parents().filterIsInstance<PsiClass>().toList().reversed()
    // We use the full name for the outer class, and only the short name for the inner class.
    var className = parents.first().qualifiedName
    for (innerClass in parents.drop(1)) className += Joiner.InnerClass + innerClass.name

    return className
}

private fun PsiParameter.getIndex() = (parent as PsiParameterList).getParameterIndex(this)
private fun PsiParameter.getParameterName(): ParamName? = (this.parent.parent as PsiMethod).getMethodName()?.let {
    ParamName(index = this.getIndex(), methodIn = it)
}
