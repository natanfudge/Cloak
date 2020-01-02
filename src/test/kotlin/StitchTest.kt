import net.fabricmc.stitch.util.FieldNameFinder
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.LogManager
import org.junit.Test
import java.io.File


class StitchTest {
    private val mcJar =
        File("C:\\Users\\natan\\Desktop\\Cloak\\src\\test\\resources\\minecraft-1.15.1-mapped-net.fabricmc.yarn-1.15.1+build.17-v2.jar")

    @Test
    fun testNameGenerator() {
        val names = FieldNameFinder().findNames(mcJar)
        println(names)
    }
}