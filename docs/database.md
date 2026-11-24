# Green Stage — Database Design

## 1. Overview

Green Stage uses Firebase Authentication for credentials and Cloud Firestore
for user profiles, communities, events, registrations, and bookmarks.
Additional community administration is outside the initial scope.

```text
users/{userId}
  registrations/{eventId}
  bookmarks/{eventId}

communities/{adminUid}

events/{eventId}
```

Document ID rules:

- User and Community documents use Firebase Authentication UIDs (a community document ID equals its owning admin's UID).
- Event documents use Firestore-generated IDs.
- Registration and bookmark documents use deterministic event IDs.
- An email address can belong to only one Firebase Authentication account in the Firebase project; an email registered as a Student cannot also be registered as a Community Admin.
- Account types (`student` or `admin`) are selected during registration and remain immutable. Role upgrades, downgrades, or transfers are not supported.
- Community Admin registration is public self-registration; there are no invitations, approval workflows, or separate admin provisioning flows.

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

**Path:** `communities/{adminUid}`

| Field | Type | Description |
|---|---|---|
| `name` | String | Community name |
| `description` | String | Community description |
| `category` | String | Community category |
| `createdBy` | String | Admin creator's Firebase Authentication UID |
| `createdAt` | Timestamp | Creation time |
| `updatedAt` | Timestamp | Last update time |

The community document ID is equal to the owning admin's Firebase Authentication UID (`community.id == adminUid`).
The `createdBy` field also equals `adminUid`. One admin owns exactly one community, and an admin cannot create a second community. Community ownership can be resolved directly using the authenticated UID (`communities/{authenticatedUid}`). Old assumptions of randomly generated Firestore IDs for communities are not used.

Community creation writes the user profile (`users/{uid}`) and community (`communities/{uid}`) together in one Firestore batch or transaction after Firebase Authentication succeeds. Because Auth creation and Firestore writes are separate operations, a failure scenario where Auth succeeds but Firestore writes fail must show a recovery state and retry missing writes safely without routing an incomplete admin account into Admin Experience.

## 4. Events

**Path:** `events/{eventId}`

Every event has exactly one owning `communityId`. The meaning of `communityId` is explicit: **`events.communityId` equals the owning admin's UID (`adminUid`)**, which is also the community document ID.

| Field | Type | Description |
|---|---|---|
| `title` | String | Event title |
| `description` | String | Detailed description |
| `category` | String | Event category |
| `venue` | String | Event location |
| `startAt` | Timestamp | Start time |
| `endAt` | Timestamp | End time |
| `imageUrl` | String or null | Optional image URL |
| `communityId` | String | Owning community ID (equals admin UID) |
| `createdBy` | String | Admin creator's Firebase Authentication UID |
| `createdAt` | Timestamp | Event creation time |
| `updatedAt` | Timestamp | Last update time |
| `isPublished` | Boolean | Whether the event is discoverable |

The application does not duplicate `organizerName` or other community information in event documents. It resolves the community name using `communityId` directly via `communities/{communityId}`, so community-name updates appear consistently. `createdBy` is kept as audit information; event authorization verifies that the authenticated admin owns the event's community by checking `events.communityId == request.auth.uid` and verifying `communities/{adminUid}`.

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
    .whereEqualTo("communityId", adminUid)
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
that admin (where `communityId == adminUid`). The query must be scoped to the selected event, and Security Rules
must authorize each returned registration by checking the event's owning
community. Use Firestore count aggregation where supported; do not add a
custom backend or Cloud Function solely for counting.

If a collection-group query cannot be proven compatible with the deployed
Security Rules, retrieve only authorized event-specific records or treat the
count query as blocked until it is validated in the Firebase Emulator Suite.

The student query requests only published events. The admin query requests
only events for the admin's owned community (`communityId == adminUid`). Potential composite indexes are:

- `events`: `isPublished` ascending, `startAt` ascending.
- `events`: `communityId` ascending, `startAt` ascending.

Create indexes only for implemented queries and verify them in the Firebase
Console or Emulator Suite. Firestore Security Rules are not query filters, so
queries must constrain the data they request.

## 7. Security and Integrity

- User profiles are readable and editable only by the authenticated owner.
- `accountType` is restricted to `student` or `admin` and cannot be changed
  after profile creation (immutable).
- Community creation requires an authenticated admin account; the community document ID equals the admin UID (`communities/{adminUid}`) and `createdBy` equals `adminUid`. The initial profile (`users/{uid}`) and community (`communities/{uid}`) write are one atomic batch or transaction.
- Community creation is limited to public self-registration during the Community Admin registration flow; an existing admin profile cannot create a second community. No invitations, manual provisioning, or approval workflows exist.
- Any authenticated user can read community information (allowing students to resolve community names when displaying events), but only the authenticated owner can update that community (`communities/{adminUid}`). Community ownership and document identity cannot be changed. Community deletion is deferred.
- Event writes require an admin whose UID equals `events.communityId`.
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
