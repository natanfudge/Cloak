import cloak.actions.ModifyJavadocAction
import cloak.format.descriptor.ObjectType
import cloak.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class JavadocTests {

    private fun testJavadoc(
        testName: String,
        javadoc: String,
        targetBuilder: (ClassBuilder.() -> NameBuilder<*>)? = null
    ) = runBlocking {
        val path = "net/minecraft/block/Block"

        val target = className(path, targetBuilder)
        TestYarnRepo.getMappingsFile("$path.mapping").delete()
        useFile("$path.mapping")

        val expected = getExpected(testName)
        ModifyJavadocAction.modify(TestPlatform(javaDocInput = javadoc), target)
        val actual = TestYarnRepo.getMappingsFile("$path.mapping")
        assert(actual.exists())
        assertEqualsIgnoreLineBreaks(expected.readText(), actual.readText())
    }

    @Test
    fun `Add class javadoc`() {
        testJavadoc("AddClassJavadoc", "the testest javadoc")
    }

    @Test
    fun `Add constructor javadoc`() = testJavadoc("AddConstructorJavadoc", "constructor javadoc") {
        method("<init>", ObjectType("net/minecraft/class_347\$class_348"))
    }

    @Test
    fun `Add method javadoc`() =
        testJavadoc("AddMethodJavadoc", "method javadoc\nline 2 of method javadoc\nline 3 of method javadoc") {
            method(
                "topCoversMediumSquare",
                ObjectType("net/minecraft/world/BlockView"),
                ObjectType("net/minecraft/util/math/BlockPos")
            )
        }

    @Test
    fun `Add field javadoc`() = testJavadoc("AddFieldJavadoc", "field javadoc") {
        field("opaque")
    }

    @Test
    fun `Add parameter javadoc`() = testJavadoc("AddParameterJavadoc", "arg javadoc") {
        method(
            "topCoversMediumSquare",
            ObjectType("net/minecraft/world/BlockView"),
            ObjectType("net/minecraft/util/math/BlockPos")
        )
            .parameter(1)
    }

    @Test
    fun `Add parameter javadoc on unmapped parameter of unmapped method of unmapped class`() = testJavadoc("AddJavadocToUnnamedChain","all the stuff unmapped javadoc") {
        innerClass("class_2251")
            .method("method_9637", ObjectType("net/minecraft/block/Material")).parameter(1)
    }
}