
import cloak.mapping.doesNotExist
import org.eclipse.jgit.lib.PersonIdent
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import util.TestYarnRepo
import java.io.File
import java.util.*

class GitTests {

    private val yarn = TestYarnRepo

    companion object {
        val TestAuthor = PersonIdent("natanfudge","natandestroyer100@gmail.com")
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
    @Ignore
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
    @Ignore("Too dangerous")
    fun `Can push changes to remote`() {
        val repo = yarn.getOrCloneGit()
        repo.stageChanges("MAINTAINERS")
        repo.commit(author = TestAuthor, commitMessage = "Test Commit")
        yarn.push()
    }

    @Test
    fun `Git remove deletes file`() {
        val repo = yarn.getOrCloneGit()
        val deleteTarget = File("yarn/testDelete")
        deleteTarget.createNewFile()
        repo.stageChanges("testDelete")
        repo.remove("testDelete")
        assert(deleteTarget.doesNotExist)
    }

    @Test
    @Ignore
    fun `Can delete branch`() {
        yarn.getOrCloneGit().switchToBranch("master")
        yarn.deleteBranch("foofo")
    }
}