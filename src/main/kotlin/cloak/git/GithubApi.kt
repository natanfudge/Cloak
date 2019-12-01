package cloak.git

import TP
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonDecodingException

object GithubApi {
    class GithubException : Exception {
        constructor(message: String, cause: Throwable) : super(message, cause)
        constructor(message: String) : super(message)
    }

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

        try {
            val json = Json(JsonConfiguration.Stable.copy(strictMode = false)).parse(
                PullRequestResponse.serializer(),
                responseText
            )

            if (json.htmlUrl == null) throw GithubException("Could not perform pull request: $responseText")

            return json
        } catch (e: JsonDecodingException) {
            throw GithubException("Could not perform pull request: $responseText", e)
        }
    }


    fun getDefaultBranch(repo: String, owner: String): String = TP.getDefaultBranch(repo, owner)


}