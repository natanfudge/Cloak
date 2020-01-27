package idea

import cloak.idea.migrate.CloakMigrationMap
import cloak.idea.migrate.CloakMigrationMapEntry
import cloak.idea.migrate.CloakMigrationProcessor
import cloak.idea.migrate.MigrationEntryType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class MigrateTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String {
        return "testdata"
    }

    private fun classMigration(old: String, new : String) = CloakMigrationMapEntry(old, new, MigrationEntryType.Class, true)

    @Test
    fun classTest(){
        myFixture.copyFileToProject("MigrateClass.kt")
        val migrationMap = CloakMigrationMap(
            name = "test maps",
            description = "a test",
            entries = mutableListOf(classMigration("foo.bar.Baz","one.two.three.Four")),
            fileName = "should prob remove this"
        )
        CloakMigrationProcessor(myFixture.project, migrationMap).run()
    }
}