import cloak.mapping.rename.Renamer
import cloak.mapping.StringSuccess
import cloak.mapping.descriptor.ObjectType
import org.junit.Test
import util.*
import java.io.File
import kotlin.test.assertEquals

class RenameTests {
    private val yarn = TestYarnRepo

    private fun useFile(path: String): File {
        val newFile = yarn.getMappingsFile(path)
        getTestResource(path).copyTo(newFile, overwrite = true)
        yarn.getOrCloneGit().stageChanges("mappings/$path")
        return newFile
    }

    private fun testRename(
        test: String,
        userInput: String,
        newFileName: String = "Block",
        nameInit: (ClassBuilder.() -> NameBuilder<*>)? = null
    ) {
        val blockPath = "net/minecraft/block/Block"
        val isTopLevelClass = newFileName != "Block"
        useFile("$blockPath.mapping")
        val project = TestProjectWrapper(userInput)
        val targetName = className(blockPath, nameInit)
        val result = Renamer.rename(project,targetName , isTopLevelClass)
        assert(result is StringSuccess) { result.toString() }

        if (isTopLevelClass) {
            assert(!TestYarnRepo.getMappingsFile("$blockPath.mapping").exists())
        }

        val actual = TestYarnRepo.getMappingsFile("net/minecraft/block/$newFileName.mapping")
        assert(actual.exists())

        val expected = getExpected(test)

        assertEquals(expected.readText(), actual.readText())
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
    fun `Rename Method`()  = testRename("RenameMethod","goodNameNow"){
        method("getPlacementState", ObjectType("net/minecraft/entity/mob/VexEntity"))
    }

    @Test
    fun `Rename Method with generic argument`() {

    }

    @Test
    fun `Rename Method when there is more than one overload`() {

    }

    @Test
    fun `Rename Field`() {

    }

    @Test
    fun `Rename Parameter`() {

    }

    @Test
    fun `Rename Complex Target`() {

    }

    @Test
    fun `Qualify With Package`() {

    }

    @Test
    fun `Change Package`() {

    }

    @Test
    fun `Rename Unnamed`() {

    }

    @Test
    fun `Part Of Path Is Unnamed`() {

    }

    @Test
    fun `Errors when there's already a class with that name in the same package`() {

    }


    //TODO: make sure that there isn't even an option to rename constructors

}