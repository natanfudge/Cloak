import cloak.idea.providerUtils.ObjWrapper.NamesJson
import cloak.mapping.rename.Name
import kotlinx.serialization.PolymorphicSerializer
import org.junit.Test
import cloak.util.className
import kotlin.test.assertEquals


class NameSerializationTests {
    @Test
    fun testJsonSerialization() {
        val obj: Name = className("foo")
        val str = NamesJson.stringify(PolymorphicSerializer(Name::class), obj)
        val back = NamesJson.parse(PolymorphicSerializer(Name::class), str)

        assertEquals(obj, back)

    }
}