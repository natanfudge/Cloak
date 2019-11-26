package cloak.fabric

import cloak.format.descriptor.FieldDescriptor
import cloak.format.descriptor.MethodDescriptor
import cloak.format.descriptor.read
import cloak.format.descriptor.remap
import cloak.platform.saved.LatestIntermediaryNames
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
    // Official class name -> intermediary class name
    val classNames = mutableMapOf<String, String>()
    // Field name -> field descriptor
    val fieldNames = mutableMapOf<String, String>()
    // Method name -> method descriptor
    val methodNames = mutableMapOf<String, String>()

    override fun pushField(name: MappingGetter, descriptor: String) {
        fieldNames[name.get(1)] = descriptor
    }

    override fun pushMethod(name: MappingGetter, descriptor: String) {
        methodNames[name.get(1)] = descriptor
    }

    override fun pushClass(name: MappingGetter) {
        classNames[name.get(0)] = name.get(1)
    }

    fun getNames(): LatestIntermediaryNames {
        val remappedMethodNames = methodNames
            .mapValues { MethodDescriptor.read(it.value).remap(classNames).classFileName }
        val remappedFieldNames = fieldNames
            .mapValues { FieldDescriptor.read(it.value).remap(classNames).classFileName }
        return LatestIntermediaryNames(
            classNames.values.toSet(),
            remappedMethodNames,
            remappedFieldNames
        )
    }

}