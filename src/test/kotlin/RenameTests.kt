import cloak.mapping.StringSuccess
import cloak.mapping.descriptor.ObjectType
import cloak.mapping.rename.Renamer
import cloak.mapping.rename.cloakUser
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import util.*

private val yarn = TestYarnRepo
fun useFile(path: String) {
    val git = yarn.getOrCloneGit()
    getTestResource(path).copyTo(yarn.getMappingsFile(path), overwrite = true)
    git.stageChanges("mappings/$path")
}

class RenameTests {

    companion object {
        @JvmStatic
        @BeforeClass
        fun prepare() {
            with(yarn.getOrCloneGit()) {
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


    private fun testRename(
        test: String,
        userInput: String,
        oldFileName: String = "Block",
        oldPath: String = "net/minecraft/block",
        newFileName: String = oldFileName,
        newPath: String = oldPath,
        nameInit: (ClassBuilder.() -> NameBuilder<*>)? = null
    ) = runBlocking {

        val oldFullPath = "$oldPath/$oldFileName"
        val newFullPath = "$newPath/$newFileName.mapping"

        TestYarnRepo.getMappingsFile(newFullPath).delete()

        val isTopLevelClass = newFileName != oldFileName

        // Let the repo know we've added a new file
        TestYarnRepo.updateMappingsFileLocation("", newLocation = oldFullPath)
        useFile("$oldFullPath.mapping")
        val project = TestProjectWrapper(userInput)
        val targetName = className(oldFullPath, nameInit)
        val result = Renamer.rename(project, targetName, isTopLevelClass)
        assert(result is StringSuccess) { result.toString() }

        if (isTopLevelClass) {
            assert(!TestYarnRepo.getMappingsFile("$oldFullPath.mapping").exists())
        }

        val actual = TestYarnRepo.getMappingsFile(newFullPath)
        assert(actual.exists())

        val expected = getExpected(test)

        assertEqualsIgnoreLineBreaks(expected.readText(), actual.readText())
    }

    private fun getExpected(testName: String) = getTestResource("expected").listFiles()!!
        .find { it.name.startsWith(testName) } ?: error("Could not find tes resource: $testName")

    @Test
    fun `Rename Class`() = testRename(
        test = "RenameClass",
        userInput = "Bleak",
        newFileName = "Bleak"
    )

    @Test
    fun `Rename Inner Class`() = testRename("RenameInner", "OffsetTypist") {
        innerClass("OffsetType")
    }

    @Test
    fun `Rename Method`() = testRename("RenameMethod", "goodnamenow") {
        method(
            "topCoversMediumSquare",
            ObjectType("net/minecraft/world/BlockView"), ObjectType("net/minecraft/util/math/BlockPos")
        )
    }

    @Test
    fun `Rename Method with inner classes descriptor`() =
        testRename("RenameMethodInnerDesc", "testInnerDescriptorNew") {
            method("testInnerDescriptor", ObjectType("net/minecraft/class_2248\$class_2250"))
        }

    @Test
    fun `Rename Method with generic argument`() = testRename("RenameMethodGeneric", "testGenericArgNew") {
        method("testGenericArg", ObjectType("java/lang/Object"))
    }

    @Test
    fun `Rename Method when there is more than one overload`() =
        testRename("RenameMethodOverload", "testOverload1New") {
            method("testOverload1")
        }

    @Test
    fun `Rename Field`() = testRename("RenameField", "testFieldNew") {
        field("testField")
    }

    @Test
    fun `Rename Parameter`() {

    }

    @Test
    fun `Rename constructor arg`() {

    }

    @Test
    fun `Rename Complex Target`() {

    }

    @Test
    fun `Change Package`() = testRename("ChangePackage", "foo/bar/boing", newFileName = "boing", newPath = "foo/bar")

    @Test
    fun `Rename Unnamed`() = testRename("RenameUnnamed", "actualName") {
        method("method_100007")
    }

    @Test
    fun `Part Of Path Is Unnamed`() =
        testRename("PartUnnamed", "newName", oldFileName = "class_2189", oldPath = "net/minecraft") {
            method("someMethod")
        }


}

