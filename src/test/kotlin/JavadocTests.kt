import cloak.actions.ModifyJavadocAction
import cloak.util.*
import org.junit.Test

class JavadocTests {

    private fun testJavadoc(
        testName: String,
        javadoc: String,
        targetBuilder: (ClassBuilder.() -> NameBuilder<*>)? = null
    ) {
        val path = "net/minecraft/block/Block"

        val target = className(path, targetBuilder)
        val expected = getExpected(testName)
        ModifyJavadocAction.modify(TestPlatform(javaDocInput = javadoc), target)
        val actual = TestYarnRepo.getMappingsFile("$path.mapping")

        assertEqualsIgnoreLineBreaks(expected.readText(), actual.readText())
    }

    @Test
    fun `Add class javadoc`() {
        testJavadoc("AddClassJavadoc","TestJavadoc")
    }
}