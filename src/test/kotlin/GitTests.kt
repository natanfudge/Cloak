
import cloak.mapping.YarnRepo
import cloak.mapping.doesNotExist
import org.eclipse.jgit.lib.PersonIdent
import org.junit.BeforeClass
import org.junit.Test
import util.TestYarnRepo
import java.io.File
import java.util.*

class GitTests {

    private val yarn = TestYarnRepo
    private val testAuthor = PersonIdent("natanfudge","natandestroyer100@gmail.com")
    companion object {
        @BeforeClass
        @JvmStatic
        fun clean() {
//            grandma.YarnRepo.clean()
        }
    }


    @Test
    fun `Can clone the yarn repository`() {
        yarn.getOrCloneGit()
        assert(yarn.mappingsDirectory.exists())
    }

    @Test
    fun `Branches retain information`() {
        yarn.getOrCloneGit().switchToBranch("secretInfo")
        assert(yarn.getFile("secretTestInfo").exists())
        yarn.getOrCloneGit().switchToBranch("master")
        yarn. getOrCloneGit().switchToBranch("secretInfo")
        assert(yarn.getFile("secretTestInfo").exists())
        yarn. getOrCloneGit().switchToBranch("secretInfo")
        assert(yarn.getFile("secretTestInfo").exists())
    }

    @Test
    fun `Can switch to non-existent branch`() {
        val repo = yarn.getOrCloneGit()
        val branchName = "testBranch" + UUID.randomUUID()
        repo.switchToBranch(branchName)
        assert(repo.getBranches().any { it.name == "refs/heads/$branchName" })
    }

    @Test
    fun `Can push changes to remote`() {
        val repo = yarn.getOrCloneGit()
        repo.stageChanges("MAINTAINERS")
        repo.commit(author = testAuthor, commitMessage = "Test Commit")
        yarn.push(repo)
    }

    @Test
    fun `Git remove deletes file`() {
        //TODO: test this actually works
        val repo = yarn.getOrCloneGit()
        val deleteTarget = File("yarn/testDelete")
        deleteTarget.createNewFile()
        repo.stageChanges("testDelete")
        repo.remove("testDelete")
        assert(deleteTarget.doesNotExist)
    }
}