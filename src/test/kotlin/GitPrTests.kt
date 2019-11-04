import cloak.idea.git.createPullRequest
import kotlinx.coroutines.runBlocking
import org.junit.Test

class GitPrTests {
    @Test
    fun testRandomRepo()  {
        createPullRequest(branch = "master", body = "FOOBAR", title = "don't merge")
    }
}