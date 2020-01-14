package cloak.idea.migrate

import cloak.idea.migrate.CloakMigrationManager
import cloak.idea.util.CloakAction
import com.intellij.openapi.actionSystem.AnActionEvent

class TestMigrateAction : CloakAction() {
    override fun actionPerformed(e: AnActionEvent) {
        CloakMigrationManager(e.project ?: return).showMigrationDialog()
//        PsiKotl
//        e.psiElement?.name
//        JavaPsiFacade.getInstance(project).findClass(qName, GlobalSearchScope.allScope(project));
//        RefactoringFactory.getInstance(e.project ?: return)
//            .createRename(e.psiElement ?: return,"foo.bar.Baz").run()
    }

}