package cloak.platform

class PlatformInputValidator(
    val allowEmptyString: Boolean,
    val tester: ((String) -> String?)? = {null}
)

