package com.example

import com.example.data.model.CallStatus
import com.example.data.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun userProfile_defaultValues_areCorrect() {
    val profile = UserProfile()
    assertEquals(0.00, profile.creditBalance, 0.001)
    assertEquals("guest_operator_001", profile.uid)
    assertEquals(false, profile.isCloudSynced)
  }

  @Test
  fun userProfile_cloudSyncState_updatesCorrectly() {
    val profile = UserProfile(
      uid = "firebase_uid_123",
      email = "operator@example.com",
      displayName = "Alex Rivera",
      creditBalance = 25.50,
      isCloudSynced = true
    )
    assertTrue(profile.isCloudSynced)
    assertEquals(25.50, profile.creditBalance, 0.001)
    assertEquals("firebase_uid_123", profile.uid)
  }

  @Test
  fun userProfile_realtimeDatabaseSerialization_worksBidirectionally() {
    val original = UserProfile(
      uid = "usr_9988",
      displayName = "Sarah Connor",
      creditBalance = 15.75,
      selectedCallerId = "+1 (555) 019-2834",
      createdAt = 1700000000000L
    )

    val map = UserProfile.toMap(original)
    assertEquals("usr_9988", map["uid"])
    assertEquals("Sarah Connor", map["displayName"])
    assertEquals(15.75, map["creditBalance"])
    assertEquals("+1 (555) 019-2834", map["selectedCallerId"])
    assertEquals(1700000000000L, map["createdAt"])

    val reconstructed = UserProfile.fromMap(map)
    assertEquals(original.uid, reconstructed.uid)
    assertEquals(original.displayName, reconstructed.displayName)
    assertEquals(original.creditBalance, reconstructed.creditBalance, 0.001)
    assertEquals(original.selectedCallerId, reconstructed.selectedCallerId)
    assertEquals(original.createdAt, reconstructed.createdAt)
  }
}
