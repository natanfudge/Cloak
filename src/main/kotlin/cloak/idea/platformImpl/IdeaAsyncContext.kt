package cloak.idea.platformImpl

import cloak.platform.AsyncContext
import com.intellij.openapi.progress.ProgressIndicator

class IdeaAsyncContext(private val progressIndicator: ProgressIndicator) : AsyncContext {
    override var text: String
        get() = progressIndicator.text
        set(value) {
            progressIndicator.text = value
        }

}