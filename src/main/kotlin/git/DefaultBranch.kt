package cloak.git

import cloak.mapping.NormalJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

fun GithubApi.getDefaultBranch(repo: String, owner: String): String {
    val httpclient = HttpClients.createDefault()
    val httpget = HttpGet("https://api.github.com/repos/$owner/$repo")
    httpget.addHeader("Authorization", "token ${GithubApi.getToken()}")

    //Execute and get the response.
    val response: HttpResponse = httpclient.execute(httpget)
    val entity = response.entity ?: throw GithubException("http post request did not return a response")

    val responseText = EntityUtils.toString(entity)
    val serializedResponse = NormalJson.parseJson(responseText) as JsonObject
    return serializedResponse["default_branch"]!!.content

}
