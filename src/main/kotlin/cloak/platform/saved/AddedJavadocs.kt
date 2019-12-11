package cloak.platform.saved

import cloak.format.rename.Name
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.mutableMap
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.serializer

private val serializer = (StringSerializer to (Name.serializer() to String.serializer()).mutableMap).mutableMap
// Mapped per branch
private val ExtendedPlatform.javadocs: MutableMap<String, MutableMap<Name, String>> by SavedState(
    mutableMapOf(), "Javadocs", serializer
)
