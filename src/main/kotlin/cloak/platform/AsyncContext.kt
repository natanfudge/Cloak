package cloak.platform

interface AsyncContext {
    var text: String
}

inline fun <T> AsyncContext.changeText(newText: String, forCode: () -> T): T {
    val old = text
    text = newText
    val value = forCode()
    text = old
    return value
}