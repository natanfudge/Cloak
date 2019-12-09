package cloak.git

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

object GithubApi {

    open class GithubException(message: String) : Exception(message)

    fun getDefaultBranch(repo: String, owner: String): String {
        return HttpClients.createDefault().use { client ->
            val httpget = HttpGet("https://api.github.com/repos/$owner/$repo")

            //Execute and get the response.
            val response = client.execute(httpget)
            val entity = response.entity ?: throw GithubException("http post request did not return a response")

            val responseText = EntityUtils.toString(entity)
            val serializedResponse = NormalJson.parseJson(responseText) as JsonObject
            serializedResponse["default_branch"]!!.content
        }

    }

    private val NormalJson = Json(JsonConfiguration.Stable)


}

