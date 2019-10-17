package cloak

import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.listeners.UndoRefactoringElementListener

class RemappingRefactoringListener(element: PsiElement?) : RefactoringElementListener,
    UndoRefactoringElementListener {
    override fun elementMoved(newElement: PsiElement) {}
    override fun elementRenamed(newElement: PsiElement) {}
    override fun undoElementMovedOrRenamed(newElement: PsiElement, oldQualifiedName: String) {}
}