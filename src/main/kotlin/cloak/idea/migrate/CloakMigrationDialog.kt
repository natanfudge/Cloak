//// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
//package cloak.idea.migrate
//
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.DialogWrapper
//import com.intellij.refactoring.HelpID
//import com.intellij.refactoring.RefactoringBundle
//import com.intellij.refactoring.migration.MigrationMapSet
//import com.intellij.ui.layout.panel
//import javax.swing.JComponent
//
//class CloakMigrationDialog(
//    private val myProject: Project,
//    private val myMigrationMapSet: MigrationMapSet
//) : DialogWrapper(myProject, true) {
//    private val panel = panel {
//
//    }
//
//    //    private val myMapComboBox: JComboBox<MigrationMap> = JComboBox()
////    private  var myDescriptionTextArea: JTextArea? = null
////    private val myEditMapButton: JButton = JButton()
////    private val myNewMapButton: JButton = JButton()
////    private val myRemoveMapButton: JButton= JButton()
////    private val promptLabel: JLabel = label
////    private val mySeparator: JSeparator = JSeparator()
////    private val myDescriptionScroll: JScrollPane = JScrollPane()
//    override fun getHelpId(): String? {
//        return HelpID.MIGRATION
//    }
//
//    override fun getPreferredFocusedComponent(): JComponent? {
//        return panel
//    }
//
//    override fun createCenterPanel(): JComponent? {
//
////        initMapCombobox()
////        myDescriptionTextArea = MyTextArea("", 10, 40)
////        myDescriptionScroll.viewport.add(myDescriptionTextArea)
////        myDescriptionScroll.border = null
////        myDescriptionScroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
////        myDescriptionScroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
////        myDescriptionTextArea!!.setEditable(false)
////        myDescriptionTextArea!!.setFont(promptLabel!!.font)
////        myDescriptionTextArea!!.setBackground(myPanel!!.background)
////        myDescriptionTextArea!!.setLineWrap(true)
////        updateDescription()
////        myMapComboBox!!.addActionListener { updateDescription() }
////        myEditMapButton!!.addActionListener { editMap() }
////        myRemoveMapButton!!.addActionListener { removeMap() }
////        myNewMapButton!!.addActionListener { addNewMap() }
////        myMapComboBox.registerKeyboardAction(
////            {
////                if (myMapComboBox.isPopupVisible) {
////                    myMapComboBox.setPopupVisible(false)
////                } else {
////                    clickDefaultButton()
////                }
////            },
////            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
////            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
////        )
//        return panel
//    }
//
////    private fun updateDescription() {
////        if (myDescriptionTextArea == null) {
////            return
////        }
////        val map = migrationMap
////        if (map == null) {
////            myDescriptionTextArea!!.text = ""
////            return
////        }
////        myDescriptionTextArea!!.text = map.description
////    }
//
////    private fun editMap() {
////        val oldMap = migrationMap ?: return
////        val newMap = oldMap.cloneMap()
////        if (editMap(newMap)) {
////            myMigrationMapSet.replaceMap(oldMap, newMap)
////            initMapCombobox()
////            myMapComboBox!!.selectedItem = newMap
////            try {
////                myMigrationMapSet.saveMaps()
////            } catch (e: IOException) {
////                LOG.error("Cannot save migration maps", e)
////            }
////        }
////    }
//
////    private fun editMap(map: MigrationMap?): Boolean {
////        if (map == null) return false
////        val dialog = CloakEditMigrationDialog(myProject, map)
////        if (!dialog.showAndGet()) {
////            return false
////        }
////        map.name = dialog.name
////        map.description = dialog.description
////        return true
////    }
//
////    private fun addNewMap() {
////        val migrationMap = MigrationMap()
////        if (editMap(migrationMap)) {
////            myMigrationMapSet.addMap(migrationMap)
////            initMapCombobox()
////            myMapComboBox!!.selectedItem = migrationMap
////            try {
////                myMigrationMapSet.saveMaps()
////            } catch (e: IOException) {
////                LOG.error("Cannot save migration maps", e)
////            }
////        }
////    }
//
////    private fun removeMap() {
////        val map = migrationMap ?: return
////        myMigrationMapSet.removeMap(map)
////        val maps = myMigrationMapSet.maps
////        initMapCombobox()
////        if (maps.size > 0) {
////            myMapComboBox!!.selectedItem = maps[0]
////        }
////        try {
////            myMigrationMapSet.saveMaps()
////        } catch (e: IOException) {
////            LOG.error("Cannot save migration maps", e)
////        }
////    }
////
////    val migrationMap: MigrationMap
////        get() = myMapComboBox!!.selectedItem as MigrationMap
////
////    private fun initMapCombobox() {
////        if (myMapComboBox!!.itemCount > 0) {
////            myMapComboBox.removeAllItems()
////        }
////        val maps = myMigrationMapSet.maps
////        for (map in maps) {
////            myMapComboBox.addItem(map)
////        }
////        updateDescription()
////    }
//
////    companion object {
////        private val LOG =
////            Logger.getInstance("#com.intellij.refactoring.migration.MigrationDialog")
////    }
//
//    init {
//        title = RefactoringBundle.message("migration.dialog.title")
//        horizontalStretch = 1.2f
//        setOKButtonText(RefactoringBundle.message("migration.dialog.ok.button.text"))
//        init()
//    }
//}