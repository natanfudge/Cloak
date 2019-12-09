package cloak.platform

data class PullRequestResponse(val prUrl: String)

enum class ForkResult {
    Canceled,
    Success,
    AlreadyForked
}