package cloak.platform

data class GitUser(val name: String) {
    val branchName get() = name
}