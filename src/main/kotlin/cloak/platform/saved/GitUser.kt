package cloak.platform.saved

import cloak.platform.ExtendedPlatform
import cloak.platform.PlatformInputValidator
import cloak.platform.SavedState
import cloak.platform.UserInputRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.nullable
import org.eclipse.jgit.lib.PersonIdent

@Serializable
data class GitUser(val name: String, val email: String) {
    val jgit get() = PersonIdent(name, email)
    val branchName get() = name
}

private var ExtendedPlatform.gitUser: GitUser? by SavedState(null,"CurrentGitUser", GitUser.serializer().nullable)

private suspend fun ExtendedPlatform.getNewGitUser(): GitUser? {
    val (username, email) = getTwoInputs(
        "Enter your Github user to attribute contributions to",
        UserInputRequest.GitUserAuthor,
        "Username",
        "Email",
        validatorA = PlatformInputValidator(allowEmptyString = false),
        validatorB = PlatformInputValidator(allowEmptyString = false)
    ) ?: return null

    return GitUser(username, email ?: error("An empty email was provided even though an empty string is not allowed"))
}

suspend fun ExtendedPlatform.getGitUser(): GitUser? {
    if (gitUser == null) {
        gitUser = getNewGitUser()
    }
    return gitUser
}