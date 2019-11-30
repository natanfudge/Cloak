package cloak.git

import TP
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonDecodingException

object GithubApi {
    class GithubException(message: String, cause: Throwable) : Exception(message, cause)

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
            return Json(JsonConfiguration.Stable.copy(strictMode = false)).parse(
                PullRequestResponse.serializer(),
                responseText
            )
        } catch (e: JsonDecodingException) {
            throw GithubException("Could not perform pull request: $responseText", e)
        }
    }


    fun getDefaultBranch(repo: String, owner: String): String = TP.getDefaultBranch(repo, owner)


}