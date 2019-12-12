//// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
//package cloak.idea.migrate
//
//import com.intellij.lang.java.JavaLanguage
//import com.intellij.openapi.editor.event.DocumentEvent
//import com.intellij.openapi.editor.event.DocumentListener
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.DialogWrapper
//import com.intellij.psi.*
//import com.intellij.refactoring.RefactoringBundle
//import com.intellij.refactoring.migration.MigrationMapEntry
//import com.intellij.ui.EditorTextField
//import com.intellij.ui.LanguageTextField
//import com.intellij.ui.LanguageTextField.DocumentCreator
//import com.intellij.util.ui.JBUI
//import java.awt.Dimension
//import java.awt.GridBagConstraints
//import java.awt.GridBagLayout
//import javax.swing.*
//
//class CloakEditMigrationEntryDialog(private val myProject: Project) :
//    DialogWrapper(myProject, true) {
//    private var myRbPackage: JRadioButton? = null
//    private var myRbClass: JRadioButton? = null
//    private lateinit var myOldNameField: EditorTextField
//    private lateinit var myNewNameField: EditorTextField
//    override fun getPreferredFocusedComponent(): JComponent? {
//        return myOldNameField
//    }
//
//    override fun createCenterPanel(): JComponent? {
//        return null
//    }
//
//    override fun createNorthPanel(): JComponent? {
//        val panel = JPanel(GridBagLayout())
//        val gbConstraints = GridBagConstraints()
//        gbConstraints.insets = JBUI.insets(4)
//        gbConstraints.weighty = 0.0
//        gbConstraints.gridwidth = GridBagConstraints.RELATIVE
//        gbConstraints.fill = GridBagConstraints.BOTH
//        gbConstraints.weightx = 0.0
//        myRbPackage = JRadioButton(RefactoringBundle.message("migration.entry.package"))
//        panel.add(myRbPackage, gbConstraints)
//        gbConstraints.gridwidth = GridBagConstraints.RELATIVE
//        gbConstraints.fill = GridBagConstraints.BOTH
//        gbConstraints.weightx = 0.0
//        myRbClass = JRadioButton(RefactoringBundle.message("migration.entry.class"))
//        panel.add(myRbClass, gbConstraints)
//        gbConstraints.gridwidth = GridBagConstraints.REMAINDER
//        gbConstraints.fill = GridBagConstraints.BOTH
//        gbConstraints.weightx = 1.0
//        panel.add(JPanel(), gbConstraints)
//        val buttonGroup = ButtonGroup()
//        buttonGroup.add(myRbPackage)
//        buttonGroup.add(myRbClass)
//        gbConstraints.weightx = 0.0
//        gbConstraints.gridwidth = GridBagConstraints.RELATIVE
//        gbConstraints.fill = GridBagConstraints.NONE
//        val oldNamePrompt =
//            JLabel(RefactoringBundle.message("migration.entry.old.name"))
//        panel.add(oldNamePrompt, gbConstraints)
//        gbConstraints.gridwidth = GridBagConstraints.REMAINDER
//        gbConstraints.fill = GridBagConstraints.HORIZONTAL
//        gbConstraints.weightx = 1.0
//        val documentCreator = DocumentCreator { value, language, project ->
//            val defaultPackage = JavaPsiFacade.getInstance(project).findPackage("")
//            val fragment: JavaCodeFragment = JavaCodeFragmentFactory.getInstance(project)
//                .createReferenceCodeFragment("", defaultPackage, true, true)
//            PsiDocumentManager.getInstance(project).getDocument(fragment)!!
//        }
//        myOldNameField = LanguageTextField(JavaLanguage.INSTANCE, myProject, "", documentCreator)
//        panel.add(myOldNameField, gbConstraints)
//        gbConstraints.weightx = 0.0
//        gbConstraints.gridwidth = GridBagConstraints.RELATIVE
//        gbConstraints.fill = GridBagConstraints.NONE
//        val newNamePrompt =
//            JLabel(RefactoringBundle.message("migration.entry.new.name"))
//        panel.add(newNamePrompt, gbConstraints)
//        gbConstraints.gridwidth = GridBagConstraints.REMAINDER
//        gbConstraints.fill = GridBagConstraints.HORIZONTAL
//        gbConstraints.weightx = 1.0
//        myNewNameField = LanguageTextField(JavaLanguage.INSTANCE, myProject, "", documentCreator)
//        panel.preferredSize = Dimension(300, panel.preferredSize.height)
//        panel.add(myNewNameField, gbConstraints)
//        val documentAdapter: DocumentListener =
//            object : DocumentListener {
//                override fun documentChanged(e: DocumentEvent) {
//                    validateOKButton()
//                }
//            }
//        myOldNameField.getDocument().addDocumentListener(documentAdapter)
//        myNewNameField.getDocument().addDocumentListener(documentAdapter)
//        return panel
//    }
//
//    private fun validateOKButton() {
//        var isEnabled = true
//        var text = myOldNameField!!.text
//        text = text.trim { it <= ' ' }
//        val manager = PsiManager.getInstance(myProject)
//        if (!PsiNameHelper.getInstance(manager.project).isQualifiedName(text)) {
//            isEnabled = false
//        }
//        text = myNewNameField!!.text
//        text = text.trim { it <= ' ' }
//        if (!PsiNameHelper.getInstance(manager.project).isQualifiedName(text)) {
//            isEnabled = false
//        }
//        isOKActionEnabled = isEnabled
//    }
//
//    fun setEntry(entry: MigrationMapEntry) {
//        myOldNameField!!.text = entry.oldName
//        myNewNameField!!.text = entry.newName
//        myRbPackage!!.isSelected = entry.type == MigrationMapEntry.PACKAGE
//        myRbClass!!.isSelected = entry.type == MigrationMapEntry.CLASS
//        validateOKButton()
//    }
//
//    fun updateEntry(entry: MigrationMapEntry) {
//        entry.oldName = myOldNameField!!.text.trim { it <= ' ' }
//        entry.newName = myNewNameField!!.text.trim { it <= ' ' }
//        if (myRbPackage!!.isSelected) {
//            entry.type = MigrationMapEntry.PACKAGE
//            entry.isRecursive = true
//        } else {
//            entry.type = MigrationMapEntry.CLASS
//        }
//    }
//
//    init {
//        title = RefactoringBundle.message("edit.migration.entry.title")
//        horizontalStretch = 1.2f
//        init()
//    }
//}