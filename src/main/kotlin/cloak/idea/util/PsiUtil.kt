package cloak.idea.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner

val PsiClass.isInnerClass get() = parent is PsiClass
val PsiModifierListOwner.isStatic get() = hasModifierProperty(PsiModifier.STATIC)