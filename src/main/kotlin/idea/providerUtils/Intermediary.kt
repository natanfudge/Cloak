package cloak.idea.providerUtils

import com.google.common.net.UrlEscapers
import net.fabricmc.mapping.reader.v2.MappingGetter
import net.fabricmc.mapping.reader.v2.TinyV2Factory
import net.fabricmc.mapping.reader.v2.TinyVisitor
import org.apache.commons.io.FileUtils
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files

object Intermediary{
    fun fetch(mcVersion: String): List<String> {
        val escapedVersion = UrlEscapers.urlFragmentEscaper().escape(mcVersion);
        val url = "https://maven.fabricmc.net/net/fabricmc/intermediary/$escapedVersion/intermediary-$escapedVersion-v2.jar"
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
    private val names = mutableListOf<String>()
    override fun pushField(name: MappingGetter, descriptor: String) {
        names.add(name.get(1))
    }

    override fun pushMethod(name: MappingGetter, descriptor: String) {
        names.add(name.get(1))
    }

    override fun pushClass(name: MappingGetter) {
        names.add(name.get(1))
    }

    fun getNames() = names
}