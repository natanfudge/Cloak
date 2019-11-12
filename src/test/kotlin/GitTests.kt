import cloak.mapping.doesNotExist
import cloak.util.TestYarnRepo
import org.eclipse.jgit.lib.PersonIdent
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.util.*

class GitTests {

    companion object {
        private val yarn = TestYarnRepo
        val TestAuthor = PersonIdent("natanfudge", "natandestroyer100@gmail.com")
        @BeforeClass
        @JvmStatic
        fun prepare() {
//            grandma.YarnRepo.clean()
        }


        @AfterClass
        @JvmStatic
        fun cleanup() {
            yarn.close()
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
//        yarn.getOrCloneGit().switchToBranch("secretInfo")
//        assert(yarn.getFile("secretTestInfo").exists())
//        yarn.getOrCloneGit().switchToBranch("master")
//        yarn. getOrCloneGit().switchToBranch("secretInfo")
//        assert(yarn.getFile("secretTestInfo").exists())
//        yarn. getOrCloneGit().switchToBranch("secretInfo")
//        assert(yarn.getFile("secretTestInfo").exists())
    }

    @Test
    fun `Can switch to non-existent branch`() {
        val repo = yarn.getOrCloneGit()
        val branchName = "testBranch" + UUID.randomUUID()
        repo.internalSwitchToBranch(branchName, force = true) { "refs/heads/master" }
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
        yarn.getOrCloneGit().internalSwitchToBranch("master", force = true) { "refs/heads/master" }
        yarn.deleteBranch("foofo")
    }

    @Test
    fun `Can switch with upstream branch as base`() {
        yarn.getOrCloneGit().internalSwitchToBranch("testUpstream2", force = true) { "upstream/19w45b" }
    }
}