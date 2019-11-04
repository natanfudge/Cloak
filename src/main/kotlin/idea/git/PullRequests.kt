package cloak.idea.git

//import org.apache.commons.httpclient.HttpClient
import cloak.mapping.NormalJson
import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import java.util.*


//import io.ktor.client

private const val GithubUsername = "natanfudge"
//TODO: these should not be used for PRs
private val GithubPassword = System.getenv("GITHUB_PASSWORD")

@Serializable
private data class PullRequest(val title: String, val body: String, val head: String, val base: String)

 fun createPullRequest(branch: String, title: String, body: String) {
    val client = DefaultHttpClient()
    val request = PullRequest(title = "testPr", base = "master", head = "natanfudge:master",body = "foo")
    val body = NormalJson.stringify(PullRequest.serializer(), request)


     val httpclient = HttpClients.createDefault()
     val httppost = HttpPost("https://api.github.com/repos/shedaniel/SmoothScrollingEverywhere/pulls")
//     httppost.targ
     httppost.addHeader("Authorization", "token ${System.getenv("GRANDMA_GITHUB_API_KEY")}")

// Request parameters and other properties.
     // Request parameters and other properties.
//     val params: MutableList<NameValuePair> = ArrayList<NameValuePair>(2)
//     params.add(BasicNameValuePair("param-1", "12345"))
//     params.add(BasicNameValuePair("param-2", "Hello!"))
     httppost.entity = StringEntity(body)

//Execute and get the response.
     //Execute and get the response.
     val response: HttpResponse = httpclient.execute(httppost)
     val entity = response.entity

     entity?.content?.use { instream ->

     }


//curl -v https://api.github.com/repos/shedaniel/SmoothScrollingEverywhere/pulls -d "{\"title\":\"testPR\",\"base\":\"master\", \"head\":\"natanfudge:mas
//ter\"}" -X POST  --header "Authorization: token b86204efabf373e953f7c05a99d8a1098a535f5d"

//    val post = HttpPost("URL")
//    post.entity = StringEntity(body).apply { contentType = Header("application/json" }
//
//    //TODO: further customization
//
//
//    println(body)
//    val result = client.post<String>(
//        path = "api.github.com/repos/shedaniel/SmoothScrollingEverywhere/pulls",
//        body = body
//    ) {
//        //TODO: weaker, compile-time api key
//        header("Authorization", "token ${System.getenv("GRANDMA_GITHUB_API_KEY")}")
//    }
    //TODO: change to use oAuth token

//    client.setCredentials(GithubUsername, GithubPassword)
////    val issueId = (getPullRequests(repo,"").maxBy { it.id }?.id ?: 0) + 1
//////    val head = repo.owner + ":" + sourceBranch
//////    val base = targetBranch
////
////    val issueService = IssueService()
////    val issue = issueService.createIssue(repo,Issue().setBody("foo").setTitle("bar"))
//
//    val pullRequestService = PullRequestService()
//    val request = PullRequest()
//    request.title = "a fix"
//    request.body = "this is a fix"
//    request.head = PullRequestMarker().setLabel("master")
//    request.base = PullRequestMarker().setLabel("master")
//    return pullRequestService.createPullRequest(repo, request)

//    val pulls = PullRequestService(client)
//    // get sha for master
////    val references = DataService(client)
////    val forkRepositoryId = RepositoryId.create("natanfudge", "SmoothScrollingEverywhere")
////    val forked = references.getReference(forkRepositoryId, "heads/master")
//    val request = PullRequest()
//    request.title = "TITLE"
//    request.body = "BODY"
//    val head = PullRequestMarker()
////    head.label = signUser.getGitHubUsername().toString() + ":pull-" + count
//    request.head = head
//    val base = PullRequestMarker()
//    base.label = "master"
//    request.base = base
//    val newPull = pulls.createPullRequest(
//        createFromId( "natanfudge/SmoothScrollingEverywhere"),
//        request
//    )
//    return newPull
//    return newPull.htmlUrl
}

//@Throws(IOException::class, InterruptedException::class)
//private  fun createPullRequest(user: User, count: Int): String? {
//
//}