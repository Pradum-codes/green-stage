package com.greenstage.data.source.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.greenstage.data.model.AccountType
import com.greenstage.data.model.Community
import com.greenstage.data.model.Event
import com.greenstage.data.model.User
import java.util.Date

object FirestoreMapper {

    fun Long.toTimestamp(): Timestamp {
        return Timestamp(Date(this))
    }

    fun DocumentSnapshot.toEpochMillis(field: String): Long {
        val timestamp = getTimestamp(field)
        if (timestamp != null) {
            return timestamp.toDate().time
        }
        val longVal = getLong(field)
        if (longVal != null) {
            return longVal
        }
        error("Required timestamp field '$field' is missing or malformed.")
    }

    fun DocumentSnapshot.toUser(uid: String): User? {
        if (!exists()) return null
        val name = getString("name") ?: return null
        val email = getString("email") ?: return null
        val accountTypeStr = getString("accountType") ?: return null
        val accountType = when (accountTypeStr.lowercase()) {
            "student" -> AccountType.STUDENT
            "admin" -> AccountType.ADMIN
            else -> return null
        }
        @Suppress("UNCHECKED_CAST")
        val interests = (get("interests") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val createdAt = toEpochMillis("createdAt")
        val updatedAt = toEpochMillis("updatedAt")

        return User(
            id = uid,
            name = name,
            email = email,
            accountType = accountType,
            interests = interests,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun DocumentSnapshot.toCommunity(communityId: String): Community? {
        if (!exists()) return null
        val name = getString("name") ?: return null
        val description = getString("description") ?: return null
        val category = getString("category") ?: return null
        val createdBy = getString("createdBy") ?: return null
        val createdAt = toEpochMillis("createdAt")
        val updatedAt = toEpochMillis("updatedAt")

        return Community(
            id = communityId,
            name = name,
            description = description,
            category = category,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun DocumentSnapshot.toEvent(id: String): Event? {
        if (!exists()) return null
        val title = getString("title") ?: return null
        val description = getString("description") ?: return null
        val category = getString("category") ?: return null
        val venue = getString("venue") ?: return null
        val startAt = toEpochMillis("startAt")
        val endAt = toEpochMillis("endAt")
        val imageUrl = getString("imageUrl")
        val communityId = getString("communityId") ?: return null
        val createdBy = getString("createdBy") ?: return null
        val createdAt = toEpochMillis("createdAt")
        val updatedAt = toEpochMillis("updatedAt")
        val isPublished = getBoolean("isPublished") ?: false

        return Event(
            id = id,
            title = title,
            description = description,
            category = category,
            venue = venue,
            startAt = startAt,
            endAt = endAt,
            imageUrl = imageUrl,
            communityId = communityId,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPublished = isPublished
        )
    }
}
