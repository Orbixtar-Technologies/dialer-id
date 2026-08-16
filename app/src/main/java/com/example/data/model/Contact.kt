package com.example.data.model

/**
 * One phone row belonging to a device contact.
 *
 * [displayNumber] is what the address book stores for the user to read, while
 * [dialNumber] has already been passed through the project sanitizer so it is
 * safe to hand to the SIP call path.
 */
data class ContactNumber(
    val displayNumber: String,
    val dialNumber: String,
    val label: String
)

/**
 * A contact read from the device address book via `ContactsContract`.
 * This app never persists or fabricates contacts; every instance comes from the
 * platform content provider.
 */
data class Contact(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoThumbnailUri: String?,
    val photoUri: String?,
    val isStarred: Boolean,
    val numbers: List<ContactNumber>
) {
    val primaryNumber: ContactNumber? get() = numbers.firstOrNull()

    val hasMultipleNumbers: Boolean get() = numbers.size > 1

    /** Uppercase first letter used for the alphabetical section headers. */
    val sectionKey: String
        get() {
            val first = displayName.trim().firstOrNull()
            return if (first != null && first.isLetter()) first.uppercase() else OTHER_SECTION
        }

    companion object {
        const val OTHER_SECTION = "#"

        /** Up to two letters used when a contact has no photo. */
        fun initialsOf(name: String): String {
            val words = name.trim().split(' ', '\t').filter { it.isNotBlank() }
            val letters = words.mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
            return when {
                letters.isEmpty() -> OTHER_SECTION
                letters.size == 1 -> letters.first().uppercase()
                else -> "${letters.first()}${letters.last()}".uppercase()
            }
        }
    }
}
