import cloak.mapping.descriptor.ObjectType
import cloak.mapping.descriptor.PrimitiveType
import cloak.mapping.mappings.MappingsFile
import cloak.mapping.mappings.read
import cloak.mapping.mappings.writeTo
import org.junit.Test
import cloak.util.assertEqualsIgnoreLineBreaks
import cloak.util.getTestResource
import cloak.util.mappingsFile
import kotlin.test.assertEquals

class MappingsTests {

    private val testMappings = mappingsFile("bgq", "net/minecraft/block/AbstractBannerBlock") {
        innerClass("testObf", "testDeobf") {
            field("a", "b", PrimitiveType.Byte)
            method("d", "e") {
                param(1, "g")
            }
        }
        innerClass("a", "b") {
            method("a", "b")
        }
        innerClass("testMissing")
        field("a", "color", ObjectType("awa"))
        method("b", "getColor", returnType = ObjectType("awa"))
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

        assertEquals(expected, actual)
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