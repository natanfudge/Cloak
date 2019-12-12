/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cloak.idea.migrate

import com.intellij.history.LocalHistory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMigration
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.impl.migration.PsiMigrationManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import java.util.*

/**
 * @author ven
 */
class CloakMigrationProcessor @JvmOverloads constructor(
    project: Project,
    private val myMigrationMap: CloakMigrationMap,
    private val mySearchScope: GlobalSearchScope = GlobalSearchScope.projectScope(
        project
    )
) : BaseRefactoringProcessor(project) {
    private var myPsiMigration: PsiMigration?
    private var myRefsToShorten: ArrayList<SmartPsiElementPointer<PsiElement>>? = null
    override fun createUsageViewDescriptor(usages: Array<UsageInfo>): UsageViewDescriptor {
        return CloakMigrationUsagesViewDescriptor(myMigrationMap, false)
    }

    private fun startMigration(project: Project): PsiMigration {
        val migration = PsiMigrationManager.getInstance(project).startMigration()
        findOrCreateEntries(project, migration)
        return migration
    }

    private fun findOrCreateEntries(
        project: Project,
        migration: PsiMigration
    ) {
        for (entry in myMigrationMap.entries) {
            when (entry.type) {
                MigrationEntryType.Class -> CloakMigrationUtil.findOrCreateClass(project, migration, entry.oldName)
                MigrationEntryType.Method -> TODO()
                MigrationEntryType.Field -> TODO()
            }
        }
    }

    override fun refreshElements(elements: Array<PsiElement>) {
        myPsiMigration = startMigration(myProject)
    }

    override fun findUsages(): Array<UsageInfo> {
        val usagesVector = ArrayList<UsageInfo>()
        try {
            for (entry in myMigrationMap.entries) {
                val usages: Array<UsageInfo> = when (entry.type) {
                    MigrationEntryType.Class -> CloakMigrationUtil.findClassUsages(
                        myProject,
                        myPsiMigration!!,
                        entry.oldName,
                        mySearchScope
                    )
                    MigrationEntryType.Method -> TODO()
                    MigrationEntryType.Field -> TODO()
                }
                for (usage in usages) {
                    usagesVector.add(MigrationUsageInfo(usage, entry))
                }
            }
        } finally { //invalidating resolve caches without write action could lead to situations when somebody with read action resolves reference and gets ResolveResult
//then here, in another read actions, all caches are invalidated but those resolve result is used without additional checks inside that read action - but it's already invalid
            ApplicationManager.getApplication().invokeLater(
                Runnable { WriteAction.run<RuntimeException> { finishFindMigration() } },
                myProject.disposed
            )
        }
        return usagesVector.toArray(UsageInfo.EMPTY_ARRAY)
    }

    private fun finishFindMigration() {
        if (myPsiMigration != null) {
            myPsiMigration!!.finish()
            myPsiMigration = null
        }
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        if (refUsages.get().isEmpty()) {
            Messages.showInfoMessage(
                myProject,
                RefactoringBundle.message("migration.no.usages.found.in.the.project"),
                REFACTORING_NAME
            )
            return false
        }
        isPreviewUsages = true
        return true
    }

    override fun performRefactoring(usages: Array<UsageInfo>) {
        finishFindMigration()
        val psiMigration = PsiMigrationManager.getInstance(myProject).startMigration()
        val a = LocalHistory.getInstance().startAction(commandName)
        myRefsToShorten = ArrayList()
        try {
            var sameShortNames = false
            for (entry in myMigrationMap.entries) {
                val newName = entry.newName
                val element: PsiElement = when (entry.type) {
                    MigrationEntryType.Class -> CloakMigrationUtil.findOrCreateClass(myProject, psiMigration, newName)
                    MigrationEntryType.Method -> TODO()
                    MigrationEntryType.Field -> TODO()
                }

                CloakMigrationUtil.doMigration(element, newName, usages, myRefsToShorten!!)
                if (!sameShortNames && Comparing.strEqual(
                        StringUtil.getShortName(entry.oldName),
                        StringUtil.getShortName(entry.newName)
                    )
                ) {
                    sameShortNames = true
                }
            }
            if (!sameShortNames) {
                myRefsToShorten!!.clear()
            }
        } finally {
            a.finish()
            psiMigration.finish()
        }
    }

    override fun performPsiSpoilingRefactoring() {
        val styleManager = JavaCodeStyleManager.getInstance(myProject)
        for (pointer in myRefsToShorten!!) {
            val element = pointer.element
            if (element != null) {
                styleManager.shortenClassReferences(element)
            }
        }
    }

    override fun getCommandName(): String {
        return REFACTORING_NAME
    }

    internal class MigrationUsageInfo(info: UsageInfo, var mapEntry: CloakMigrationMapEntry) : UsageInfo(
        info.element!!,
        info.rangeInElement!!.startOffset,
        info.rangeInElement!!.endOffset
    )

    companion object {
        private val REFACTORING_NAME = RefactoringBundle.message("migration.title")
    }

    init {
        myPsiMigration = startMigration(project)
    }
}