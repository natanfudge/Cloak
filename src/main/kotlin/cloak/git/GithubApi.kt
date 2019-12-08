package cloak.git

import TP
import kotlinx.serialization.json.*

object GithubApi {
    open class GithubException : Exception {
        constructor(message: String, cause: Throwable) : super(message, cause)
        constructor(message: String) : super(message)
    }

    class PullRequestAlreadyExistsException(val pullRequestTitle: String) :
        GithubException("Pull Request $pullRequestTitle already exists")

    fun createPullRequest(
        repositoryName: String,
        requestingUser: String,
        requestingBranch: String,
        targetBranch: String,
        targetUser: String,
        title: String,
        body: String
    ): PullRequestResponse {
        val responseText = TP.createPullRequest(
            repositoryName,
            requestingUser,
            requestingBranch,
            targetBranch,
            targetUser,
            title,
            body
        )

//        try {
        val json = Json(JsonConfiguration.Stable.copy(strictMode = false))
        val tree = json.parseJson(responseText)
        if (tree.isPullRequestAlreadyExistsResponse()) throw PullRequestAlreadyExistsException(title)

        val parsed = json.fromJson(PullRequestResponse.serializer(), tree)

        if (parsed.htmlUrl == null) throw GithubException("Could not perform pull request: $responseText")

        return parsed
//        } catch (e: JsonDecodingException) {
//            throw GithubException("Could not perform pull request: $responseText", e)
//        }
    }

    private fun JsonElement.isPullRequestAlreadyExistsResponse(): Boolean {
        if (this !is JsonObject) return false
        val errors = this["errors"] as? JsonArray ?: return false
        val error = errors.firstOrNull() as? JsonObject ?: return false
        val message = error["message"] as? JsonPrimitive ?: return false
        return message.content.startsWith(PullRequestExistsMessageStart)
    }


    fun getDefaultBranch(repo: String, owner: String): String = TP.getDefaultBranch(repo, owner)


}

private const val PullRequestExistsMessageStart = "A pull request already exists"