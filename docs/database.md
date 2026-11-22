# Green Stage — Database Design

## 1. Overview

Green Stage uses Firebase Authentication for credentials and Cloud Firestore
for user profiles, communities, events, registrations, and bookmarks.
Additional community administration is outside the initial scope.

```text
users/{userId}
  registrations/{eventId}
  bookmarks/{eventId}

communities/{communityId}

events/{eventId}
```

Document ID rules:

- User documents use Firebase Authentication UIDs.
- Community and event documents use Firestore-generated IDs.
- Registration and bookmark documents use deterministic event IDs.
- Not every Firestore document ID is a user UID.

## 2. Users

**Path:** `users/{userId}`

| Field | Type | Description |
|---|---|---|
| `name` | String | User display name |
| `email` | String | Account email |
| `accountType` | String | `student` or `admin` |
| `interests` | Array<String> | Interest categories |
| `createdAt` | Timestamp | Profile creation time |
| `updatedAt` | Timestamp | Last profile update time |

The document ID equals the Firebase Authentication UID. Each account has
exactly one `accountType`. It cannot be changed through profile editing.
The only account types are `student` and `admin`.

### Registrations

**Path:** `users/{userId}/registrations/{eventId}`

| Field | Type | Description |
|---|---|---|
| `eventId` | String | Referenced event ID; equals the document ID |
| `registeredAt` | Timestamp | Registration time |

The event ID is both a field and the deterministic document ID, preventing
duplicate registration documents. Registration uses a Firestore transaction
that verifies the event exists, is published, and is not already registered.

### Bookmarks

**Path:** `users/{userId}/bookmarks/{eventId}`

| Field | Type | Description |
|---|---|---|
| `savedAt` | Timestamp | Bookmark time |

Adding an existing bookmark is idempotent.

## 3. Communities

**Path:** `communities/{communityId}`

| Field | Type | Description |
|---|---|---|
| `name` | String | Community name |
| `description` | String | Community description |
| `category` | String | Community category |
| `createdBy` | String | Admin creator's Firebase Authentication UID |
| `createdAt` | Timestamp | Creation time |
| `updatedAt` | Timestamp | Last update time |

The admin account created during the community-registration flow owns this
community through `createdBy`. The initial scope supports one community per admin account. An admin owns only
the community created during registration.

Community creation writes the user profile and community in one Firestore
batch or transaction after Firebase Authentication succeeds. The operation
must be retry-safe so a failed retry does not create duplicate communities.

## 4. Events

**Path:** `events/{eventId}`

Every event has exactly one owning `communityId`.

| Field | Type | Description |
|---|---|---|
| `title` | String | Event title |
| `description` | String | Detailed description |
| `category` | String | Event category |
| `venue` | String | Event location |
| `startAt` | Timestamp | Start time |
| `endAt` | Timestamp | End time |
| `imageUrl` | String or null | Optional image URL |
| `communityId` | String | Owning community ID |
| `createdBy` | String | Admin creator's Firebase Authentication UID |
| `createdAt` | Timestamp | Event creation time |
| `updatedAt` | Timestamp | Last update time |
| `isPublished` | Boolean | Whether the event is discoverable |

The application does not duplicate `organizerName` in the event. It resolves
the community name using `communityId`, so community-name updates appear
consistently. `createdBy` is audit information; event authorization verifies
that the authenticated admin owns the event's community through
`communities.createdBy`.

Firestore `Timestamp` values are converted to `Long` epoch milliseconds in
Kotlin models and converted back before writes. `startAt` and `endAt` must be
valid timestamps with `endAt > startAt`.

## 5. Event Status

For the current time `currentTime`:

- **Upcoming:** `startAt > currentTime`
- **Active:** `startAt <= currentTime` and `endAt > currentTime`
- **Completed:** `endAt <= currentTime`

These definitions apply to dashboard statistics, admin event lists, and
student home or exploration screens.

## 6. Queries and Indexes

```kotlin
// Student discovery
firestore.collection("events")
    .whereEqualTo("isPublished", true)
    .orderBy("startAt")

// Admin dashboard and My Events
firestore.collection("events")
    .whereEqualTo("communityId", adminCommunityId)
    .orderBy("startAt")

// Current user's registrations
firestore.collection("users")
    .document(currentUserId)
    .collection("registrations")

// Current user's bookmarks
firestore.collection("users")
    .document(currentUserId)
    .collection("bookmarks")
```

### Registration counts

Registration documents include `eventId`, enabling a collection-group query:

```kotlin
firestore.collectionGroup("registrations")
    .whereEqualTo("eventId", eventId)
    .count()
```

The admin may use this query only for an event whose `communityId` belongs to
that admin. The query must be scoped to the selected event, and Security Rules
must authorize each returned registration by checking the event's owning
community. Use Firestore count aggregation where supported; do not add a
custom backend or Cloud Function solely for counting.

If a collection-group query cannot be proven compatible with the deployed
Security Rules, retrieve only authorized event-specific records or treat the
count query as blocked until it is validated in the Firebase Emulator Suite.

The student query requests only published events. The admin query requests
only events for the admin's owned community. Potential composite indexes are:

- `events`: `isPublished` ascending, `startAt` ascending.
- `events`: `communityId` ascending, `startAt` ascending.

Create indexes only for implemented queries and verify them in the Firebase
Console or Emulator Suite. Firestore Security Rules are not query filters, so
queries must constrain the data they request.

## 7. Security and Integrity

- User profiles are readable and editable only by the authenticated owner.
- `accountType` is restricted to `student` or `admin` and cannot be changed
  after profile creation.
- Community creation requires an authenticated admin account and matching
  `createdBy`; the initial profile and community write are one atomic batch.
- Community creation is limited to the initial admin-registration flow; an
  existing admin profile cannot create a second community in the initial scope.
- Only the authenticated owner can update that community. Community deletion
  is deferred.
- Event writes require an admin whose UID equals the community's `createdBy`.
- Event updates cannot change `communityId` or `createdBy`.
- Students can read published events but cannot write events or communities.
- Registration and bookmark paths must contain the authenticated user's UID.
- Registration validates event existence, publication status, matching `eventId`,
  and student account type.
- Admin registration-count reads are limited to events owned by that admin.
- Events with registrations should normally be unpublished rather than deleted.
- Deleting a parent document does not delete subcollections automatically.

The documented rules and queries require Firebase Emulator Suite testing
before release.
