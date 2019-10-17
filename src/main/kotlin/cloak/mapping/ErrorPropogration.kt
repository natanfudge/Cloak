package cloak.mapping

sealed class Errorable<A> {
    abstract fun <C> map(mapping: (A) -> C): Errorable<C>
    abstract fun <C> flatMap(mapping: (A) -> Errorable<C>): Errorable<C>
    abstract fun orElse(other: A): A
    abstract fun orElse(function: (String) -> A): A
}

data class StringSuccess<A>(val value: A) : Errorable<A>() {
    override fun <C> map(mapping: (A) -> C): Errorable<C> = StringSuccess(mapping(value))
    override fun <C> flatMap(mapping: (A) -> Errorable<C>): Errorable<C> = mapping(value)
    override fun orElse(other: A): A = value
    override fun orElse(function: (String) -> A): A = value
}

data class StringError<A>(val value: String) : Errorable<A>() {
    override fun <C> map(mapping: (A) -> C): Errorable<C> = StringError(value)
    override fun <C> flatMap(mapping: (A) -> Errorable<C>): Errorable<C> = StringError(value)
    override fun orElse(other: A): A = other
    override fun orElse(function: (String) -> A): A = function(value)
}

