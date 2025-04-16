sealed class SC1 {
    class C1 : SC1()
    data object O1 : SC1()
//    class Added : SC1()
    class C2 : SC1()
    data object O2 : SC1()
}

sealed class SC2 {
    class C1 : SC2()
    data object O1 : SC2()
//    data object Added : SC2()
    class C2 : SC2()
    data object O2 : SC2()
}

sealed interface SI1 {
    class C1 : SI1
    data object O1 : SI1
    interface I1 : SI1
//    class Added : SI1
    class C2 : SI1
    data object O2 : SI1
    interface I2 : SI1
}

sealed interface SI2 {
    class C1 : SI2
    data object O1 : SI2
    interface I1 : SI2
//    data object Added : SI2
    class C2 : SI2
    data object O2 : SI2
    interface I2 : SI2
}

sealed interface SI3 {
    class C1 : SI3
    data object O1 : SI3
    interface I1 : SI3
//    interface Added : SI3
    class C2 : SI3
    data object O2 : SI3
    interface I2 : SI3
}
