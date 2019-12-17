import cloak.format.descriptor.ObjectType
import cloak.format.descriptor.PrimitiveType
import cloak.format.mappings.MappingsFile
import cloak.format.mappings.read
import cloak.format.mappings.writeTo
import org.junit.Test
import cloak.util.assertEqualsIgnoreLineBreaks
import cloak.util.comment
import cloak.util.getTestResource
import cloak.util.mappingsFile
import kotlin.test.assertEquals

class MappingsTests {

    private val testMappings = mappingsFile("bgq", "net/minecraft/block/AbstractBannerBlock") {
        comment("LE BEST MAPPINGS")
        comment("OF THE WORLDdddd xd")
        innerClass("testObf", "testDeobf") {
            comment("inner class comment")
            field("a", "b", PrimitiveType.Byte)
            method("d", "e") {
                param(1, "g"){
                    comment("arg comment")
                    comment("line 2")
                    comment("and line 3")
                }
            }
        }
        innerClass("a", "b") {
            method("a", "b")
        }
        innerClass("testMissing")
        field("a", "color", ObjectType("awa")){
            comment("field comment")
        }
        method("b", "getColor", returnType = ObjectType("awa")){
            comment("method comment")
        }
        method("obf")

    }


    @Test
    fun `Enigma files are parsed correctly`() {
        val parsed = MappingsFile.read(getTestResource("AbstractBannerBlock.mapping"))
        assertEquals(expected = testMappings, actual = parsed)
    }

    @Test
    fun `Enigma files are written correctly`() {
        val expected = getTestResource("AbstractBannerBlock.mapping").readText()
        val actual = createTempFile().let {
            testMappings.writeTo(it)
            it.readText()
        }

        assertEqualsIgnoreLineBreaks(expected, actual)
    }

    @Test
    fun `Reading and writing a mappings file results in the same thing`() {
        val originalFile = getTestResource("net/minecraft/block/Block.mapping")
        val actual = createTempFile().let {
            MappingsFile.read(originalFile).writeTo(it)
            it.readText()
        }
        assertEqualsIgnoreLineBreaks(originalFile.readText(), actual)
    }
}