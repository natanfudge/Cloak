package cloak.util

import RenameErrorTests.Companion.TestAuthor
import cloak.git.CloakRepository
import cloak.git.JGit
import cloak.platform.*
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

private object TestAsyncContext : AsyncContext{
    override var text: String
        get() = "Test async"
        set(_) {}

}

class TestPlatform(private val renameInput: Pair<String, String?>? = null, private val javaDocInput: String? = null) :
    ExtendedPlatform {
    override fun getStorageDirectory(): Path = Paths.get(System.getProperty("user.dir"), "caches")
    override val persistentSaver: PersistentSaver = object : PersistentSaver() {
        override fun registerProjectCloseCallback(callback: () -> Unit) {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    callback()
                }
            })
        }
    }

    override suspend fun getTwoInputs(
        message: String?,
        request: UserInputRequest,
        helpId: String?,
        inputA: InputFieldData,
        inputB: InputFieldData
    ): Pair<String, String?>? = when (request) {
        UserInputRequest.NewName -> renameInput
//        UserInputRequest.GitUserAuthor -> Pair(TestAuthor.name, TestAuthor.email)
        else -> error("Unexpected")
    }


    override suspend fun showMessageDialog(message: String, title: String) {}
    override suspend fun showErrorPopup(message: String, title: String) {}
    override suspend fun getUserInput(title: String, message: String, validator: PlatformInputValidator?) = null
    override suspend fun getMultilineInput(
        title: String,
        message: String,
        validator: PlatformInputValidator?,
        initialValue: String?
    ): String? {
        return javaDocInput
    }

    override suspend fun getChoiceBetweenOptions(title: String, options: List<String>): String {
        return ""
    }

    override suspend fun getMultipleChoicesBetweenOptions(title: String, options: List<String>): List<String> {
        return listOf()
    }

    override suspend fun getJavadocInput(title: String, oldJavadoc: String): String? {
        return javaDocInput
    }

    override suspend fun <T> asyncWithText(title: String, action: suspend (AsyncContext) -> T): T = action(TestAsyncContext)

//    override suspend fun <T> asyncWithText(title: String, action: suspend () -> T): T = action()
    override fun createPullRequest(
        repositoryName: String,
        requestingUser: String,
        requestingBranch: String,
        targetBranch: String,
        targetUser: String,
        title: String,
        body: String
    ): PullRequestResponse {
        return PullRequestResponse("")
    }

    override fun forkRepository(repositoryName: String, forkedUser: String, forkingUser: String): ForkResult {
        return ForkResult.Canceled
    }

    override suspend fun getAuthenticatedUser(): GitUser {
        return TestAuthor
    }


    override fun createGit(git: JGit, path: File): CloakRepository {
        return TestRepository(git, path)
    }

}

class TestRepository(git: JGit, path: File) : CloakRepository(git, path) {
    override fun deleteBranch(remoteUrl: String, branchName: String) {
    }

    override fun commit(commitMessage: String) {

    }

    override fun push(remoteUrl: String, branch: String, refSpec: String) {
    }

}
