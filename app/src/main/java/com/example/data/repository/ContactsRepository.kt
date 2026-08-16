package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.Contact
import com.example.data.model.ContactNumber
import com.example.util.PhoneNumberSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

/**
 * Reads the device address book through `ContactsContract`.
 *
 * The platform content provider is the only source of truth here: nothing is
 * cached, seeded, or synthesised. Every query runs on [Dispatchers.IO] and
 * returns an empty list when the caller does not hold `READ_CONTACTS`.
 */
class ContactsRepository private constructor(private val context: Context) {

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Loads contacts that have at least one phone number.
     *
     * @param query when non-blank, filtering is delegated to
     *   [Phone.CONTENT_FILTER_URI] so the provider matches display names and
     *   numbers without pulling the whole address book into memory.
     * @param starredOnly restricts the result to contacts flagged as favourites.
     * @param limit caps the number of contacts returned, [NO_LIMIT] for all.
     */
    suspend fun queryContacts(
        query: String = "",
        starredOnly: Boolean = false,
        limit: Int = NO_LIMIT
    ): List<Contact> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()

        val trimmedQuery = query.trim()
        val uri: Uri = if (trimmedQuery.isEmpty()) {
            Phone.CONTENT_URI
        } else {
            Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(trimmedQuery))
        }
        val selection = if (starredOnly) "${Phone.STARRED} = 1" else null

        try {
            context.contentResolver.query(uri, PROJECTION, selection, null, SORT_ORDER)
                ?.use { cursor -> readContacts(cursor, limit) }
                ?: emptyList()
        } catch (e: SecurityException) {
            Log.w(TAG, "Contacts read denied: ${e.message}")
            emptyList()
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Contacts query rejected: ${e.message}")
            emptyList()
        }
    }

    /**
     * Emits whenever the address book changes so callers can re-query.
     * Registration itself needs no permission; the follow-up query is still gated.
     */
    fun contactChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val resolver = context.contentResolver
        try {
            resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
        } catch (e: SecurityException) {
            Log.w(TAG, "Contacts observer denied: ${e.message}")
        }
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate()

    private fun readContacts(cursor: Cursor, limit: Int): List<Contact> {
        val idIndex = cursor.getColumnIndex(Phone.CONTACT_ID)
        val lookupIndex = cursor.getColumnIndex(Phone.LOOKUP_KEY)
        val nameIndex = cursor.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY)
        val numberIndex = cursor.getColumnIndex(Phone.NUMBER)
        val normalizedIndex = cursor.getColumnIndex(Phone.NORMALIZED_NUMBER)
        val typeIndex = cursor.getColumnIndex(Phone.TYPE)
        val labelIndex = cursor.getColumnIndex(Phone.LABEL)
        val thumbnailIndex = cursor.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
        val photoIndex = cursor.getColumnIndex(Phone.PHOTO_URI)
        val starredIndex = cursor.getColumnIndex(Phone.STARRED)

        if (idIndex < 0 || numberIndex < 0) return emptyList()

        // Provider rows arrive one per phone number, already name-sorted, so a
        // LinkedHashMap both groups the numbers and preserves that order.
        val grouped = LinkedHashMap<Long, MutableContact>()

        while (cursor.moveToNext()) {
            val rawNumber = cursor.getString(numberIndex).orEmpty()
            val dialNumber = PhoneNumberSanitizer.filterDialInput(rawNumber)
            if (dialNumber.isEmpty()) continue

            val contactId = cursor.getLong(idIndex)
            val entry = grouped.getOrPut(contactId) {
                val name = nameIndex.takeIf { it >= 0 }
                    ?.let { cursor.getString(it) }
                    ?.trim()
                    .orEmpty()
                MutableContact(
                    id = contactId,
                    lookupKey = lookupIndex.takeIf { it >= 0 }?.let { cursor.getString(it) }.orEmpty(),
                    displayName = name.ifEmpty { rawNumber.trim() },
                    photoThumbnailUri = thumbnailIndex.takeIf { it >= 0 }?.let { cursor.getString(it) },
                    photoUri = photoIndex.takeIf { it >= 0 }?.let { cursor.getString(it) },
                    isStarred = starredIndex >= 0 && cursor.getInt(starredIndex) == 1
                )
            }

            val normalized = normalizedIndex.takeIf { it >= 0 }
                ?.let { cursor.getString(it) }
                ?.takeIf { it.isNotBlank() }
            val dedupeKey = normalized ?: dialNumber
            if (!entry.seenNumbers.add(dedupeKey)) continue

            entry.numbers.add(
                ContactNumber(
                    displayNumber = rawNumber.trim(),
                    dialNumber = dialNumber,
                    label = numberLabel(cursor, typeIndex, labelIndex)
                )
            )

            if (limit != NO_LIMIT && grouped.size > limit) {
                grouped.remove(contactId)
                break
            }
        }

        return grouped.values.map { it.toContact() }
    }

    private fun numberLabel(cursor: Cursor, typeIndex: Int, labelIndex: Int): String {
        if (typeIndex < 0) return ""
        val type = cursor.getInt(typeIndex)
        val custom = labelIndex.takeIf { it >= 0 }?.let { cursor.getString(it) }
        return Phone.getTypeLabel(context.resources, type, custom).toString()
    }

    private class MutableContact(
        val id: Long,
        val lookupKey: String,
        val displayName: String,
        val photoThumbnailUri: String?,
        val photoUri: String?,
        val isStarred: Boolean
    ) {
        val numbers = mutableListOf<ContactNumber>()
        val seenNumbers = mutableSetOf<String>()

        fun toContact() = Contact(
            id = id,
            lookupKey = lookupKey,
            displayName = displayName,
            photoThumbnailUri = photoThumbnailUri,
            photoUri = photoUri,
            isStarred = isStarred,
            numbers = numbers.toList()
        )
    }

    companion object {
        const val NO_LIMIT = -1

        private const val TAG = "ContactsRepository"

        private val PROJECTION = arrayOf(
            Phone.CONTACT_ID,
            Phone.LOOKUP_KEY,
            Phone.DISPLAY_NAME_PRIMARY,
            Phone.NUMBER,
            Phone.NORMALIZED_NUMBER,
            Phone.TYPE,
            Phone.LABEL,
            Phone.PHOTO_THUMBNAIL_URI,
            Phone.PHOTO_URI,
            Phone.STARRED
        )

        private const val SORT_ORDER = "${Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"

        @Volatile
        private var INSTANCE: ContactsRepository? = null

        fun getInstance(context: Context): ContactsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ContactsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
