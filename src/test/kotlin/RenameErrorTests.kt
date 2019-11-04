import cloak.idea.util.RenameInput
import cloak.mapping.StringError
import cloak.mapping.descriptor.ObjectType
import cloak.mapping.rename.Renamer
import cloak.mapping.rename.cloakUser
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import util.*

class RenameErrorTests {

    companion object {
        @JvmStatic
        @BeforeClass
        fun prepare() {
            with(TestYarnRepo.getOrCloneGit()){
                commit(GitTests.TestAuthor, "preparation")
                switchToBranch(GitTests.TestAuthor.cloakUser.branchName)
                TestYarnRepo.getMappingsFilesLocations()
            }
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            saveIntermediaryMap()
        }
    }

    private fun testError(
        newName: String,
        oldFileName: String = "Block",
        oldPath: String = "net/minecraft/block",
        newFileName: String = oldFileName,
        explanation : String? = null,
        nameInit: (ClassBuilder.() -> NameBuilder<*>)? = null
    ) = runBlocking {

        val oldFullPath = "$oldPath/$oldFileName"

        val isTopLevelClass = newFileName != oldFileName
        useFile("$oldFullPath.mapping")
        val project = TestProjectWrapper(RenameInput(newName,explanation))
        val targetName = className(oldFullPath, nameInit)
        val result = Renamer.rename(project, targetName, isTopLevelClass)
        assert(result is StringError)
    }

    @Test
    fun `Errors when there's already a class with that name in the same package`() = testError("AnvilBlock")

    @Test
    fun `Errors when there's already a class with that name in the new package`() =
        testError("net/minecraft/block/piston/PistonBehavior")

    @Test
    fun `Errors when there's already a field with that name`() = testError("field_20720") {
        field("field_100004")
    }

    @Test
    fun `Errors when there's already a method with the same name and param descriptor`() = testError("method_100007") {
        method("method_100003")
    }

    @Test
    fun `Errors when there's already a parameter with the same name`() = testError("pos") {
        method(
            "topCoversMediumSquare",
            ObjectType("net/minecraft/class_1922"), ObjectType("net/minecraft/class_2338")
        ).parameter(0)
    }

    @Test
    fun `Errors when you try to rename the package of a non-top level class`() = testError("net/test/foo") {
        innerClass("OffsetType")
    }

    @Test
    fun `Errors when you try to rename the package of a non-class`() = testError("net/test/foo") {
        method("method_100007")
    }
}