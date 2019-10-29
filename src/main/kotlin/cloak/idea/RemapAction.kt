package cloak.idea

import RenamedNamesProvider
import cloak.idea.util.*
import cloak.mapping.StringSuccess
import cloak.mapping.rename.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//TODO: list of default ignored auto-imports
//TODO: "new project" dialog: Kotlin/Java, include publishing block
//TODO: turn on "autoscroll from source" by default
//TODO: give cool icon to fabric.mod.json and modid.mixin.json
//TODO: inspections for fabric stuff?


class RemapAction : Action("Hello") {
    override fun isEnabledAndVisible(event: AnActionEvent): Boolean {
        val element = event.psiElement ?: return false
        // Only allow minecraft classes
        if (!element.packageName.startsWith("net.minecraft")) return false


        return when (element) {
            is PsiClass, is PsiField, is PsiParameter -> true
            is PsiMethod -> !element.isConstructor
            else -> false
        }
    }
    //TODO: allow going back to already submitted branches to fix after review

    //TODO: test renaming package


    //TODO: bigger screen that can fit a reason
    //TODO: use bigger screen to get git username and email input
    override fun actionPerformed(event: AnActionEvent) {
        val element = event.psiElement ?: return
        val isTopLevelClass = element is PsiClass && !element.isInnerClass
        val nameBeforeName = element.asName()
        val name = nameBeforeName.updateAccordingToRenames(RenamedNamesProvider.getInstance())

        GlobalScope.launch {
            val result = Renamer.rename(IdeaProjectWrapper(event.project ?: return@launch), name, isTopLevelClass)
            if (result is StringSuccess) {
                println("$name was renamed to ${result.value}")
                RenamedNamesProvider.getInstance().addRenamedName(nameBeforeName, result.value)
            } else {
                println("Could not rename: $result")
            }
        }
    }


}

/**
 * After the player renames something, the yarn repository has different information than what is in the editor.
 * This updates the information in the editor to be up to date to the repo.
 * */
private fun Name.updateAccordingToRenames(renames: RenamedNamesProvider) = when (this) {
    is ClassName -> updateAccordingToRenames(renames)
    is FieldName -> updateAccordingToRenames(renames)
    is MethodName -> updateAccordingToRenames(renames)
    is ParamName -> updateAccordingToRenames(renames)
}

private fun ClassName.updateAccordingToRenames(renames: RenamedNamesProvider): ClassName =
    renames.getRenameOf(this)?.let { copy(className = it) } ?: this

private fun FieldName.updateAccordingToRenames(renames: RenamedNamesProvider): FieldName {
    val newClassName = classIn.updateAccordingToRenames(renames)
    return renames.getRenameOf(this)?.let { copy(fieldName = it, classIn = newClassName) }
        ?: copy(classIn = newClassName)
}

private fun MethodName.updateAccordingToRenames(renames: RenamedNamesProvider): MethodName {
    val newClassName = classIn.updateAccordingToRenames(renames)
    return renames.getRenameOf(this)?.let { copy(methodName = it, classIn = newClassName) }
        ?: copy(classIn = newClassName)
}

private fun ParamName.updateAccordingToRenames(renames: RenamedNamesProvider) =
    copy(methodIn = methodIn.updateAccordingToRenames(renames))

//TODO: when we have yarn that can work on any version:
// - Have an option to use mappings from the newest version,
// have a blue button thing to remap the jar and optionally generate sources again.
// You can also choose to "auto update", which will essentially do that after every rename.
// This button also converts the mappings from yarn to tiny and puts them in the project folder, so it can be checked
// into source control. when this is done all the green marks reset.
// The green marks reset if the jar gets remapped by some other way as well.
//TODO: the user must first use the correct mappings that will be generated, by using an intention that copies the correct
// build.gradle line to clipboard.


