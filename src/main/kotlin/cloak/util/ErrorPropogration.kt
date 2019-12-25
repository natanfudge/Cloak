@file:Suppress("NOTHING_TO_INLINE")

package cloak.util

import cloak.platform.saved.DualResult
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok


inline fun <V, E> DualResult<V, E>.getOrElseError(err: (Err<E>) -> V): V = when (this) {
    is Ok<V> -> value
    is Err<E> -> err(this)
}

val <T> T.success get() = Ok(this)
val UnitSuccess = Unit.success
inline fun success() = UnitSuccess
inline fun fail(error: String) = Err(error)
