package cloak.util

import RenameErrorTests.Companion.TestAuthor
import cloak.git.CloakRepository
import cloak.git.JGit
import cloak.platform.*
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths


class TestPlatform(private val renameInput: Pair<String, String?>? = null, private val javaDocInput: String? = null) :
    ExtendedPlatform {
    override val storageDirectory: Path = Paths.get(System.getProperty("user.dir"), "caches")
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
        descriptionA: String?,
        descriptionB: String?,
        initialValueA: String?,
        initialValueB: String?,
        defaultSelectionA: IntRange?,
        defaultSelectionB: IntRange?,
        validatorA: PlatformInputValidator?,
        validatorB: PlatformInputValidator?
    ): Pair<String, String?>? = when (request) {
        UserInputRequest.NewName -> renameInput
//        UserInputRequest.GitUserAuthor -> Pair(TestAuthor.name, TestAuthor.email)
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

    override suspend fun <T> asyncWithText(title: String, action: suspend () -> T): T = action()
    override fun createPullRequest(
        repositoryName: String,
        requestingUser: String,
        requestingBranch: String,
        targetBranch: String,
        targetUser: String,
        title: String,
        body: String
    ): PullRequestResponse? {
        return null
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

class TestRepository(override val git: JGit, override val path: File) : CloakRepository() {
    override fun deleteBranch(remoteUrl: String, branchName: String) {
    }

    override fun commit(commitMessage: String) {

    }

    override fun push(remoteUrl: String, branch: String, refSpec: String) {
    }

}
