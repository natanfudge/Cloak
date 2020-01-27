package cloak.idea.migrate

import cloak.idea.gui.CloakDialog
import com.intellij.openapi.project.Project
import com.intellij.refactoring.migration.MigrationDialog
import com.intellij.refactoring.migration.MigrationMap
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import java.io.IOException
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

class MigrationsDialog(private val project: Project, migrationMaps: MutableList<CloakMigrationMap>) :
    CloakDialog(
        project, "Migrations", helpId = "Cloak-Migration"
    ) {
     var currentMigrationMap: CloakMigrationMap = migrationMaps.firstOrNull() ?: EmptyMigrationMap

    private val comboBoxModel = DefaultComboBoxModel(Vector(migrationMaps))

    private val panel = panel {
        row(label = "Select migration map:") {
            cell(isFullWidth = true) {
                comboBox(
                    comboBoxModel,
                    ::currentMigrationMap,
                    growPolicy = GrowPolicy.MEDIUM_TEXT
                )
                button(text = "Edit...", constraints = *arrayOf(CCFlags.growX), actionListener = {
                    editMap(currentMigrationMap)?.let { currentMigrationMap = it }
                })
                button(text = "New...", constraints = *arrayOf(CCFlags.growX), actionListener = {
                    addNewMap()
                })
                button(text = "Remove", constraints = *arrayOf(CCFlags.growX), actionListener = { removeMap() })
            }

        }
        row {
            label(currentMigrationMap.description)
        }
    }

//    private val mapsComboBox =

    override fun createCenterPanel(): JComponent  = panel

    init {
        init()

        setOKButtonText("Run")
    }

    private fun editMap(map: CloakMigrationMap): CloakMigrationMap? {
        val dialog = CloakEditMigrationDialog(project, map)
        if (!dialog.showAndGet()) {
            return null
        }
        return map.copy(name = dialog.name, description = dialog.description)
    }

    private fun addNewMap() {
        val edited = editMap(CloakMigrationMap(name = "", description = "", entries = mutableListOf()))
        if (edited != null) {
            comboBoxModel.addElement(edited)
            currentMigrationMap = edited
            comboBoxModel.selectedItem = edited
            panel.apply()
            //TODO
//                myMigrationMapSet.saveMaps()
        }
    }

    private fun removeMap() {
        comboBoxModel.removeElement(currentMigrationMap)
        if (comboBoxModel.size > 0) {
            currentMigrationMap = comboBoxModel.getElementAt(0)
        }
        //TODO
//                myMigrationMapSet.saveMaps()
    }

    override fun doOKAction() {
        CloakMigrationProcessor(project, currentMigrationMap).run()
        close(0)
    }

}

private val EmptyMigrationMap = CloakMigrationMap(name = "No migration maps", description = "Add migration maps", entries = mutableListOf())


