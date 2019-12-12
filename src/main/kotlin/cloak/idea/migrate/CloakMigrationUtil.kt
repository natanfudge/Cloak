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

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.toArray
import java.util.*

object CloakMigrationUtil {
    private val LOG =
        Logger.getInstance("#com.intellij.refactoring.migration.MigrationUtil")

//    fun findPackageUsages(
//        project: Project?,
//        migration: PsiMigration,
//        qName: String?,
//        searchScope: GlobalSearchScope
//    ): Array<UsageInfo> {
//        val aPackage = findOrCreatePackage(project, migration, qName)
//        return findRefs(aPackage, searchScope)
//    }

    private fun bindNonReference(bindTo: PsiElement, element: PsiElement, usage: UsageInfo): PsiElement {
        val range: TextRange? = usage.rangeInElement
        for (reference in element.references) {
            if (reference is PsiReference) {
                if (reference.rangeInElement == range) {
                    return reference.bindToElement(bindTo)
                }
            }
        }
        return bindTo
    }

    fun findClassUsages(
        project: Project?,
        migration: PsiMigration,
        qName: String?,
        searchScope: GlobalSearchScope
    ): Array<UsageInfo> {
        val aClass = findOrCreateClass(project, migration, qName)
        return findRefs(aClass, searchScope)
    }

    private fun findRefs(aClass: PsiElement?, searchScope: GlobalSearchScope): Array<UsageInfo> {
        val results: MutableList<UsageInfo> = ArrayList()
        for (usage in ReferencesSearch.search(aClass!!, searchScope, true)) {
            results.add(UsageInfo(usage))
        }
        results.sortWith(
            Comparator.comparing<UsageInfo, String> { u: UsageInfo ->
                val file = u.virtualFile
                file?.name
            }.thenComparingInt { u: UsageInfo ->
                val range = u.navigationRange
                range?.startOffset ?: 0
            })
        return results.toArray(UsageInfo.EMPTY_ARRAY)
    }

    fun doMigration(
        elementToBind: PsiElement,
        newQName: String?,
        usages: Array<UsageInfo>,
        refsToShorten: MutableList<SmartPsiElementPointer<PsiElement>>
    ) {
        try {
            val smartPointerManager = SmartPointerManager.getInstance(elementToBind.project)
            // rename all references
            for (usage in usages) {
                if (usage is CloakMigrationProcessor.MigrationUsageInfo) {
                    if (Comparing.equal(newQName, usage.mapEntry.newName)) {
                        val element = usage.getElement()
                        if (element == null || !element.isValid) continue
                        var psiElement: PsiElement?
                        psiElement = if (element is PsiReference) {
                            element.bindToElement(elementToBind)
                        } else {
                            bindNonReference(elementToBind, element, usage)
                        }
                        if (psiElement != null) {
                            refsToShorten.add(smartPointerManager.createSmartPsiElementPointer(psiElement))
                        }
                    }
                }
            }
        } catch (e: IncorrectOperationException) { // should not happen!
            LOG.error(e)
        }
    }

//    fun findOrCreatePackage(
//        project: Project?,
//        migration: PsiMigration,
//        qName: String?
//    ): PsiPackage {
//        val aPackage = JavaPsiFacade.getInstance(project).findPackage(qName!!)
//        return aPackage
//            ?: WriteAction.compute<PsiPackage, RuntimeException> {
//                migration.createPackage(
//                    qName
//                )
//            }
//    }

    fun findOrCreateClass(
        project: Project?,
        migration: PsiMigration,
        qName: String?
    ): PsiClass {
        var aClass = JavaPsiFacade.getInstance(project).findClass(qName!!, GlobalSearchScope.allScope(project!!))
        if (aClass == null) {
            aClass = WriteAction.compute<PsiClass, RuntimeException> {
                migration.createClass(qName)
            }
        }
        return aClass!!
    }
}