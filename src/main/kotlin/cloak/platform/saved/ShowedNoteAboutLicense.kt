package cloak.platform.saved

import cloak.platform.ExtendedPlatform
import cloak.platform.SavedState
import kotlinx.serialization.internal.BooleanSerializer

var ExtendedPlatform.showedNoteAboutLicense : Boolean by SavedState(false,"ShowedNoteAboutLicense" ,BooleanSerializer)