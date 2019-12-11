package cloak.idea.actions

import cloak.idea.util.CloakAction
import cloak.idea.util.psiElement
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.refactoring.RefactoringFactory

class TestAction  : CloakAction(){
    override fun actionPerformed(e: AnActionEvent) {
        PsiKotl
//        e.psiElement?.name
//        JavaPsiFacade.getInstance(project).findClass(qName, GlobalSearchScope.allScope(project));
//        RefactoringFactory.getInstance(e.project ?: return)
//            .createRename(e.psiElement ?: return,"foo.bar.Baz").run()
    }

}