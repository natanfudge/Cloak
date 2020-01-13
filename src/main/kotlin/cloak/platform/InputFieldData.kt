package cloak.platform

data class InputFieldData(
    val description: String? = null,
    val initialValue: String? = null,
    val defaultSelection: IntRange? = null,
    val validator: PlatformInputValidator? = null,
    val multiline : Boolean = false
)