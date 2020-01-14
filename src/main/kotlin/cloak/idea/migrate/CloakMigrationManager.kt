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

import com.intellij.openapi.project.Project

class CloakMigrationManager(private val myProject: Project) {

    private fun test(str: String) = CloakMigrationMapEntry(str, "", MigrationEntryType.Class, true)

    //    private val myMigrationMapSet = CloakMigrationMapSet()
    fun showMigrationDialog() {
        val migrationMap = CloakMigrationMap(
            name = "test maps",
            description = "a test",
            entries =
            listOf(
//                CloakMigrationMapEntry("java.io.File", "java.io.Nigga", MigrationEntryType.Class, true)
//            ,CloakMigrationMapEntry("java.lang.String", "java.nigga.String", MigrationEntryType.Class, true)
//            ,
//                CloakMigrationMapEntry("java.shit.Path", "java.nio.file.Path", MigrationEntryType.Class, true),
//                CloakMigrationMapEntry("java.io.Foo", "java.io.File", MigrationEntryType.Class, true),
//                CloakMigrationMapEntry("java.nio.Path", "", MigrationEntryType.Class, true),
//                CloakMigrationMapEntry("java.nio.bar.Path", "", MigrationEntryType.Class, true),
//                CloakMigrationMapEntry("java.io.bar.File", "", MigrationEntryType.Class, true),
//                CloakMigrationMapEntry("java.bar.File", "", MigrationEntryType.Class, true)

                test("fuck.minecraft.block.Bleak"),
                test("foo.Bar"),
                test("foo.bar.b"),
                test("foo.bar.ba"),
                test("foo.bar.baz"),
                test("foo.bar.baz2"),
                test("foo.bar.baz23"),
                test("foo.bar.baz234"),
                test("foo.bar.baz2345"),
                test("foo.bar.baz23456"),
                test("foo.bar.baz234567")


            ),
            fileName = "should prob remove this"
        )



        MigrationsDialog(listOf(migrationMap)).show()
//        val migrationDialog = CloakMigrationDialog(myProject, myMigrationMapSet)
//        if (!migrationDialog.showAndGet()) {
//            return
//        }

//        val x = Messages.showInputDialog("")
//        println("asdfsdf")
//
//        CloakMigrationProcessor(myProject, migrationMap).run()
    }
//
//    fun findMigrationMap(name: String): CloakMigrationMap? {
//        return myMigrationMapSet.findMigrationMap(name)
//    }

}