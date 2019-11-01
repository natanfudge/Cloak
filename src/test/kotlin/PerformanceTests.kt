import cloak.mapping.NormalJson
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import org.junit.Test
import util.TestYarnRepo
import java.io.File
import kotlin.system.measureTimeMillis

class PerformanceTests {
//    @Test
//    fun testFiles(){
//        var wantedFile : File? = null
//        val time = measureTimeMillis {
//            TestYarnRepo.walkMappingsDirectory().forEach { if (it.nameWithoutExtension == "Block") wantedFile = it }
//        }
//        println("File time = $time")
//    }
//
//    private val filesCacheLocation = "filePaths.json"
//
//    @Test
//    fun createLocationCache(){
//        val paths = TestYarnRepo.walkMappingsDirectory().map { it.relativeTo(TestYarnRepo.mappingsDirectory).path }.toList()
//        File(filesCacheLocation).writeText(NormalJson.stringify(StringSerializer.list,paths))
//    }

//    @Test
//    fun testFromCache(){
//
//        var wantedFile : File? = null
//        val time = measureTimeMillis {
//            val paths =  NormalJson.parse(StringSerializer.list,File(filesCacheLocation).readText())
//            paths.forEach { if (it.endsWith("Block")) wantedFile = TestYarnRepo.getMappingsFile(it) }
//        }
//        println("Cache time = $time")
//    }
}