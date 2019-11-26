package cloak.idea.platformImpl

import cloak.platform.PersistentSaver
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import java.nio.file.Path
import java.nio.file.Paths

 object IdeaPersistentSaver : PersistentSaver() {

    override fun registerProjectCloseCallback(callback: () -> Unit) {
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            ProjectManager.TOPIC, object : ProjectManagerListener {
                override fun projectClosingBeforeSave(project: Project) {
                    callback()
                }
            }
        )
    }


}


