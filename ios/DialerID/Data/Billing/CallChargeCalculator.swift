import Foundation

enum CallChargeCalculator {
    static let minuteSeconds = 60

    static func expectedMinutes(elapsedSeconds: Int) -> Int {
        if elapsedSeconds < 0 { return 0 }
        return max(1, Int(ceil(Double(elapsedSeconds) / Double(minuteSeconds))))
    }

    static func billedMinutes(durationSeconds: Int) -> Int {
        if durationSeconds <= 0 { return 0 }
        return Int(ceil(Double(durationSeconds) / Double(minuteSeconds)))
    }

    static func minuteChargeUsd(markedUpRatePerMin: Double) -> Double {
        if markedUpRatePerMin <= 0 { return 0 }
        return roundUsd(markedUpRatePerMin)
    }

    static func chargeUsd(durationSeconds: Int, markedUpRatePerMin: Double) -> Double {
        let minutes = billedMinutes(durationSeconds: durationSeconds)
        if minutes <= 0 || markedUpRatePerMin <= 0 { return 0 }
        return roundUsd(Double(minutes) * markedUpRatePerMin)
    }

    static func minimumChargeUsd(markedUpRatePerMin: Double) -> Double {
        minuteChargeUsd(markedUpRatePerMin: markedUpRatePerMin)
    }

    private static func roundUsd(_ amount: Double) -> Double {
        (amount * 10_000.0).rounded() / 10_000.0
    }
}
