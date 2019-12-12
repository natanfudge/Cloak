//// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
//package cloak.idea.migrate
//
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.DialogWrapper
//import com.intellij.refactoring.RefactoringBundle
//import com.intellij.ui.DocumentAdapter
//import com.intellij.ui.ScrollPaneFactory
//import com.intellij.ui.ToolbarDecorator
//import com.intellij.ui.table.JBTable
//import com.intellij.util.ui.FormBuilder
//import com.intellij.util.ui.UIUtil
//import java.awt.Dimension
//import javax.swing.*
//import javax.swing.event.DocumentEvent
//import javax.swing.table.AbstractTableModel
//import javax.swing.table.TableModel
//
//class CloakEditMigrationDialog(private val myProject: Project, private val myMigrationMap: CloakMigrationMap) :
//    DialogWrapper(myProject, true) {
//    private lateinit var myTable: JBTable
//    private var myNameField: JTextField? = null
//    private lateinit var myDescriptionTextArea: JTextArea
//    override fun getPreferredFocusedComponent(): JComponent? {
//        return myNameField
//    }
//
//    private fun validateOKButton() {
//        var isEnabled = true
//        if (myNameField!!.text.trim { it <= ' ' }.length == 0) {
//            isEnabled = false
//        } else if (myMigrationMap.entryCount == 0) {
//            isEnabled = false
//        }
//        isOKActionEnabled = isEnabled
//    }
//
//    val name: String
//        get() = myNameField!!.text
//
//    val description: String
//        get() = myDescriptionTextArea!!.text
//
//    override fun createNorthPanel(): JComponent? {
//        myNameField = JTextField(myMigrationMap.name)
//        myNameField!!.document.addDocumentListener(object : DocumentAdapter() {
//            override fun textChanged(e: DocumentEvent) {
//                validateOKButton()
//            }
//        })
//        myDescriptionTextArea = object : JTextArea(myMigrationMap.description, 3, 40) {
//            override fun getMinimumSize(): Dimension {
//                return super.getPreferredSize()
//            }
//        }
//        myDescriptionTextArea.setLineWrap(true)
//        myDescriptionTextArea.setWrapStyleWord(true)
//        myDescriptionTextArea.setFont(myNameField!!.font)
//        myDescriptionTextArea.setBackground(myNameField!!.background)
//        val scrollPane = ScrollPaneFactory.createScrollPane(myDescriptionTextArea)
//        scrollPane.border = myNameField!!.border
//        return FormBuilder.createFormBuilder()
//            .addLabeledComponent(
//                JLabel(RefactoringBundle.message("migration.map.name.prompt")),
//                myNameField!!
//            )
//            .addLabeledComponent(
//                JLabel(RefactoringBundle.message("migration.map.description.label")),
//                scrollPane
//            )
//            .addVerticalGap(UIUtil.LARGE_VGAP).panel
//    }
//
//    override fun createCenterPanel(): JComponent? {
//        return ToolbarDecorator.createDecorator(createTable())
//            .setAddAction {
//                addRow()
//                validateOKButton()
//            }.setRemoveAction {
//                removeRow()
//                validateOKButton()
//            }.setEditAction { edit() }.setMoveUpAction { moveUp() }
//            .setMoveDownAction { moveDown() }.createPanel()
//    }
//
//    private fun edit() {
//        val dialog = CloakEditMigrationEntryDialog(myProject)
//        val selected = myTable!!.selectedRow
//        if (selected < 0) return
//        val entry = myMigrationMap.getEntryAt(selected)
//        dialog.setEntry(entry)
//        if (!dialog.showAndGet()) {
//            return
//        }
//        dialog.updateEntry(entry)
//        val model = myTable!!.model as AbstractTableModel
//        model.fireTableRowsUpdated(selected, selected)
//    }
//
//    private fun addRow() {
//        val dialog = CloakEditMigrationEntryDialog(myProject)
//        val entry = CloakMigrationMapEntry()
//        dialog.setEntry(entry)
//        if (!dialog.showAndGet()) {
//            return
//        }
//        dialog.updateEntry(entry)
//        myMigrationMap.addEntry(entry)
//        val model = myTable!!.model as AbstractTableModel
//        model.fireTableRowsInserted(myMigrationMap.entryCount - 1, myMigrationMap.entryCount - 1)
//        myTable!!.setRowSelectionInterval(myMigrationMap.entryCount - 1, myMigrationMap.entryCount - 1)
//    }
//
//    private fun removeRow() {
//        var selected = myTable!!.selectedRow
//        if (selected < 0) return
//        myMigrationMap.removeEntryAt(selected)
//        val model = myTable!!.model as AbstractTableModel
//        model.fireTableRowsDeleted(selected, selected)
//        if (selected >= myMigrationMap.entryCount) {
//            selected--
//        }
//        if (selected >= 0) {
//            myTable!!.setRowSelectionInterval(selected, selected)
//        }
//    }
//
//    private fun moveUp() {
//        val selected = myTable!!.selectedRow
//        if (selected < 1) return
//        val entry = myMigrationMap.getEntryAt(selected)
//        val previousEntry = myMigrationMap.getEntryAt(selected - 1)
//        myMigrationMap.setEntryAt(previousEntry, selected)
//        myMigrationMap.setEntryAt(entry, selected - 1)
//        val model = myTable!!.model as AbstractTableModel
//        model.fireTableRowsUpdated(selected - 1, selected)
//        myTable!!.setRowSelectionInterval(selected - 1, selected - 1)
//    }
//
//    private fun moveDown() {
//        val selected = myTable!!.selectedRow
//        if (selected >= myMigrationMap.entryCount - 1) return
//        val entry = myMigrationMap.getEntryAt(selected)
//        val nextEntry = myMigrationMap.getEntryAt(selected + 1)
//        myMigrationMap.setEntryAt(nextEntry, selected)
//        myMigrationMap.setEntryAt(entry, selected + 1)
//        val model = myTable!!.model as AbstractTableModel
//        model.fireTableRowsUpdated(selected, selected + 1)
//        myTable!!.setRowSelectionInterval(selected + 1, selected + 1)
//    }
//
//    private fun createTable(): JBTable {
//        val names = arrayOf(
//            RefactoringBundle.message("migration.type.column.header"),
//            RefactoringBundle.message("migration.old.name.column.header"),
//            RefactoringBundle.message("migration.new.name.column.header")
//        )
//        // Create a model of the data.
//        val dataModel: TableModel = object : AbstractTableModel() {
//            override fun getColumnCount(): Int {
//                return 3
//            }
//
//            override fun getRowCount(): Int {
//                return myMigrationMap.entryCount
//            }
//
//            override fun getValueAt(row: Int, col: Int): Any {
//                val entry = myMigrationMap.getEntryAt(row)
//                if (col == 0) {
//                    return if (entry.type == CloakMigrationMapEntry.PACKAGE && entry.isRecursive) {
//                        RefactoringBundle.message("migration.package.with.subpackages")
//                    } else if (entry.type == CloakMigrationMapEntry.PACKAGE && !entry.isRecursive) {
//                        RefactoringBundle.message("migration.package")
//                    } else {
//                        RefactoringBundle.message("migration.class")
//                    }
//                }
//                val suffix = if (entry.type == CloakMigrationMapEntry.PACKAGE) ".*" else ""
//                return if (col == 1) {
//                    entry.oldName + suffix
//                } else {
//                    entry.newName + suffix
//                }
//            }
//
//            override fun getColumnName(column: Int): String {
//                return names[column]
//            }
//
//            override fun getColumnClass(c: Int): Class<*> {
//                return String::class.java
//            }
//
//            override fun isCellEditable(row: Int, col: Int): Boolean {
//                return false
//            }
//
//            override fun setValueAt(aValue: Any, row: Int, column: Int) {}
//        }
//        // Create the table
//        myTable = JBTable(dataModel)
//        myTable!!.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
//        myTable!!.preferredScrollableViewportSize = Dimension(300, myTable!!.rowHeight * 10)
//        return myTable
//    }
//
//    init {
//        horizontalStretch = 1.2f
//        title = RefactoringBundle.message("edit.migration.map.title")
//        init()
//        validateOKButton()
//    }
//}