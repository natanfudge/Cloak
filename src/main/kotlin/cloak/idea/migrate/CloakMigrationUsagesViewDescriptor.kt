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

import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor

internal class CloakMigrationUsagesViewDescriptor(
    val migrationMap: CloakMigrationMap,
    private val isSearchInComments: Boolean
) : UsageViewDescriptor {

    override fun getElements(): Array<PsiElement> {
        return PsiElement.EMPTY_ARRAY
    }

    override fun getProcessedElementsHeader(): String? {
        return null
    }

    override fun getCodeReferencesText(usagesCount: Int, filesCount: Int): String {
        return RefactoringBundle.message(
            "references.in.code.to.elements.from.migration.map", migrationMap.name,
            UsageViewBundle.getReferencesString(usagesCount, filesCount)
        )
    }

    override fun getCommentReferencesText(usagesCount: Int, filesCount: Int): String? {
        return null
    }

    val info: String
        get() = RefactoringBundle.message("press.the.do.migrate.button", migrationMap.name)

}