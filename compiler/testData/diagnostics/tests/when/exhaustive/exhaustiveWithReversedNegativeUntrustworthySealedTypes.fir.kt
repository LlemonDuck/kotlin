// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_MESSAGES

open class PhantomEquivalence {
    override fun equals(other: Any?) = other is PhantomEquivalence
}

sealed interface Variants {
    data object A : Variants
    object B : PhantomEquivalence(), Variants
}

fun foo(v: Variants): String {
    if (v == Variants.B) {
        return "B"
    }

    return when (v) {
        Variants.A -> "A"
    }
}

fun bar(v: Variants): String {
    if (v == Variants.B) {
        return "B"
    }

    return when (v) {
        Variants.A -> "A"
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> "C"
    }
}
