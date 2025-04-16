// RUN_PIPELINE_TILL: BACKEND
sealed class Sealed {
    object First: Sealed()
    sealed class NonFirst {
        object Second: NonFirst()
        object Third: NonFirst()
        object Fourth: Sealed()
    }
}

fun foo(s: Sealed, nf: Sealed.NonFirst): Int {
    val si = when(s) {
        <!UNSAFE_EXHAUSTIVENESS!>Sealed.First<!> -> 1
        <!UNSAFE_EXHAUSTIVENESS!>Sealed.NonFirst.Fourth<!> -> 4
    }
    val nfi = when(nf) {
        <!UNSAFE_EXHAUSTIVENESS!>Sealed.NonFirst.Second<!> -> 2
        <!UNSAFE_EXHAUSTIVENESS!>Sealed.NonFirst.Third<!> -> 3
    }
    return si + nfi
}
