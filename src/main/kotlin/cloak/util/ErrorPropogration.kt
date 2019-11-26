@file:Suppress("NOTHING_TO_INLINE")

package cloak.util

sealed class Errorable<A> {
    abstract fun <C> map(mapping: (A) -> C): Errorable<C>
    abstract fun <C> flatMap(mapping: (A) -> Errorable<C>): Errorable<C>
    abstract fun orElse(other: A): A
    abstract fun orElse(function: (String) -> A): A
}


data class StringSuccess<A>(val value: A) : Errorable<A>() {
    override fun toString() = "Success: $value"
    override fun <C> map(mapping: (A) -> C): Errorable<C> =
        StringSuccess(mapping(value))
    override fun <C> flatMap(mapping: (A) -> Errorable<C>): Errorable<C> = mapping(value)
    override fun orElse(other: A): A = value
    override fun orElse(function: (String) -> A): A = value
}

data class StringError<A>(val value: String) : Errorable<A>() {
    override fun toString() = "Error: $value"
    override fun <C> map(mapping: (A) -> C): Errorable<C> =
        StringError(value)
    override fun <C> flatMap(mapping: (A) -> Errorable<C>): Errorable<C> =
        StringError(value)
    override fun orElse(other: A): A = other
    override fun orElse(function: (String) -> A): A = function(value)
}

val <T> T.success get() = StringSuccess(this)
val UnitSuccess = Unit.success
inline fun success() = UnitSuccess
inline fun <T> fail(error: String) = StringError<T>(error)
