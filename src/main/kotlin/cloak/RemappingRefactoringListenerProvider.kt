package cloak

import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider

class RemappingRefactoringListenerProvider : RefactoringElementListenerProvider {
    override fun getListener(element: PsiElement): RefactoringElementListener? {
        return RemappingRefactoringListener(element)
    }
}