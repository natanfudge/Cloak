package cloak.idea.migrate

import cloak.idea.migrate.CloakMigrationManager
import cloak.idea.util.CloakAction
import com.intellij.openapi.actionSystem.AnActionEvent

class TestMigrateAction : CloakAction() {

    private fun classMigration(old: String, new : String) = CloakMigrationMapEntry(old, new, MigrationEntryType.Class, true)


    override fun actionPerformed(e: AnActionEvent) {
        val migrationMap = CloakMigrationMap(
            name = "test maps",
            description = "a test",
            entries =
            mutableListOf(
                classMigration("net.minecraft.block.Block","net.minecraft.bleck.Renamed")
            ),
            fileName = "should prob remove this"
        )

        MigrationsDialog(e.project ?: return, mutableListOf(migrationMap)).show()
    }

}