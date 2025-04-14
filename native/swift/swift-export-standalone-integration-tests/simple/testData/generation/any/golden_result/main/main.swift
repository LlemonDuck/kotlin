@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class MyObject: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public static var shared: main.MyObject {
        get {
            return main.MyObject.__create(externalRCRef: __root___MyObject_get())
        }
    }
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        cache: Swift.Bool,
        substitute: Swift.Bool
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, cache: cache, substitute: substitute)
    }
}
public func getMainObject() -> KotlinRuntime.KotlinBase {
    return KotlinRuntime.KotlinBase.__create(externalRCRef: __root___getMainObject())
}
public func isMainObject(
    obj: KotlinRuntime.KotlinBase
) -> Swift.Bool {
    return __root___isMainObject__TypesOfArguments__KotlinRuntime_KotlinBase__(obj.__externalRCRef())
}
