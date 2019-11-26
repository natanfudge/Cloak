package cloak.platform

enum class UserInputRequest(val title: String) {
    GitUserAuthor("Identification"),
    NewName("Rename")
}