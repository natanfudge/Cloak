import cloak.idea.util.RenameInput
import cloak.mapping.StringSuccess
import cloak.mapping.descriptor.ObjectType
import cloak.mapping.rename.Renamer
import cloak.mapping.rename.cloakUser
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import cloak.util.*

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
                internalSwitchToBranch(GitTests.TestAuthor.cloakUser.branchName)
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
        testName: String,
        newName: String,
        oldFileName: String = "Block",
        oldPath: String = "net/minecraft/block",
        newFileName: String = oldFileName,
        newPath: String = oldPath,
        explanation: String? = null,
        nameInit: (ClassBuilder.() -> NameBuilder<*>)? = null
    ) = runBlocking {

        val oldFullPath = "$oldPath/$oldFileName"
        val newFullPath = "$newPath/$newFileName.mapping"

        TestYarnRepo.getMappingsFile(newFullPath).delete()

        val isTopLevelClass = newFileName != oldFileName

        useFile("$oldFullPath.mapping")
        val project = TestProjectWrapper(RenameInput(newName, explanation))
        val targetName = className(oldFullPath, nameInit)
        val result = Renamer.rename(project, targetName, isTopLevelClass)
        assert(result is StringSuccess) { result.toString() }

        if (isTopLevelClass) {
            assert(!TestYarnRepo.getMappingsFile("$oldFullPath.mapping").exists())
        }

        val actual = TestYarnRepo.getMappingsFile(newFullPath)
        assert(actual.exists())

        val expected = getExpected(testName)

        assertEqualsIgnoreLineBreaks(expected.readText(), actual.readText())
    }

    private fun getExpected(testName: String) = getTestResource("expected").listFiles()!!
        .find { it.name.startsWith(testName) } ?: error("Could not find tes resource: $testName")

    @Test
    fun `Rename Class`() = testRename(
        testName = "RenameClass",
        newName = "Bleak",
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

    //TODO: complete these tests
    @Test
    fun `Rename Parameter`() = testRename("RenameParameter", "foobarbaz") {
        method(
            "topCoversMediumSquare",
            ObjectType("net/minecraft/world/BlockView"), ObjectType("net/minecraft/util/math/BlockPos")
        ).parameter(0)
    }

    @Test
    fun `Rename constructor arg`() = testRename("RenameConstructorParam", "newconstruct") {
        method("<init>",ObjectType("net/minecraft/client/gui/widget/LockButtonWidget\$IconLocation")).parameter(0)
    }

    @Test
    fun `Rename Complex Target`() = testRename("ComplexRename","ayyy") {
        innerClass("OffsetType").innerClass("complexInner")
            .method("complexMethod").parameter(0)
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

    @Test
    fun `Add new outer class`() {

    }

    @Test
    fun `Add new inner class`() {

    }

    @Test
    fun `Add new method`() {

    }

    @Test
    fun `Add new field`() {

    }

    @Test
    fun `Add new parameter name`() {

    }

}

