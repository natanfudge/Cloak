package cloak.idea.migrate

import cloak.idea.gui.CloakDialog
import com.intellij.ui.layout.panel
import java.util.*
import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.event.ListDataListener
import kotlin.reflect.KMutableProperty0

class MigrationsDialog(private val migrationMaps: List<CloakMigrationMap>) : CloakDialog("Migrations") {
    private var currentMigrationMap: CloakMigrationMap = migrationMaps.firstOrNull() ?: EmptyMigrationMap

    override fun createCenterPanel(): JComponent = panel {
        row(label = "Select migration map:") {
            comboBox(DefaultComboBoxModel(Vector(migrationMaps)), ::currentMigrationMap)
            button(text = "Edit...", actionListener = {})
            button(text = "New...", actionListener = {})
            button(text = "Remove",actionListener = {})
        }
    }

    init {
        init()
    }

}

private val EmptyMigrationMap =
    CloakMigrationMap(name = "No migration maps", description = "Add migration maps", entries = listOf())


private class MigrationComboBoxModel(private val migrationMaps: List<CloakMigrationMap>) :
    ComboBoxModel<CloakMigrationMap> {
    override fun setSelectedItem(anItem: Any?) {
        TODO("not implemented")
    }

    override fun getElementAt(index: Int): CloakMigrationMap {
        TODO("not implemented")
    }

    override fun getSelectedItem(): Any {
        TODO("not implemented")
    }

    override fun getSize(): Int {
        TODO("not implemented")
    }

    override fun addListDataListener(l: ListDataListener?) {
        TODO("not implemented")
    }

    override fun removeListDataListener(l: ListDataListener?) {
        TODO("not implemented")
    }

}