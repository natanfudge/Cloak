import cloak.mapping.descriptor.*
import org.junit.Test
import kotlin.test.assertEquals

class DescriptorTests {
    private fun testReadWrite(parsed: FieldDescriptor, unparsed: String) {
        assertEquals(parsed, FieldDescriptor.read(unparsed))
        assertEquals(parsed.classFileName, unparsed)
    }

    private fun testReadWrite(parsed: MethodDescriptor, unparsed: String) {
        assertEquals(parsed, MethodDescriptor.read(unparsed))
        assertEquals(parsed.classFileName, unparsed)
    }

    @Test
    fun `Parse Basic`() = testReadWrite(FieldType.Byte, "B")

    @Test
    fun `Parse Array`() = testReadWrite(ArrayType(FieldType.Int), "[I")

    @Test
    fun `Parse Class`() = testReadWrite(ObjectType("Foo"), "LFoo;")

    @Test
    fun `Parse Complex Type`() = testReadWrite(ArrayType(ArrayType(ObjectType("Bar"))), "[[LBar;")

    @Test
    fun `Parse Returning Method`() {
        val parsed = MethodDescriptor(
            listOf(
                FieldType.Long, ArrayType(FieldType.Double), ObjectType("Baz"),
                ArrayType(ArrayType(ObjectType("Bar")))
            ), ArrayType(ObjectType("Ka"))
        )

        val unparsed = "(J[DLBaz;[[LBar;)[LKa;"
        testReadWrite(parsed, unparsed)
    }


    @Test
    fun `Parse Void Method`() {
        val parsed = MethodDescriptor(
            listOf(
                FieldType.Long, ArrayType(FieldType.Double), ObjectType("Baz"),
                ArrayType(ArrayType(ObjectType("Bar")))
            ), ReturnDescriptor.Void
        )

        val unparsed = "(J[DLBaz;[[LBar;)V"
        testReadWrite(parsed, unparsed)
    }
}