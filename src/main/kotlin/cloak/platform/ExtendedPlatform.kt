package cloak.platform

import java.nio.file.Path

interface ExtendedPlatform {
    val storageDirectory: Path

    val persistentSaver: PersistentSaver

    suspend fun getTwoInputs(
        message: String?,
        request: UserInputRequest,
        descriptionA: String?,
        descriptionB: String?,
        initialValueA: String? = null,
        initialValueB: String? = null,
        validatorA: PlatformInputValidator? = null,
        validatorB: PlatformInputValidator? = null
    ): Pair<String, String?>?

    suspend fun showMessageDialog(message: String, title: String)

    suspend fun showErrorPopup(message: String, title: String)

    suspend fun getUserInput(
        title: String,
        message: String,
        validator: PlatformInputValidator? = null
    ): String?

    suspend fun getChoiceBetweenOptions(title: String, options: List<String>): String

    suspend fun getMultipleChoicesBetweenOptions(title : String, options: List<String>) : List<String>

    suspend fun <T> asyncWithText(title: String, action: suspend () -> T): T
}