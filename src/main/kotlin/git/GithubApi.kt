package cloak.git

object GithubApi {
    fun getToken(): String = System.getenv("CLOAK_GITHUB_TOKEN")
}