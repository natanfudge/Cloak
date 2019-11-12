package cloak.fabric

import cloak.idea.LatestIntermediaryNames
import com.google.common.net.UrlEscapers
import net.fabricmc.mapping.reader.v2.MappingGetter
import net.fabricmc.mapping.reader.v2.TinyV2Factory
import net.fabricmc.mapping.reader.v2.TinyVisitor
import org.apache.commons.io.FileUtils
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files

object Intermediary {
    fun fetchExistingNames(mcVersion: String): LatestIntermediaryNames {
        val escapedVersion = UrlEscapers.urlFragmentEscaper().escape(mcVersion);
        val url =
            "https://maven.fabricmc.net/net/fabricmc/intermediary/$escapedVersion/intermediary-$escapedVersion-v2.jar"
        val jar = Files.createTempFile("intermediaries", ".jar")
        FileUtils.copyURLToFile(URL(url), jar.toFile())
        return FileSystems.newFileSystem(jar, null).use { jarFs ->
            Files.newBufferedReader(jarFs.getPath("mappings/mappings.tiny")).use {
                val visitor = Visitor()
                TinyV2Factory.visit(it, visitor)
                visitor.getNames()
            }
        }
    }
}


private class Visitor : TinyVisitor {
    val classNames = mutableSetOf<String>()
    val fieldNames = mutableMapOf<String,String>()
    val methodNames = mutableMapOf<String,String>()
    override fun pushField(name: MappingGetter, descriptor: String) {
        fieldNames[name.get(1)] = descriptor
    }

    override fun pushMethod(name: MappingGetter, descriptor: String) {
        methodNames[name.get(1)] = descriptor
    }

    override fun pushClass(name: MappingGetter) {
        classNames.add(name.get(1))
    }

    fun getNames() = LatestIntermediaryNames(classNames, methodNames, fieldNames)

}