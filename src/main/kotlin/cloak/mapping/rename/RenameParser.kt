package cloak.mapping.rename

//@file:Suppress("UnnecessaryVariable")
//
//package cloak.mapping
//
//import javax.lang.model.SourceVersion
//

//
//fun parseRename(
//    rawOldName: String,
//    rawNewName: String,
//    explanation: String?
//): Errorable<Rename<*>> {
//    val (oldNamePackage, oldName) = splitPackageAndName(rawOldName)
//    val (newNamePackage, newName) = splitPackageAndName(rawNewName)
//    for (part in newNamePackage?.split("/") ?: listOf()) {
//        if (!SourceVersion.isIdentifier(part)) return fail("'$part' is not a valid package name")
//    }
//
//    if (!SourceVersion.isName(newName)) return fail("'$newName' is not a valid class name")
//
//    val oldNameParsed = when (val oldNameParsedOrError = parseName(oldName)) {
//        is StringSuccess -> oldNameParsedOrError.value
//        is StringError -> return StringError(oldNameParsedOrError.value)
//    }
//
//
//    if ((oldNameParsed !is ClassName || (oldNameParsed.innerClass != null))) {
//        return fail("Changing the package name can only be done on top-level classes")
//    }
//
//    return Rename(
//        originalName = OriginalName(oldNameParsed, oldNamePackage),
//        explanation = explanation,
//        newName = newName,
//        newPackageName = newNamePackage,
//        byObfuscated = keyWord == KeyWord.Name
//    ).success
//
//}
//
//
//private fun splitPackageAndName(rawName: String): Pair<String?, String> {
//    val lastSlashIndex = rawName.lastIndexOf('/')
//    return if (lastSlashIndex == -1) null to rawName
//    else rawName.splitOn(lastSlashIndex)
//}
//
//
//// The return type on this method was not simple to figure out :D
//fun parseName(name: String): Errorable<out Name<*>> {
//    // Yes, the split index is being found twice, this is slightly inefficient.
//    val lastSplitter = name.findLastAnyOf(Joiner.All)
//
//    return if (lastSplitter != null) {
//        when (lastSplitter.second) {
//            Joiner.Method -> parseMethod(name)
//            Joiner.Field -> parseField(name)
//            Joiner.InnerClass -> parseClass(name)
//            Joiner.Parameter -> parseParameter(name)
//            else -> error("Impossible")
//        }
//    } else parseClass(name)
//
//}
//
//// Note that classes are parsed from left to right, and methods, fields and parameter are parsed from right to left
//private fun parseClass(name: String): Errorable<ClassName> {
//    val splitIndex = name.indexOf(Joiner.InnerClass)
//    if (splitIndex == -1) {
//        return ClassName(name, innerClass = null).success
////        return if (!SourceVersion.isName(name)) fail("'$name' is not a valid class name")
////        else ClassName(name, innerClass = null).success
//    }
//
//    val (outerClass, innerClass) = name.splitOn(splitIndex)
//    if (!SourceVersion.isName(name)) fail<ClassName>("'$name' is not a valid class name")
//
//    return parseClass(innerClass).map { ClassName(outerClass, innerClass = it) }
//}
//
//private fun parseMethod(name: String): Errorable<MethodName> {
//    val splitIndex = name.lastIndexOf(Joiner.Method)
//    if (splitIndex == -1) return fail("Expected '$name' to be a method")
//    val (className, methodName) = name.splitOn(splitIndex)
////    if (!SourceVersion.isName(methodName)) return fail("'$methodName' is not a valid method name")
//
//    //TODO: parse parameter types
//    return parseClass(className).map { MethodName(methodName, it, null) }
//}
//
//private fun parseField(name: String): Errorable<FieldName> {
//    val splitIndex = name.lastIndexOf(Joiner.Field)
//    if (splitIndex == -1) return fail("Expected '$name' to be a field")
//    val (className, fieldName) = name.splitOn(splitIndex)
////    if (!SourceVersion.isName(fieldName)) return fail("'$fieldName' is not a valid field name")
//
//    return parseClass(className).map { FieldName(fieldName, it) }
//}
//
//private fun parseParameter(name: String): Errorable<ParameterName> {
//    val splitIndex = name.lastIndexOf(Joiner.Parameter)
//    if (splitIndex == -1) return fail("Expected '$name' to be a parameter")
//    val (methodName, parameterIndexOrNameWithBracket) = name.splitOn(splitIndex)
//    val parameterIndexOrName = parameterIndexOrNameWithBracket.removeSuffix("]")
//    parameterIndexOrName.toIntOrNull()?.let { parameterIndex ->
//        return parseMethod(methodName).map { ParameterName.ByIndex(parameterIndex, it) }
//    }
//
//    val parameterName = parameterIndexOrName
//
////    if (!SourceVersion.isName(parameterName)) return fail("'$parameterName' is not a valid parameter name")
//    return parseMethod(methodName).map { ParameterName.ByName(parameterName, it) }
//}
//
//
//object Joiner {
//    const val Method = "#"
//    const val Field = "%"
//    const val InnerClass = "$"
//    const val Parameter = "["
//    val All = listOf(Method, Field, InnerClass, Parameter)
//}
//
