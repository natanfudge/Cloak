// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cloak.idea.migrate

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.refactoring.RefactoringBundle
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableModel

class CloakEditMigrationDialog(private val myProject: Project, private val migrationMap: CloakMigrationMap) :
    DialogWrapper(myProject, true) {
    private lateinit var table: JBTable
    private var myNameField: JTextField? = null
    private lateinit var myDescriptionTextArea: JTextArea
    override fun getPreferredFocusedComponent(): JComponent? {
        return myNameField
    }

    private fun validateOKButton() {
        var isEnabled = true
        if (myNameField!!.text.trim { it <= ' ' }.isEmpty()) {
            isEnabled = false
        } else if (migrationMap.entries.isEmpty()) {
            isEnabled = false
        }
        isOKActionEnabled = isEnabled
    }

    val name: String
        get() = myNameField!!.text

    val description: String
        get() = myDescriptionTextArea.text

    override fun createNorthPanel(): JComponent? {
        myNameField = JTextField(migrationMap.name)
        myNameField!!.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                validateOKButton()
            }
        })
        myDescriptionTextArea = object : JTextArea(migrationMap.description, 3, 40) {
            override fun getMinimumSize(): Dimension {
                return super.getPreferredSize()
            }
        }
        myDescriptionTextArea.lineWrap = true
        myDescriptionTextArea.wrapStyleWord = true
        myDescriptionTextArea.font = myNameField!!.font
        myDescriptionTextArea.background = myNameField!!.background
        val scrollPane = ScrollPaneFactory.createScrollPane(myDescriptionTextArea)
        scrollPane.border = myNameField!!.border
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JLabel(RefactoringBundle.message("migration.map.name.prompt")),
                myNameField!!
            )
            .addLabeledComponent(
                JLabel(RefactoringBundle.message("migration.map.description.label")),
                scrollPane
            )
            .addVerticalGap(UIUtil.LARGE_VGAP).panel
    }

    override fun createCenterPanel(): JComponent? {
        return ToolbarDecorator.createDecorator(createTable())
            .setAddAction {
                addRow()
                validateOKButton()
            }.setRemoveAction {
                removeRow()
                validateOKButton()
            }.setEditAction { edit() }.setMoveUpAction { moveUp() }
            .setMoveDownAction { moveDown() }.createPanel()
    }

    private fun edit() {
        val dialog = CloakEditMigrationEntryDialog(myProject)
        val selected = table.selectedRow
        if (selected < 0) return
        val entry = migrationMap.entries[selected]
        dialog.setEntry(entry)
        if (!dialog.showAndGet()) {
            return
        }
        val newEntry = dialog.updateEntry(entry)
        migrationMap.entries[selected] = newEntry
        val model = table.model as AbstractTableModel
        model.fireTableRowsUpdated(selected, selected)
    }

    private fun addRow() {
        val dialog = CloakEditMigrationEntryDialog(myProject)
        val entry = CloakMigrationMapEntry(oldName = "", newName = "", recursive = false, type = MigrationEntryType.Class)
        dialog.setEntry(entry)
        if (!dialog.showAndGet()) {
            return
        }
        migrationMap.entries.add(dialog.updateEntry(entry))
        val model = table.model as AbstractTableModel
        val lastPos = migrationMap.entries.count() - 1
        model.fireTableRowsInserted(lastPos, lastPos)
        table.setRowSelectionInterval(lastPos, lastPos)
    }

    private fun removeRow() {
        var selected = table.selectedRow
        if (selected < 0) return
        migrationMap.entries.removeAt(selected)
        val model = table.model as AbstractTableModel
        model.fireTableRowsDeleted(selected, selected)
        if (selected >= migrationMap.entries.count()) {
            selected--
        }
        if (selected >= 0) {
            table.setRowSelectionInterval(selected, selected)
        }
    }

    private fun moveUp() {
        val selected = table.selectedRow
        if (selected < 1) return
        val entry = migrationMap.entries[selected]
        val previousEntry = migrationMap.entries[selected - 1]
        migrationMap.entries[selected] = previousEntry
        migrationMap.entries[selected - 1] = entry
        val model = table.model as AbstractTableModel
        model.fireTableRowsUpdated(selected - 1, selected)
        table.setRowSelectionInterval(selected - 1, selected - 1)
    }

    private fun moveDown() {
        val selected = table.selectedRow
        if (selected >= migrationMap.entries.count() - 1) return
        val entry = migrationMap.entries[selected]
        val nextEntry = migrationMap.entries[selected + 1]
        migrationMap.entries[selected] = nextEntry
        migrationMap.entries[selected + 1] = entry
        val model = table.model as AbstractTableModel
        model.fireTableRowsUpdated(selected, selected + 1)
        table.setRowSelectionInterval(selected + 1, selected + 1)
    }

    private fun createTable(): JBTable {
        val names = arrayOf(
            RefactoringBundle.message("migration.type.column.header"),
            RefactoringBundle.message("migration.old.name.column.header"),
            RefactoringBundle.message("migration.new.name.column.header")
        )
        // Create a model of the data.
        val dataModel: TableModel = object : AbstractTableModel() {
            override fun getColumnCount(): Int {
                return 3
            }

            override fun getRowCount(): Int {
                return migrationMap.entries.count()
            }

            override fun getValueAt(row: Int, col: Int): Any {
                val entry = migrationMap.entries[row]
                if (col == 0) {
                    return RefactoringBundle.message("migration.class")
//                    if (entry.type == CloakMigrationMapEntry.PACKAGE && entry.isRecursive) {
//                        RefactoringBundle.message("migration.package.with.subpackages")
//                    } else if (entry.type == CloakMigrationMapEntry.PACKAGE && !entry.isRecursive) {
//                        RefactoringBundle.message("migration.package")
//                    } else {
//                    RefactoringBundle.message("migration.class")
//                    }
                }
                return if (col == 1) {
                    entry.oldName
                } else {
                    entry.newName
                }
            }

            override fun getColumnName(column: Int): String {
                return names[column]
            }

            override fun getColumnClass(c: Int): Class<*> {
                return String::class.java
            }

            override fun isCellEditable(row: Int, col: Int): Boolean {
                return false
            }

            override fun setValueAt(aValue: Any, row: Int, column: Int) {}
        }
        // Create the table
        table = JBTable(dataModel)
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.preferredScrollableViewportSize = Dimension(300, table.rowHeight * 10)
        return table
    }

    init {
        horizontalStretch = 1.2f
        title = RefactoringBundle.message("edit.migration.map.title")
        init()
        validateOKButton()
    }
}