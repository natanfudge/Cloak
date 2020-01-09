package cloak.idea.util

import com.intellij.psi.*

val PsiClass.isInnerClass get() = parent is PsiClass
val PsiModifierListOwner.isStatic get() = hasModifierProperty(PsiModifier.STATIC)
/**
 * In the case this is an override it gets whatever it overrides.
 */
fun PsiMethod.getMethodDeclaration(): PsiMethod = findDeepestSuperMethods().firstOrNull() ?: this
fun PsiParameter.getMethodIn() : PsiMethod? = this.parent.parent as? PsiMethod
