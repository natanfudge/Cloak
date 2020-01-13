package cloak.idea.gui

import cloak.idea.util.CommonIcons
import cloak.platform.InputFieldData
import cloak.platform.PlatformInputValidator
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
    icon: Icon = CommonIcons.Question,
    inputA: InputFieldData,
    inputB: InputFieldData
): Pair<String, String>? {

    val inputAWrapper = InputFieldDataWrapper(inputA)
    val inputBWrapper = InputFieldDataWrapper(inputB)

    val dialog = TwoInputsDialog(
        project, message, title, icon,
        arrayOf(Messages.OK_BUTTON, Messages.CANCEL_BUTTON),
        0, inputAWrapper, inputBWrapper
    )
    inputAWrapper.applySelection()
    inputBWrapper.applySelection()

    dialog.show()

    val inputStringA = dialog.inputStringA
    val inputStringB = dialog.inputStringB
    return if (inputStringA != null && inputStringB != null) inputStringA to inputStringB
    else null
}

fun <T : JComponent> JPanel.add(component: T, position: String, init: T.() -> Unit = {}) =
    add(component.apply(init), position)

class InputValidatorWrapper(private val validator: PlatformInputValidator) : InputValidatorEx {
    override fun checkInput(inputString: String): Boolean {
        return validator.allowEmptyString || inputString.any { !it.isWhitespace() }
    }

    override fun getErrorText(inputString: String): String? = validator.tester?.invoke(inputString)

    override fun canClose(inputString: String?) = true
}

class InputFieldDataWrapper(private val data: InputFieldData) {
    val validator: InputValidator? = data.validator?.let { InputValidatorWrapper(it) }

    lateinit var textField: JTextComponent
        private set

    fun getInputString(exitCode: Int): String? = if (exitCode == 0) {
        textField.text.trim { it <= ' ' }
    } else null

    fun initText() {
        textField.text = data.initialValue
    }

    fun textValid(): Boolean {
        val inputString = textField.text.trim { it <= ' ' }
        return validator == null ||
                validator.checkInput(inputString) &&
                validator.canClose(inputString)
    }

    private val textFieldCore: JTextComponent = if (data.multiline) JTextArea(7, 47) else JTextField(30)

    fun createTextFieldComponent() = JPanel(BorderLayout()).apply {
        add(JLabel(data.description), BorderLayout.LINE_START) {
            border = EmptyBorder(0, 0, 5, 5)
        }
        add(textFieldCore, BorderLayout.LINE_END) {
            margin = JBInsets(0, 10, 0, 0)
            textField = this
        }
    }

    fun applySelection() {
        if (data.defaultSelection != null) textField.select(data.defaultSelection.first, data.defaultSelection.last + 1)
        textField.putClientProperty(DialogWrapperPeer.HAVE_INITIAL_SELECTION, true)
    }
}

class TwoInputsDialog(
    project: Project?,
    message: String?,
    title: String?,
    icon: Icon?,
    options: Array<String?>,
    defaultOption: Int,
    private val inputA: InputFieldDataWrapper,
    private val inputB: InputFieldDataWrapper
) : MessageDialog(project, true) {

    private inline fun bothInputs(code: InputFieldDataWrapper.() -> Unit) {
        inputA.code()
        inputB.code()
    }

    init {
        _init(title, message, options, defaultOption, -1, icon, null)
        bothInputs { initText() }
        enableOkAction()
    }

    private fun enableOkAction() {
        okAction.isEnabled = true
        bothInputs {
            if (validator != null && !validator.checkInput(textField.text.trim { it <= ' ' })) okAction.isEnabled =
                false
        }
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

                bothInputs {
                    textField.document.onTextChange {
                        actions[i]!!.isEnabled = true
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
                }

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


    override fun doOKAction() {
        if (inputA.textValid() && inputB.textValid()) {
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
        val inputRowA = inputA.createTextFieldComponent()
        val inputRowB = inputB.createTextFieldComponent()
        messagePanel.add(inputRowA, BorderLayout.CENTER)
        messagePanel.add(inputRowB, BorderLayout.SOUTH)
        return messagePanel
    }


    private fun createTextComponent(): JComponent {
        val textComponent: JComponent = if (BasicHTML.isHTMLString(myMessage)) {
            createMessageComponent(myMessage)
        } else {
            val textLabel = JLabel(myMessage)
            textLabel.ui = MultiLineLabelUI()
            textLabel
        }
        textComponent.border = BorderFactory.createEmptyBorder(0, 0, 5, 20)
        return textComponent
    }


    override fun getPreferredFocusedComponent(): JComponent? {
        return inputA.textField
    }

    val inputStringA: String?
        get() = inputA.getInputString(exitCode)
    val inputStringB: String?
        get() = inputB.getInputString(exitCode)
}