//// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
//package cloak.idea.migrate
//
//import com.intellij.application.options.CodeStyle
//import com.intellij.openapi.application.PathManager
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.util.InvalidDataException
//import com.intellij.openapi.util.JDOMUtil
//import com.intellij.openapi.util.io.FileFilters
//import com.intellij.openapi.util.io.FileUtil
//import com.intellij.openapi.util.io.FileUtilRt
//import com.intellij.openapi.util.text.StringUtil
//import com.intellij.refactoring.migration.PredefinedMigrationProvider
//import com.intellij.util.text.UniqueNameGenerator
//import org.jdom.Document
//import org.jdom.Element
//import org.jdom.JDOMException
//import org.jetbrains.annotations.NonNls
//import java.io.File
//import java.io.FileOutputStream
//import java.io.IOException
//import java.net.URL
//import java.util.*
//
//class CloakMigrationMapSet {
//    private var myMaps: ArrayList<CloakMigrationMap>? = null
//    private val myDeletedMaps: MutableSet<String?> = TreeSet()
//    fun addMap(map: CloakMigrationMap) {
//        if (myMaps == null) {
//            loadMaps()
//        }
//        myMaps!!.add(map)
//    }
//
//    fun findMigrationMap(name: String): CloakMigrationMap? {
//        if (myMaps == null) {
//            loadMaps()
//        }
//        for (map in myMaps!!) {
//            if (name == map.name) {
//                return map
//            }
//        }
//        return null
//    }
//
//    fun replaceMap(oldMap: CloakMigrationMap, newMap: CloakMigrationMap) {
//        for (i in myMaps!!.indices) {
//            if (myMaps!![i] == oldMap) {
//                myMaps!![i] = newMap
//            }
//        }
//    }
//
//    fun removeMap(map: CloakMigrationMap) {
//        if (myMaps == null) {
//            loadMaps()
//        }
//        myMaps!!.remove(map)
//        val name = map.fileName
//        if (isPredefined(name)) {
//            myDeletedMaps.add(name)
//        }
//    }
//
//    val maps: Array<CloakMigrationMap?>
//        get() {
//            if (myMaps == null) {
//                loadMaps()
//            }
//            val ret = arrayOfNulls<CloakMigrationMap>(myMaps!!.size)
//            for (i in myMaps!!.indices) {
//                ret[i] = myMaps!![i]
//            }
//            return ret
//        }
//
//    private fun copyPredefinedMaps(dir: File?) {
//        val deletedFiles = File(dir, "deleted.txt")
//        if (deletedFiles.isFile) {
//            try {
//                myDeletedMaps.addAll(
//                    Arrays.asList(
//                        *FileUtil.loadFile(
//                            deletedFiles,
//                            true
//                        ).split("\n".toRegex()).toTypedArray()
//                    )
//                )
//            } catch (e: IOException) {
//                LOG.error(e)
//            }
//        }
//        for (provider in PredefinedMigrationProvider.EP_NAME.extensionList) {
//            val migrationMap = provider.migrationMap
//            val fileName = File(migrationMap.file).name
//            if (myDeletedMaps.contains(FileUtilRt.getNameWithoutExtension(fileName))) continue
//            copyMap(dir, migrationMap, fileName)
//        }
//        for (defaultTemplate in DEFAULT_MAPS) {
//            val url = CloakMigrationMapSet::class.java.getResource(defaultTemplate)
//            LOG.assertTrue(url != null)
//            val fileName = defaultTemplate.substring(defaultTemplate.lastIndexOf("/") + 1)
//            if (myDeletedMaps.contains(FileUtilRt.getNameWithoutExtension(fileName))) continue
//            copyMap(dir, url, fileName)
//        }
//    }
//
//    private fun loadMaps() {
//        myMaps = ArrayList()
//        val dir = mapDirectory
//        copyPredefinedMaps(dir)
//        val files = getMapFiles(dir)
//        for (file in files) {
//            try {
//                val map = readMap(file)
//                if (map != null) {
//                    map.fileName = FileUtilRt.getNameWithoutExtension(file!!.name)
//                    myMaps!!.add(map)
//                }
//            } catch (e: InvalidDataException) {
//                LOG.error("Invalid data in file: " + file!!.absolutePath)
//            } catch (e: JDOMException) {
//                LOG.error("Invalid data in file: " + file!!.absolutePath)
//            } catch (e: IOException) {
//                LOG.error(e)
//            }
//        }
//    }
//
//    @Throws(IOException::class)
//    fun saveMaps() {
//        val dir = mapDirectory ?: return
//        val files = getMapFiles(dir)
//        @NonNls val filePaths =
//            arrayOfNulls<String>(myMaps!!.size)
//        val documents = arrayOfNulls<Document>(myMaps!!.size)
//        val namesProvider = UniqueNameGenerator()
//        for (i in myMaps!!.indices) {
//            val map = myMaps!![i]
//            filePaths[i] =
//                dir.toString() + File.separator + namesProvider.generateUniqueName(map.fileName!!) + ".xml"
//            documents[i] = saveMap(map)
//        }
//        JDOMUtil.updateFileSet(
//            files,
//            filePaths,
//            documents,
//            CodeStyle.getDefaultSettings().lineSeparator
//        )
//        if (!myDeletedMaps.isEmpty()) {
//            FileUtil.writeToFile(
//                File(dir, "deleted.txt"),
//                StringUtil.join(myDeletedMaps, "\n")
//            )
//        }
//    }
//
//    companion object {
//        private val LOG =
//            Logger.getInstance("#com.intellij.refactoring.migration.MigrationMapSet")
//        @NonNls
//        private val MIGRATION_MAP = "migrationMap"
//        @NonNls
//        private val ENTRY = "entry"
//        @NonNls
//        private val NAME = "name"
//        @NonNls
//        private val OLD_NAME = "oldName"
//        @NonNls
//        private val NEW_NAME = "newName"
//        @NonNls
//        private val DESCRIPTION = "description"
//        @NonNls
//        private val VALUE = "value"
//        @NonNls
//        private val TYPE = "type"
//        @NonNls
//        private val PACKAGE_TYPE = "package"
//        @NonNls
//        private val CLASS_TYPE = "class"
//        @NonNls
//        private val RECURSIVE = "recursive"
//        @NonNls
//        private val DEFAULT_MAPS = arrayOf(
//            "/com/intellij/refactoring/migration/res/Swing__1_0_3____1_1_.xml"
//        )
//
//        private fun isPredefined(name: String?): Boolean {
//            for (provider in PredefinedMigrationProvider.EP_NAME.extensionList) {
//                val migrationMap = provider.migrationMap
//                val fileName =
//                    FileUtilRt.getNameWithoutExtension(File(migrationMap.file).name)
//                if (fileName == name) return true
//            }
//            for (defaultTemplate in DEFAULT_MAPS) {
//                val fileName = FileUtilRt.getNameWithoutExtension(
//                    StringUtil.getShortName(
//                        defaultTemplate,
//                        '/'
//                    )
//                )
//                if (fileName == name) return true
//            }
//            return false
//        }
//
//        private val mapDirectory: File?
//            private get() {
//                val dir = File(PathManager.getConfigPath() + File.separator + "migration")
//                if (!dir.exists() && !dir.mkdirs()) {
//                    LOG.error("cannot create directory: " + dir.absolutePath)
//                    return null
//                }
//                return dir
//            }
//
//        private fun copyMap(dir: File?, url: URL?, fileName: String) {
//            val targetFile = File(dir, fileName)
//            if (targetFile.isFile) return
//            try {
//                FileOutputStream(targetFile).use { outputStream ->
//                    url!!.openStream()
//                        .use { inputStream -> FileUtil.copy(inputStream, outputStream) }
//                }
//            } catch (e: Exception) {
//                LOG.error(e)
//            }
//        }
//
//        private fun getMapFiles(dir: File?): Array<File?> {
//            if (dir == null) {
//                return arrayOfNulls(0)
//            }
//            val ret = dir.listFiles(FileFilters.filesWithExtension("xml"))
//            if (ret == null) {
//                LOG.error("cannot read directory: " + dir.absolutePath)
//                return arrayOfNulls(0)
//            }
//            return ret
//        }
//
//        @Throws(
//            JDOMException::class,
//            InvalidDataException::class,
//            IOException::class
//        )
//        private fun readMap(file: File?): CloakMigrationMap? {
//            if (!file!!.exists()) {
//                return null
//            }
//            val root = JDOMUtil.load(file)
//            if (MIGRATION_MAP != root.name) {
//                throw InvalidDataException()
//            }
//            val map = CloakMigrationMap()
//            for (node in root.children) {
//                if (NAME == node.name) {
//                    val name = node.getAttributeValue(VALUE)
//                    map.name = name
//                }
//                if (DESCRIPTION == node.name) {
//                    val description = node.getAttributeValue(VALUE)
//                    map.description = description
//                }
//                if (ENTRY == node.name) {
//                    val entry = CloakMigrationMapEntry()
//                    val oldName = node.getAttributeValue(OLD_NAME) ?: throw InvalidDataException()
//                    entry.oldName = oldName
//                    val newName = node.getAttributeValue(NEW_NAME) ?: throw InvalidDataException()
//                    entry.newName = newName
//                    val typeStr = node.getAttributeValue(TYPE) ?: throw InvalidDataException()
//                    entry.type = CloakMigrationMapEntry.CLASS
//                    if (typeStr == PACKAGE_TYPE) {
//                        entry.type = CloakMigrationMapEntry.PACKAGE
//                        @NonNls val isRecursiveStr =
//                            node.getAttributeValue(RECURSIVE)
//                        if (isRecursiveStr != null && isRecursiveStr == "true") {
//                            entry.isRecursive = true
//                        } else {
//                            entry.isRecursive = false
//                        }
//                    }
//                    map.addEntry(entry)
//                }
//            }
//            return map
//        }
//
//        private fun saveMap(map: CloakMigrationMap): Document {
//            val root = Element(MIGRATION_MAP)
//            val nameElement = Element(NAME)
//            nameElement.setAttribute(VALUE, map.name)
//            root.addContent(nameElement)
//            val descriptionElement = Element(DESCRIPTION)
//            descriptionElement.setAttribute(VALUE, map.description)
//            root.addContent(descriptionElement)
//            for (i in 0 until map.entryCount) {
//                val entry = map.getEntryAt(i)
//                val element = Element(ENTRY)
//                element.setAttribute(OLD_NAME, entry.oldName)
//                element.setAttribute(NEW_NAME, entry.newName)
//                if (entry.type == CloakMigrationMapEntry.PACKAGE) {
//                    element.setAttribute(
//                        TYPE,
//                        PACKAGE_TYPE
//                    )
//                    element.setAttribute(
//                        RECURSIVE,
//                        java.lang.Boolean.valueOf(entry.isRecursive).toString()
//                    )
//                } else {
//                    element.setAttribute(TYPE, CLASS_TYPE)
//                }
//                root.addContent(element)
//            }
//            return Document(root)
//        }
//    }
//}