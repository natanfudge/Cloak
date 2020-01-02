//package cloak.fabric
//
//import com.google.common.net.UrlEscapers
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonConfiguration
//import kotlinx.serialization.list
//import net.fabricmc.mapping.reader.v2.MappingGetter
//import net.fabricmc.mapping.reader.v2.TinyV2Factory
//import net.fabricmc.mapping.reader.v2.TinyVisitor
//import org.apache.commons.io.FileUtils
//import java.net.URL
//import java.nio.file.FileSystems
//import java.nio.file.Files
//
//@Serializable
//data class MethodIdentifier(val methodName: String, val descriptor: String)
//
//@Serializable
//data class YarnNames(
//     val fieldNames: Map<String, String>,
//     val methodNames: Map<MethodIdentifier, String>,
//     val classNames: Map<String, String>
//) {
//    companion object {
//        fun fetchLatestYarnMappings(): YarnNames {
//            val escapedVersion = UrlEscapers.urlFragmentEscaper().escape(YarnVersion.getLatestYarnVersion())
//            val url = "https://maven.fabricmc.net/net/fabricmc/yarn/$escapedVersion/yarn-$escapedVersion-v2.jar"
//            val jar = Files.createTempFile("yarn", ".jar")
//            FileUtils.copyURLToFile(URL(url), jar.toFile())
//            return FileSystems.newFileSystem(jar, null).use { jarFs ->
//                Files.newBufferedReader(jarFs.getPath("mappings/mappings.tiny")).use {
//                    val visitor = Visitor()
//                    TinyV2Factory.visit(it, visitor)
//                    YarnNames(visitor.fieldNames, visitor.methodNames, visitor.classNames)
//                }
//            }
//        }
//    }
//
//    @Serializable
//    private data class YarnVersion(
//        val gameVersion: String,
//        val seperator: String,
//        val build: Int,
//        val maven: String,
//        val version: String,
//        val stable: Boolean
//    ) {
//
//        companion object {
//            private const val YARN_API_ENTRYPOINT = "https://meta.fabricmc.net/v2/versions/yarn/"
//
//            fun getLatestYarnVersion(): String {
//                val versions = Json(JsonConfiguration.Stable).parse(
//                    YarnVersion.serializer().list,
//                    URL(YARN_API_ENTRYPOINT).readText()
//                )
//
//                return versions[0].run { "${gameVersion}build.$build" }
//            }
//        }
//    }
//
//
//    private class Visitor : TinyVisitor {
//        // intermediary class name -> yarn class name
//        val classNames = mutableMapOf<String, String>()
//        // int field name -> named field name
//        val fieldNames = mutableMapOf<String, String>()
//        // int method name -> named method name
//        val methodNames = mutableMapOf<MethodIdentifier, String>()
//
//        override fun pushField(name: MappingGetter, descriptor: String) {
//            fieldNames[name.get(1)] = name.get(0)
//        }
//
//        override fun pushMethod(name: MappingGetter, descriptor: String) {
//            methodNames[MethodIdentifier(name.get(1), descriptor)] = name.get(0)
//        }
//
//        override fun pushClass(name: MappingGetter) {
//            classNames[name.get(0)] = name.get(1)
//        }
//
//    }
//
//}
//
