package util

import cloak.idea.util.ProjectWrapper
import java.io.File
import javax.swing.Icon


class TestProjectWrapper(private val userInput: String?) : ProjectWrapper {
    private val messages = mutableListOf<String>()
    override fun showInputDialog(
        message: String,
        title: String,
        icon: Icon,
        initialValue: String?,
        validator: ((String) -> String?)?
    ): String? = userInput

    override fun showMessageDialog(message: String, title: String, icon: Icon) {
        messages.add(message)
    }

    override val yarnRepoDir = File("yarn")
}

fun testProject(input: String? = null) = TestProjectWrapper(input)