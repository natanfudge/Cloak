package cloak.git

//import org.apache.commons.httpclient.HttpClient
import cloak.mapping.NormalJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonDecodingException
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils


//import io.ktor.client


@Serializable
private data class PullRequest(val title: String, val body: String, val head: String, val base: String)

class GithubException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

//curl -v https://api.github.com/repos/shedaniel/SmoothScrollingEverywhere/pulls -d "{\"title\":\"testPR\",\"base\":\"master\", \"head\":\"natanfudge:master\"}" -X POST  --header "Authorization: token "

fun GithubApi.createPullRequest(
    repositoryName: String,
    requestingUser: String,
    requestingBranch: String,
    targetBranch: String,
    targetUser: String,
    title: String,
    body: String
): PullRequestResponse {
    val request =
        PullRequest(title = title, base = targetBranch, head = "$requestingUser:$requestingBranch", body = body)
    val postBody = NormalJson.stringify(PullRequest.serializer(), request)


    val httpclient = HttpClients.createDefault()
    val httppost = HttpPost("https://api.github.com/repos/$targetUser/$repositoryName/pulls")
    httppost.addHeader("Authorization", "token ${GithubApi.getToken()}")

    httppost.entity = StringEntity(postBody)

    //Execute and get the response.
    val response: HttpResponse = httpclient.execute(httppost)
    val entity = response.entity ?: throw GithubException("http post request did not return a response")

    val responseText = EntityUtils.toString(entity)

    try {
        return Json(JsonConfiguration.Stable.copy(strictMode = false)).parse(
            PullRequestResponse.serializer(),
            responseText
        )
    } catch (e: JsonDecodingException) {
        throw GithubException("Could not perform pull request: $responseText", e)
    }
}

