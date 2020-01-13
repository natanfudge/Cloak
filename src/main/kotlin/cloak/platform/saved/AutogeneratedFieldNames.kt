package cloak.platform.saved


import cloak.fabric.Stitch
import cloak.format.rename.FieldName
import cloak.format.rename.fullyQualifiedName
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
private data class AutogeneratedFieldNames(
    var currentVersion: String? = null,
    var autogeneratedNames: Set<AutoGeneratedField>? = null
)

@Serializable
private data class AutoGeneratedField(val namedClassName: String, val namedFieldName: String)

private var ExtendedPlatform.autoGeneratedFieldNames: AutogeneratedFieldNames by SavedState(
    AutogeneratedFieldNames(),
    "AutoGeneratedFields",
    AutogeneratedFieldNames.serializer()
)

fun ExtendedPlatform.cleanAutoGeneratedFieldNames() {
    autoGeneratedFieldNames = AutogeneratedFieldNames()
}

fun ExtendedPlatform.warmUpAutogeneratedFieldNames(mcJar: File) = runBlocking {
    val names = autoGeneratedFieldNames
    if (names.autogeneratedNames == null || names.currentVersion != branch.getMinecraftVersion()) {
        names.currentVersion = branch.getMinecraftVersion()
        names.autogeneratedNames = Stitch.getAutogeneratedFieldNames(mcJar).map { (field, name) ->
            AutoGeneratedField(namedClassName = field.owner, namedFieldName = field.name)
        }.toSet()
    }
}

fun ExtendedPlatform.isAutogenerated(
    field: FieldName,
    mcJar: File
): Boolean {
    warmUpAutogeneratedFieldNames(mcJar)

    return AutoGeneratedField(
        namedClassName = field.classIn.fullyQualifiedName(), namedFieldName = field.fieldName
    ) in autoGeneratedFieldNames.autogeneratedNames!!
}
