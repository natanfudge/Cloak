import cloak.git.GithubApi
import cloak.git.YarnRepo
import cloak.git.createPullRequest
import cloak.git.getDefaultBranch
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class GithubTests {
    @Test
    @Ignore
    fun `Can send pull request`() {
        val response = GithubApi.createPullRequest(
            repositoryName = "yarn", requestingUser = "natanfudge", requestingBranch = "natanfudge",
            targetBranch = "19w04b", targetUser = "shedaniel",
            body = "FOOBAR", title = "don't merge"
        )
    }

    @Test
    fun `The default branch of cloak is master`() {
        assertEquals("master", GithubApi.getDefaultBranch("yarn", YarnRepo.GithubUsername))
    }
}