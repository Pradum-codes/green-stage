# Green Stage — API and Repository Contracts

## 1. Purpose

Repositories are the boundary between ViewModels and Firebase or mock data.
One-shot operations return `Result<T>` and continuous observations use
`Flow<T>`. UI and ViewModels never access Firebase SDKs directly.

Interfaces live under `data/repository/`; implementations live under
`data/source/mock/` and `data/source/firebase/`.

Firebase implementations derive the authenticated UID from
`FirebaseAuth.currentUser`. A user ID supplied by the UI or ViewModel is never
authorization evidence.

## 2. Models

```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String,
    val accountType: AccountType,
    val interests: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)

enum class AccountType {
    STUDENT,
    ADMIN
}

data class Community(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val venue: String,
    val startAt: Long,
    val endAt: Long,
    val imageUrl: String?,
    val communityId: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPublished: Boolean
)

data class Registration(val eventId: String, val registeredAt: Long)
data class Bookmark(val eventId: String, val savedAt: Long)
```

Firestore `Timestamp` values convert to and from epoch milliseconds in Kotlin
models. The event display name is resolved from `communityId`; `organizerName`
is not duplicated in the event document.

## 3. AuthRepository

`AuthRepository` owns authentication, account registration, startup session
state, login, and logout:

```kotlin
interface AuthRepository {
    suspend fun registerStudent(
        name: String,
        email: String,
        password: String
    ): Result<User>

    suspend fun registerCommunityAdmin(
        name: String,
        email: String,
        password: String,
        communityName: String,
        communityDescription: String,
        communityCategory: String
    ): Result<CommunityRegistrationResult>

    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    fun observeAuthState(): Flow<AuthState>
}

data class CommunityRegistrationResult(
    val user: User,
    val community: Community
)
```

`registerCommunityAdmin` creates Auth credentials, a profile with
`accountType = ADMIN`, and the creator's community with `createdBy = uid`.
The profile and community writes use one Firestore batch or transaction after
Auth succeeds. The operation must be retry-safe.

## 4. UserRepository

```kotlin
interface UserRepository {
    suspend fun getCurrentUser(): Result<User>
    suspend fun updateProfile(
        name: String,
        interests: List<String>
    ): Result<Unit>
}
```

The Firebase implementation derives the current UID from Firebase Auth.
Profile updates cannot change `accountType` or email.

## 5. CommunityRepository

`CommunityRepository` manages the admin-owned community document. It has no
community-participation operations.

```kotlin
interface CommunityRepository {
    suspend fun getOwnedCommunity(): Result<Community>
    suspend fun updateCommunity(
        communityId: String,
        name: String,
        description: String,
        category: String
    ): Result<Unit>
}
```

Admin registration is the single Firestore operation that creates the
community; `CommunityRepository` does not expose a second creation workflow.
Community updates require the authenticated UID to equal
`communities.createdBy`. Community deletion is deferred.

## 6. EventRepository

```kotlin
interface EventRepository {
    suspend fun createEvent(event: Event): Result<String>
    suspend fun getPublishedEvents(): Result<List<Event>>
    suspend fun getEvent(eventId: String): Result<Event>
    suspend fun getOwnedCommunityEvents(): Result<List<Event>>
    suspend fun updateEvent(event: Event): Result<Unit>
    suspend fun deleteEvent(eventId: String): Result<Unit>
    suspend fun setPublished(
        eventId: String,
        isPublished: Boolean
    ): Result<Unit>
}
```

The Firebase implementation derives the UID from Auth and verifies that the
admin owns `event.communityId` through `communities.createdBy`. It does not
accept an acting-user ID argument. Student discovery queries
`isPublished == true`; admin My Events queries the admin's community ID.

Every event write validates required fields, `endAt > startAt`, immutable
`communityId`, immutable `createdBy`, and ownership of the associated
community. Students cannot call administrative event operations successfully.

## 7. RegistrationRepository

```kotlin
interface RegistrationRepository {
    suspend fun registerForEvent(eventId: String): Result<Unit>
    suspend fun cancelRegistration(eventId: String): Result<Unit>
    suspend fun isRegistered(eventId: String): Result<Boolean>
    suspend fun getCurrentUserRegistrations(): Result<List<Registration>>
    suspend fun getRegistrationCount(eventId: String): Result<Long>
}
```

The Firebase implementation derives the UID from Auth and stores
`users/{uid}/registrations/{eventId}` with both `eventId` and `registeredAt`
fields. A transaction verifies that the event exists, is published, the
document ID matches `eventId`, and no registration already exists.

Admin registration counts use an event-specific collection-group query:

```kotlin
firestore.collectionGroup("registrations")
    .whereEqualTo("eventId", eventId)
    .count()
```

The query is allowed only after the event's `communityId` is verified against
the authenticated admin's owned community. Validate this query and its
Security Rules in the Firebase Emulator Suite. It must not return unrelated
student registration data.

## 8. BookmarkRepository

```kotlin
interface BookmarkRepository {
    suspend fun addBookmark(eventId: String): Result<Unit>
    suspend fun removeBookmark(eventId: String): Result<Unit>
    suspend fun isBookmarked(eventId: String): Result<Boolean>
    suspend fun getCurrentUserBookmarks(): Result<List<Bookmark>>
}
```

Bookmarks use `users/{uid}/bookmarks/{eventId}` and are owned by the
authenticated user. Adding an existing bookmark is idempotent.

## 9. Errors and Implementations

Repositories return application-level failures for invalid input, missing
documents, network errors, duplicate registrations, and permission denials.
ViewModels decide how to display them.

Mock repositories implement the same contracts without Firebase imports and
model both account types, one admin-owned community, published discovery,
registrations, bookmarks, and admin event management. Firebase repositories
map the same models to Firestore and keep SDK exceptions inside the data layer.

Manual object creation selects mock or Firebase implementations. No Hilt,
Dagger, Koin, AppContainer, UseCase layer, custom backend, or separate domain
module is introduced.
