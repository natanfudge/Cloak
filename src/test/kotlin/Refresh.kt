import cloak.format.mappings.MappingsFile
import cloak.format.mappings.read
import cloak.format.mappings.writeTo
import org.junit.Ignore
import org.junit.Test
import java.io.File

class Refresh {
    @Test
    @Ignore
    fun `Refresh Mappings`() {
        val dir = File("src/test/resources")
        dir.walk().forEach {
            if (it.isDirectory || it.extension != "mapping") return@forEach
            val parsed = MappingsFile.read(it)
            parsed.writeTo(it)
        }
    }
}