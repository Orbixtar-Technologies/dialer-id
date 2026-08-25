import Contacts
import Foundation

struct DeviceContact: Identifiable, Equatable {
    var id: String
    var name: String
    var numbers: [String]
    var isFavorite: Bool

    var primaryNumber: String { numbers.first ?? "" }
}

@MainActor
final class ContactsStore: ObservableObject {
    static let shared = ContactsStore()

    @Published private(set) var contacts: [DeviceContact] = []
    @Published private(set) var authorization: CNAuthorizationStatus = CNContactStore.authorizationStatus(for: .contacts)
    @Published var search = ""

    var favorites: [DeviceContact] {
        contacts.filter(\.isFavorite)
    }

    var filtered: [DeviceContact] {
        let needle = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if needle.isEmpty { return contacts }
        return contacts.filter {
            $0.name.lowercased().contains(needle) || $0.numbers.contains { $0.contains(needle) }
        }
    }

    func requestAccess() {
        let store = CNContactStore()
        store.requestAccess(for: .contacts) { [weak self] granted, _ in
            Task { @MainActor in
                self?.authorization = CNContactStore.authorizationStatus(for: .contacts)
                if granted { self?.reload() }
            }
        }
    }

    func reload() {
        authorization = CNContactStore.authorizationStatus(for: .contacts)
        guard authorization == .authorized else { return }
        let store = CNContactStore()
        let keys: [CNKeyDescriptor] = [
            CNContactIdentifierKey as CNKeyDescriptor,
            CNContactGivenNameKey as CNKeyDescriptor,
            CNContactFamilyNameKey as CNKeyDescriptor,
            CNContactPhoneNumbersKey as CNKeyDescriptor
        ]
        let request = CNContactFetchRequest(keysToFetch: keys)
        var loaded: [DeviceContact] = []
        try? store.enumerateContacts(with: request) { contact, _ in
            let numbers = contact.phoneNumbers.compactMap {
                PhoneNumberSanitizer.sanitizeDestination($0.value.stringValue)
            }
            if numbers.isEmpty { return }
            let name = [contact.givenName, contact.familyName]
                .filter { !$0.isEmpty }
                .joined(separator: " ")
            loaded.append(
                DeviceContact(
                    id: contact.identifier,
                    name: name.isEmpty ? numbers[0] : name,
                    numbers: numbers,
                    isFavorite: false
                )
            )
        }
        contacts = loaded.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }
}
