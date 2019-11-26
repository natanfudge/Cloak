package cloak.util

import RenameErrorTests.Companion.TestAuthor
import cloak.platform.ExtendedPlatform
import cloak.platform.PersistentSaver
import cloak.platform.PlatformInputValidator
import cloak.platform.UserInputRequest
import java.nio.file.Path
import java.nio.file.Paths


class TestPlatform(private val userInput: Pair<String, String?>) : ExtendedPlatform {
    override val storageDirectory: Path = Paths.get(System.getProperty("user.dir"),"caches")
    override val persistentSaver: PersistentSaver = object : PersistentSaver() {
        override fun registerProjectCloseCallback(callback: () -> Unit) {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    callback()
                }
            })
        }
    }

    override suspend fun getTwoInputs(
        message: String?,
        request: UserInputRequest,
        descriptionA: String?,
        descriptionB: String?,
        initialValueA: String?,
        initialValueB: String?,
        validatorA: PlatformInputValidator?,
        validatorB: PlatformInputValidator?
    ): Pair<String, String?>? = when (request) {
        UserInputRequest.NewName -> userInput
        UserInputRequest.GitUserAuthor -> Pair(TestAuthor.name, TestAuthor.email)
    }

    override suspend fun showMessageDialog(message: String, title: String) {}
    override suspend fun showErrorPopup(message: String, title: String) {}
    override suspend fun getUserInput(title: String, message: String, validator: PlatformInputValidator?) = null
    override suspend fun <T> asyncWithText(title: String, action: suspend () -> T): T = action()

}


