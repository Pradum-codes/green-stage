package com.greenstage.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.greenstage.data.model.AccountType
import com.greenstage.data.model.Community
import com.greenstage.data.model.User
import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.repository.AuthState
import com.greenstage.data.repository.CommunityRegistrationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun registerStudent(
        name: String,
        email: String,
        password: String
    ): Result<User> = runCatching {
        validateCredentials(name, email, password)
        val normalizedEmail = normalizeEmail(email)

        val authResult = firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password).await()
        val firebaseUser = authResult.user ?: error("Failed to create Auth account.")
        val uid = firebaseUser.uid

        val now = System.currentTimeMillis()
        val userData = mapOf(
            "name" to name.trim(),
            "email" to normalizedEmail,
            "accountType" to "student",
            "interests" to emptyList<String>(),
            "createdAt" to now,
            "updatedAt" to now
        )

        try {
            firestore.collection("users").document(uid).set(userData).await()
        } catch (e: Exception) {
            // Attempt cleanup of newly created Auth user if profile creation failed
            runCatching { firebaseUser.delete().await() }
            throw e
        }

        User(
            id = uid,
            name = name.trim(),
            email = normalizedEmail,
            accountType = AccountType.STUDENT,
            interests = emptyList(),
            createdAt = now,
            updatedAt = now
        )
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun registerCommunityAdmin(
        name: String,
        email: String,
        password: String,
        communityName: String,
        communityDescription: String,
        communityCategory: String
    ): Result<CommunityRegistrationResult> = runCatching {
        validateCredentials(name, email, password)
        require(communityName.isNotBlank()) { "Community name cannot be empty." }
        require(communityDescription.isNotBlank()) { "Community description cannot be empty." }
        require(communityCategory.isNotBlank()) { "Community category cannot be empty." }

        val normalizedEmail = normalizeEmail(email)

        val authResult = firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password).await()
        val firebaseUser = authResult.user ?: error("Failed to create Auth account.")
        val uid = firebaseUser.uid

        val now = System.currentTimeMillis()

        val userMap = mapOf(
            "name" to name.trim(),
            "email" to normalizedEmail,
            "accountType" to "admin",
            "interests" to emptyList<String>(),
            "createdAt" to now,
            "updatedAt" to now
        )

        val communityMap = mapOf(
            "name" to communityName.trim(),
            "description" to communityDescription.trim(),
            "category" to communityCategory.trim(),
            "createdBy" to uid,
            "createdAt" to now,
            "updatedAt" to now
        )

        val batch = firestore.batch()
        batch.set(firestore.collection("users").document(uid), userMap)
        batch.set(firestore.collection("communities").document(uid), communityMap)

        try {
            batch.commit().await()
        } catch (e: Exception) {
            // Attempt cleanup of newly created Auth user if Firestore batch write failed
            runCatching { firebaseUser.delete().await() }
            throw e
        }

        val user = User(
            id = uid,
            name = name.trim(),
            email = normalizedEmail,
            accountType = AccountType.ADMIN,
            interests = emptyList(),
            createdAt = now,
            updatedAt = now
        )

        val community = Community(
            id = uid,
            name = communityName.trim(),
            description = communityDescription.trim(),
            category = communityCategory.trim(),
            createdBy = uid,
            createdAt = now,
            updatedAt = now
        )

        CommunityRegistrationResult(user = user, community = community)
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<User> = runCatching {
        require(email.isNotBlank()) { "Email cannot be empty." }
        require(password.isNotBlank()) { "Password cannot be empty." }

        val normalizedEmail = normalizeEmail(email)
        val authResult = firebaseAuth.signInWithEmailAndPassword(normalizedEmail, password).await()
        val firebaseUser = authResult.user ?: error("Authentication failed.")
        val uid = firebaseUser.uid

        val userDoc = firestore.collection("users").document(uid).get().await()
        val user = userDoc.toUser(uid)
            ?: error("User profile not found or invalid.")

        check(user.id == uid) { "Profile UID mismatch." }

        user
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun sendPasswordResetEmail(
        email: String
    ): Result<Unit> = runCatching {
        require(email.isNotBlank() && email.contains("@")) { "Enter a valid email address." }
        val normalizedEmail = normalizeEmail(email)
        firebaseAuth.sendPasswordResetEmail(normalizedEmail).await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        firebaseAuth.signOut()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                trySend(AuthState.SignedOut)
            } else {
                val uid = firebaseUser.uid
                launch {
                    try {
                        val snapshot = firestore.collection("users").document(uid).get().await()
                        if (firebaseAuth.currentUser?.uid != uid) return@launch

                        val user = snapshot.toUser(uid)
                        if (user == null) {
                            trySend(AuthState.ProfileError("User profile not found or invalid."))
                        } else if (user.accountType == AccountType.ADMIN) {
                            val communityDoc = firestore.collection("communities").document(uid).get().await()
                            if (firebaseAuth.currentUser?.uid != uid) return@launch

                            val community = communityDoc.toCommunity(uid)
                            if (community == null || community.createdBy != uid) {
                                trySend(AuthState.RegistrationRecoveryError("Admin community profile is missing or invalid."))
                            } else {
                                trySend(AuthState.Authenticated(user))
                            }
                        } else {
                            trySend(AuthState.Authenticated(user))
                        }
                    } catch (e: Exception) {
                        if (firebaseAuth.currentUser?.uid == uid) {
                            trySend(AuthState.ProfileError(FirebaseErrorMapper.map(e)))
                        }
                    }
                }
            }
        }

        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    private fun validateCredentials(
        name: String,
        email: String,
        password: String
    ) {
        require(name.isNotBlank()) { "Name cannot be empty." }
        require(email.isNotBlank() && email.contains("@")) { "Enter a valid email address." }
        require(password.length >= 6) { "Password must contain at least 6 characters." }
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }

    private fun DocumentSnapshot.toUser(uid: String): User? {
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
        val createdAt = getLong("createdAt")
            ?: getTimestamp("createdAt")?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()
        val updatedAt = getLong("updatedAt")
            ?: getTimestamp("updatedAt")?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()

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

    private fun DocumentSnapshot.toCommunity(communityId: String): Community? {
        if (!exists()) return null
        val name = getString("name") ?: return null
        val description = getString("description") ?: return null
        val category = getString("category") ?: return null
        val createdBy = getString("createdBy") ?: return null
        val createdAt = getLong("createdAt")
            ?: getTimestamp("createdAt")?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()
        val updatedAt = getLong("updatedAt")
            ?: getTimestamp("updatedAt")?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()

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
}
