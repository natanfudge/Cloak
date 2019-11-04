package cloak.idea.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.openapi.ui.messages.MessageDialog
import com.intellij.ui.DocumentAdapter
import com.intellij.util.ui.JBInsets
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.Document
import javax.swing.text.JTextComponent

fun showTwoInputsDialog(
    project: Project,
    message: String?,
    title: String,
    descriptionA: String?,
    descriptionB: String?,
    icon: Icon = CommonIcons.Question,
    initialValueA: String? = null,
    initialValueB: String? = null,
    validatorA: InputValidator? = null,
    validatorB: InputValidator? = null
): Pair<String, String>? {

    val dialog = TwoInputsDialog(
        project,
        message,
        descriptionA,
        descriptionB,
        title,
        icon,
        initialValueA,
        initialValueB,
        validatorA,
        validatorB,
        arrayOf(
            Messages.OK_BUTTON,
            Messages.CANCEL_BUTTON
        ),
        0
    )

    dialog.show()

    val inputA = dialog.inputStringA
    val inputB = dialog.inputStringB
    return if (inputA != null && inputB != null) inputA to inputB
    else null
}

class TwoInputsDialog(
    project: Project?,
    message: String?,
    private val descriptionA: String?,
    private val descriptionB: String?,
    title: String?,
    icon: Icon?,
    initialValueA: String?,
    initialValueB: String?,
    private val validatorA: InputValidator?,
    private val validatorB: InputValidator?,
    options: Array<String?>,
    defaultOption: Int
) : MessageDialog(project, true) {
    lateinit var textFieldA: JTextComponent
        private set

    lateinit var textFieldB: JTextComponent
        private set

    init {
        _init(title, message, options, defaultOption, -1, icon, null)
        textFieldA.text = initialValueA
        textFieldB.text = initialValueB
        enableOkAction()
    }

    private fun enableOkAction() {
        okAction.isEnabled = true
        if (validatorA != null && !validatorA.checkInput(textFieldA.text.trim { it <= ' ' })) okAction.isEnabled = false
        if (validatorB != null && !validatorB.checkInput(textFieldB.text.trim { it <= ' ' })) okAction.isEnabled = false
    }

    private fun Document.onTextChange(callback: (DocumentEvent) -> Unit) = addDocumentListener(
        object : DocumentAdapter() {
            public override fun textChanged(event: DocumentEvent) {
                callback(event)
            }
        })

    override fun createActions(): Array<Action?> {
        val actions = arrayOfNulls<Action>(myOptions.size)
        for (i in myOptions.indices) {
            val option = myOptions[i]
            if (i == 0) { // "OK" is default button. It has index 0.
                actions[0] = okAction
                actions[0]!!.putValue(DialogWrapper.DEFAULT_ACTION, true)

                fun validateInput(textField: JTextComponent, validator: InputValidator?) {
                    val text = textField.text.trim { it <= ' ' }
                    if (validator != null) {
                        if (!validator.checkInput(text)) {
                            actions[i]!!.isEnabled = false
                        }

                        if (validator is InputValidatorEx) {
                            val errorText = validator.getErrorText(text)
                            setErrorText(errorText, textField)
                            if (errorText != null) actions[i]!!.isEnabled = false
                        }
                    }
                }

                fun validateBoth() {
                    actions[i]!!.isEnabled = true
                    validateInput(textFieldA, validatorA)
                    validateInput(textFieldB, validatorB)
                }


                textFieldA.document.onTextChange { validateBoth() }

                textFieldB.document.onTextChange { validateBoth() }


            } else {
                actions[i] = object : AbstractAction(option) {
                    override fun actionPerformed(e: ActionEvent) {
                        close(i)
                    }
                }
            }
        }
        return actions
    }



    private fun textValid(textField: JTextComponent, validator: InputValidator?): Boolean {
        val inputString = textField.text.trim { it <= ' ' }
        return validator == null ||
                validator.checkInput(inputString) &&
                validator.canClose(inputString)
    }

    override fun doOKAction() {
        if (textValid(textFieldA, validatorA) && textValid(textFieldB, validatorB)) {
            close(0)
        }
    }

    override fun createCenterPanel(): JComponent? = null

    override fun createNorthPanel(): JComponent? {
        val panel = createIconPanel()
        val messagePanel = createMessagePanel()
        panel.add(messagePanel, BorderLayout.CENTER)
        return panel
    }

    override fun createMessagePanel(): JPanel {
        val messagePanel = JPanel(BorderLayout())
        if (myMessage != null) {
            val textComponent = createTextComponent()
            messagePanel.add(textComponent, BorderLayout.NORTH)
        }
        val inputRowA = createTextFieldComponentA()
        val inputRowB = createTextFieldComponentB()
        messagePanel.add(inputRowA, BorderLayout.CENTER)
        messagePanel.add(inputRowB, BorderLayout.SOUTH)
        return messagePanel
    }


    private fun createTextComponent(): JComponent {
        val textComponent: JComponent
        if (BasicHTML.isHTMLString(myMessage)) {
            textComponent = createMessageComponent(myMessage)
        } else {
            val textLabel = JLabel(myMessage)
            textLabel.ui = MultiLineLabelUI()
            textComponent = textLabel
        }
        textComponent.border = BorderFactory.createEmptyBorder(0, 0, 5, 20)
        return textComponent
    }

    private fun <T : JComponent> JPanel.add(component: T, position: String, init: T.() -> Unit = {}) =
        add(component.apply(init), position)

    private fun createTextFieldComponentA(): JComponent = JPanel(BorderLayout()).apply {
        add(JLabel(descriptionA), BorderLayout.LINE_START) {
            border = EmptyBorder(0, 0, 0, 5)
        }
        add(JTextField(30), BorderLayout.LINE_END) {
            margin = JBInsets(0, 10, 0, 0)
            textFieldA = this
        }
    }

    private fun createTextFieldComponentB(): JComponent = JPanel(BorderLayout()).apply {
        add(JLabel(descriptionB), BorderLayout.LINE_START) {
            border = EmptyBorder(0, 0, 0, 5)
        }
        add(JTextField(30), BorderLayout.LINE_END) {
            margin = JBInsets(0, 10, 0, 0)
            textFieldB = this
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return textFieldA
    }

    val inputStringA: String?
        get() = if (exitCode == 0) {
            textFieldA.text.trim { it <= ' ' }
        } else null

    val inputStringB: String?
        get() = if (exitCode == 0) {
            textFieldB.text.trim { it <= ' ' }
        } else null
}
