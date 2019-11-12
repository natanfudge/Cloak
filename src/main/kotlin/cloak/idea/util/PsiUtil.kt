package cloak.idea.util

import com.intellij.psi.PsiClass

val PsiClass.isInnerClass get() = parent is PsiClass