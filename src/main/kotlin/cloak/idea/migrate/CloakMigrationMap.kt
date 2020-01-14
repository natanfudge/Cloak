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

import com.intellij.openapi.util.io.FileUtil

data class CloakMigrationMap(
    val name: String,
    val description: String,
    val entries: List<CloakMigrationMapEntry>,
    val fileName: String = FileUtil.sanitizeFileName(name, false)
) {

    override fun toString(): String {
        return name
    }
}



data class CloakMigrationMapEntry(val oldName: String, val newName: String, val type: MigrationEntryType, val recursive: Boolean) :
    Cloneable {
}

enum class MigrationEntryType{
    Class,
    Method,
    Field
}