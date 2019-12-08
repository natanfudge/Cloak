package cloak.platform.saved

import cloak.platform.DebugDump
import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import cloak.util.DebugJson
import kotlinx.serialization.internal.BooleanSerializer


var ExtendedPlatform.showedNoteAboutLicense: Boolean by SavedState(false, "ShowedNoteAboutLicense", BooleanSerializer)
fun DebugDump.showedNoteAboutLicenseDump() = DebugJson.stringify(BooleanSerializer, platform.showedNoteAboutLicense)