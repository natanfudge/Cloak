import cloak.mapping.StringSuccess
import cloak.mapping.descriptor.ObjectType
import cloak.mapping.rename.Renamer
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Test
import util.*
import kotlin.test.assertEquals



class RenameTests {
    private val yarn = TestYarnRepo

    private fun useFile(path: String) {
        val git = yarn.getOrCloneGit()
        getTestResource(path).copyTo(yarn.getMappingsFile(path), overwrite = true)
        git.stageChanges("mappings/$path")
    }

    private fun testRename(
        test: String,
        userInput: String,
        oldFileName : String = "Block",
        oldPath : String = "net/minecraft/block",
        newFileName: String = oldFileName,
        newPath : String = oldPath,
        nameInit: (ClassBuilder.() -> NameBuilder<*>)? = null
    ) = runBlocking {

        val oldFullPath = "$oldPath/$oldFileName"
        val newFullPath = "$newPath/$newFileName"

        val isTopLevelClass = newFileName != oldFileName
        useFile("$oldFullPath.mapping")
        val project = TestProjectWrapper(userInput)
        val targetName = className(oldFullPath, nameInit)
        val result = Renamer.rename(project, targetName, isTopLevelClass)
        assert(result is StringSuccess) { result.toString() }

        if (isTopLevelClass) {
            assert(!TestYarnRepo.getMappingsFile("$oldFullPath.mapping").exists())
        }

        val actual = TestYarnRepo.getMappingsFile("$newFullPath.mapping")
        assert(actual.exists())

        val expected = getExpected(test)

        assertEquals(expected.readText().replace("\r\n", "\n"), actual.readText().replace("\r\n", "\n"))
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
    fun `Change Package`()  = testRename("ChangePackage", "foo/bar/boing", newFileName = "boing",newPath = "foo/bar")

    @Test
    fun `Rename Unnamed`() =testRename("RenameUnnamed","actualName"){
        method("method_100007")
    }

    @Test
    fun `Part Of Path Is Unnamed`() = testRename("PartUnnamed","newName", oldFileName = "class_2189",oldPath = "net/minecraft") {
        method("someMethod")
    }

    @Test
    fun `Errors when there's already a class with that name in the same package`() {

    }

    @Test
    fun `Errors when there's already a class with that name in the new package`() {

    }

    @Test
    fun `Errors when there's already a field with that name`() {

    }

    @Test
    fun `Errors when there's already a method with the same name and param descriptor`() {

    }


    companion object {
        @AfterClass
        @JvmStatic
        fun save() {
            saveIntermediaryMap()
        }
    }


    //TODO: make sure that there isn't even an option to rename constructors

}