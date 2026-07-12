import Foundation
import sharedKit

func toKotlinDouble(_ value: Double) -> KotlinDouble { KotlinDouble(value: value) }

func toKotlinDouble(_ value: Double?) -> KotlinDouble? {
    value.map { KotlinDouble(value: $0) }
}

func toKotlinBoolean(_ value: Bool) -> KotlinBoolean { KotlinBoolean(value: value) }

extension KotlinDouble {
    var asDouble: Double { doubleValue }
}

extension KotlinBoolean {
    var asBool: Bool { boolValue }
}

extension Dictionary where Value == KotlinBoolean {
    var asBoolDict: [Key: Bool] { mapValues(\.asBool) }
}

extension Dictionary where Value == Bool {
    var asKotlinBoolDict: [Key: KotlinBoolean] { mapValues { KotlinBoolean(value: $0) } }
}

func fmt(_ value: KotlinDouble, decimals: Int = 2) -> String {
    fmt(value.asDouble, decimals: decimals)
}
