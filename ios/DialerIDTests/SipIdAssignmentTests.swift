import XCTest
@testable import DialerID

final class SipIdAssignmentTests: XCTestCase {
    private let pool = [
        SipLineIdentity(sipId: "13251", username: "13251", deviceId: "10404"),
        SipLineIdentity(sipId: "13252", username: "13252", deviceId: "10405"),
        SipLineIdentity(sipId: "13253", username: "13253", deviceId: "10406"),
        SipLineIdentity(sipId: "13254", username: "13254", deviceId: "10407")
    ]

    func testCapacityIsFour() {
        XCTAssertEqual(SipIdAssignment.maxAssignedIds, 4)
        XCTAssertEqual(pool.count, 4)
        XCTAssertEqual(
            SipIdAssignment.identitiesFromDevices([
                SipUpDevice(id: 1, username: "a"),
                SipUpDevice(id: 2, username: "b"),
                SipUpDevice(id: 3, username: "c"),
                SipUpDevice(id: 4, username: "d"),
                SipUpDevice(id: 5, username: "e")
            ]).count,
            4
        )
    }

    func testRejectsWhenPoolIsFull() {
        let taken = assigned(("u1", "13251"), ("u2", "13252"), ("u3", "13253"), ("u4", "13254"))
        let result = SipIdAssignment.decide(pool: pool, assignments: taken, uid: "u5", nowMs: 1_700_000_000_000)
        XCTAssertEqual(result, .poolFull)
        XCTAssertEqual(SipIdAssignment.countAssigned(taken), 4)
    }

    func testAssignsFirstUnusedId() {
        let taken = assigned(("u1", "13251"))
        guard case .assigned(let record) = SipIdAssignment.decide(pool: pool, assignments: taken, uid: "u2", nowMs: 99) else {
            return XCTFail("expected assigned")
        }
        XCTAssertEqual(record.sipId, "13252")
        XCTAssertEqual(record.firebaseUid, "u2")
        XCTAssertEqual(record.deviceId, "10405")
        XCTAssertEqual(record.assignedAt, 99)
    }

    func testDoesNotDoubleAssignSameUid() {
        let taken = assigned(("u1", "13251"))
        guard case .alreadyAssigned(let first) = SipIdAssignment.decide(pool: pool, assignments: taken, uid: "u1", nowMs: 10) else {
            return XCTFail("expected already assigned")
        }
        XCTAssertEqual(first.sipId, "13251")
    }

    func testPreferFirstTakesTheOriginalPoolId() {
        guard case .assigned(let record) = SipIdAssignment.decidePreferFirst(
            pool: pool,
            assignments: [:],
            uid: "admin",
            nowMs: 5
        ) else {
            return XCTFail("expected assigned")
        }
        XCTAssertEqual(record.sipId, "13251")
        XCTAssertEqual(record.deviceId, "10404")
    }

    func testClaimSpecificDoesNotStealAnotherUsersId() {
        let taken = assigned(("u1", "13251"))
        let result = SipIdAssignment.claimSpecific(
            identity: SipLineIdentity(sipId: "13251", username: "13251", deviceId: "10404"),
            assignments: taken,
            uid: "u2",
            nowMs: 30
        )
        XCTAssertEqual(result, .noUnusedId)
    }

    func testParseAssignmentsReturnsEmptyWhenQueryFailed() {
        let raw: [String: Any] = [
            "13251": ["firebaseUid": "u1", "assignedAt": 1, "username": "13251", "deviceId": "10404"]
        ]
        let denied = NSError(domain: "FIRDatabase", code: 1, userInfo: [
            NSLocalizedDescriptionKey: "permission_denied"
        ])
        XCTAssertTrue(SipIdAssignment.parseAssignments(raw, error: denied).isEmpty)
        XCTAssertEqual(SipIdAssignment.parseAssignments(raw, error: nil)["13251"]?.firebaseUid, "u1")
    }

    func testRecordFromUserFieldsUsesProfileWhenLedgerIsUnavailable() {
        let fromAssigned = SipIdAssignment.recordFromUserFields(
            assignedSipId: "13251",
            username: "13251",
            deviceId: "10404",
            uid: "user-1"
        )
        XCTAssertEqual(fromAssigned?.sipId, "13251")
        XCTAssertEqual(fromAssigned?.firebaseUid, "user-1")
        XCTAssertEqual(fromAssigned?.deviceId, "10404")

        let fromSipOnly = SipIdAssignment.recordFromUserFields(
            assignedSipId: "",
            username: "13252",
            deviceId: "10405",
            uid: "user-2"
        )
        XCTAssertEqual(fromSipOnly?.sipId, "13252")

        XCTAssertNil(
            SipIdAssignment.recordFromUserFields(
                assignedSipId: "",
                username: "",
                deviceId: "",
                uid: "user-3"
            )
        )
    }

    func testApplyToSipConfigKeepsExistingPasswordAndHost() {
        let existing = SipConfig(host: "sip.example.test", password: "local-only", port: 5060)
        let merged = SipIdAssignment.applyToSipConfig(
            existing,
            record: SipIdAssignmentRecord(sipId: "13251", firebaseUid: "u1", assignedAt: 1, username: "13251", deviceId: "10404")
        )
        XCTAssertEqual(merged.username, "13251")
        XCTAssertEqual(merged.deviceId, "10404")
        XCTAssertEqual(merged.password, "local-only")
        XCTAssertEqual(merged.host, "sip.example.test")
    }

    private func assigned(_ pairs: (String, String)...) -> [String: SipIdAssignmentRecord] {
        Dictionary(uniqueKeysWithValues: pairs.map { uid, sipId in
            (sipId, SipIdAssignmentRecord(sipId: sipId, firebaseUid: uid, assignedAt: 1, username: sipId, deviceId: ""))
        })
    }
}
