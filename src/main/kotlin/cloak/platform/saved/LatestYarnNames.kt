//package cloak.platform.saved
//
//import cloak.fabric.YarnNames
//import cloak.platform.ExtendedPlatform
//import cloak.platform.SavedState
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.internal.nullable
//
//
//private var ExtendedPlatform.yarnNames: YarnNames? by SavedState(
//    null,
//    "LatestIntermediaries",
//    YarnNames.serializer().nullable
//)
//
//
//fun ExtendedPlatform.getLatestYarnNames(): YarnNames {
//    if (yarnNames == null) yarnNames = YarnNames.fetchLatestYarnMappings()
//    return yarnNames!!
//}
//
//fun ExtendedPlatform.mapClassIntermediaryToNamed(className: String): String =
//    getLatestYarnNames().classNames[className] ?: className
//
//fun ExtendedPlatform.mapFieldIntermediaryToNamed(fieldName: String): String =
//    getLatestYarnNames().fieldNames[fieldName] ?: fieldName
//
////@Serializable
////private data class YarnNameValues(
////    val fieldNames: Set<String>,
////    val methodNames: Set<String>,
////    val classNames: Set<String>
////)
////
////private var yarnNameValues: YarnNameValues? = null
////
////private fun ExtendedPlatform.getYarnNameValues() : YarnNameValues{
////    if (yarnNameValues == null) {
////        yarnNameValues = getLatestYarnNames()
////            .run { YarnNameValues(fieldNames.values.toSet(), methodNames.values.toSet(), classNames.values.toSet()) }
////    }
////    return yarnNameValues!!
////}
