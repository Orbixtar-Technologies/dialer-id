import Foundation

enum RateMarkup {
    static let rateMarkupPercent = 40
    static let rateMarkupMultiplier = 1.40

    static func apply(_ wholesaleRatePerMin: Double) -> Double {
        wholesaleRatePerMin * rateMarkupMultiplier
    }
}
