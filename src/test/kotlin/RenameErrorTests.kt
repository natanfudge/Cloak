import RenameErrorTests.Companion.TestAuthor
import cloak.format.descriptor.ObjectType
import cloak.format.rename.Renamer
import cloak.platform.saved.GitUser
import cloak.platform.saved.thisIsAMethodForTestToNotLongerRenamesNamesBetweenTestsDontUseItThanks
import cloak.util.*
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.junit.Test

fun prepareRenames(){
    with(TestYarnRepo) {
        commitChanges(TestAuthor, "preparation")
        switchToBranch(TestAuthor.branchName,false)
        TestYarnRepo.getMappingsFilesLocations()
    }
}
class RenameErrorTests {

    companion object {
        val TestAuthor = GitUser("natanfudge", "natandestroyer100@gmail.com")
        @JvmStatic
        @BeforeClass
        fun prepare() {
            prepareRenames()
        }

    }

    private fun testError(
        newName: String,
        oldFileName: String = "Block",
        oldPath: String = "net/minecraft/block",
        newFileName: String = oldFileName,
        explanation: String? = null,
        nameInit: (ClassBuilder.() -> NameBuilder<*>)? = null
    ) = runBlocking {

        val oldFullPath = "$oldPath/$oldFileName"

        val isTopLevelClass = newFileName != oldFileName
        useFile("$oldFullPath.mapping")
        val platform = TestPlatform(Pair(newName, explanation))
        platform.thisIsAMethodForTestToNotLongerRenamesNamesBetweenTestsDontUseItThanks()
        val targetName = className(oldFullPath, nameInit)
        val result = with(Renamer) { platform.rename(targetName, isTopLevelClass) }
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

    @Test
    fun `Errors when you add an outer class that doesn't exist in the intermediaries`() =
        testError(newName = "nomatter", oldFileName = "class_123456")


    @Test
    fun `Errors when you add an inner class that doesn't exist in the intermediaries`() = testError("foo") {
        innerClass("noexist")
    }

    @Test
    fun `Errors when you add a method that doesn't exist in the intermediaries`() = testError("bar") {
        method("noexist2")
    }

    @Test
    fun `Errors when you add a field that doesn't exist in the intermediaries`() = testError("baz") {
        field("noexist3")
    }
}