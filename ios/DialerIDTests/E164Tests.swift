import XCTest
@testable import DialerID

final class E164Tests: XCTestCase {
    func testAlreadyE164IsNormalized() {
        XCTAssertEqual(E164.format("+1 415 555 2671", defaultRegion: "US"), "+14155552671")
        XCTAssertEqual(E164.format("+14155552671", defaultRegion: "GB"), "+14155552671")
    }

    func testUsTenDigitBecomesPlusOne() {
        XCTAssertEqual(E164.format("4155552671", defaultRegion: "US"), "+14155552671")
        XCTAssertEqual(E164.format("415-555-2671", defaultRegion: "US"), "+14155552671")
    }

    func testInternationalZeroZeroBecomesPlus() {
        XCTAssertEqual(E164.format("00442079460000", defaultRegion: "US"), "+442079460000")
        XCTAssertEqual(E164.format("0044 20 7946 0000", defaultRegion: "GB"), "+442079460000")
    }

    func testShortCodesStayAsDialed() {
        XCTAssertEqual(E164.format("3200", defaultRegion: "US"), "3200")
        XCTAssertEqual(E164.format("444", defaultRegion: "GB"), "444")
        XCTAssertEqual(E164.format("+3200", defaultRegion: "US"), "3200")
    }

    func testUkNationalUsesDefaultRegion() {
        XCTAssertEqual(E164.format("02079460000", defaultRegion: "GB"), "+442079460000")
    }

    func testPakistanTrunkNationalBecomesE164WhenDefaultIsUs() {
        XCTAssertEqual(E164.format("03157909702", defaultRegion: "US"), "+923157909702")
        XCTAssertEqual(E164.format("0315 790 9702", defaultRegion: "US"), "+923157909702")
        XCTAssertEqual(E164.format("03157909702", defaultRegion: "GB"), "+923157909702")
    }

    func testPakistanInternationalFormsStayE164() {
        XCTAssertEqual(E164.format("+923157909702", defaultRegion: "US"), "+923157909702")
        XCTAssertEqual(E164.format("00923157909702", defaultRegion: "US"), "+923157909702")
    }

    func testUkTrunkNationalWorksWhenDefaultIsUs() {
        XCTAssertEqual(E164.format("02079460000", defaultRegion: "US"), "+442079460000")
    }

    func testRateSheetCallingCodesMapToRegions() {
        let regions = E164.regionsFromCallingCodes(["92", "44", "1"])
        XCTAssertTrue(regions.contains("PK"))
        XCTAssertTrue(regions.contains("GB"))
        XCTAssertTrue(regions.contains("US"))
        XCTAssertTrue(regions.firstIndex(of: "PK")! < regions.firstIndex(of: "GB")!)
        XCTAssertTrue(regions.firstIndex(of: "GB")! < regions.firstIndex(of: "US")!)
        XCTAssertEqual(E164.format("03157909702", defaultRegion: "US", candidateRegions: regions), "+923157909702")
    }

    func testIndiaTrunkNationalBecomesE164WhenDefaultIsUs() {
        XCTAssertEqual(E164.format("09876543210", defaultRegion: "US"), "+919876543210")
        XCTAssertEqual(E164.format("09876 543210", defaultRegion: "GB"), "+919876543210")
    }

    func testAustraliaMobileNationalUsesDefaultRegion() {
        XCTAssertEqual(E164.format("0412345678", defaultRegion: "AU"), "+61412345678")
    }

    func testFinlandUsesDefaultRegionWhenValid() {
        XCTAssertEqual(E164.format("0412345678", defaultRegion: "FI"), "+358412345678")
    }

    func testUkMobileNationalWorksWhenDefaultIsUs() {
        XCTAssertEqual(E164.format("07400123456", defaultRegion: "US"), "+447400123456")
    }

    func testCallingCodeIsLongestSupportedPrefix() {
        XCTAssertEqual(E164.callingCodeForDigits("1240"), "1")
        XCTAssertEqual(E164.callingCodeForDigits("447911"), "44")
        XCTAssertEqual(E164.callingCodeForDigits("92315"), "92")
        XCTAssertEqual(E164.callingCodeForDigits("91"), "91")
        XCTAssertEqual(E164.callingCodeForDigits("2126"), "212")
        XCTAssertEqual(E164.callingCodeForDigits("88299"), "882")
    }
}
