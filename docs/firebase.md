# Green Stage — Firebase Integration

## Services

- Firebase Authentication manages email/password accounts and sessions.
- Cloud Firestore stores users, communities, events, registrations, and
  bookmarks.
- Firebase Storage may store optional event images.
- Firebase Cloud Messaging may provide notifications.

No custom backend is required. Repository implementations keep Firebase SDK
calls inside `data/source/firebase/`.

## Required Firestore paths

```text
users/{userId}
users/{userId}/registrations/{eventId}
users/{userId}/bookmarks/{eventId}
communities/{communityId}
events/{eventId}
```

`users/{userId}` contains `accountType: "student" | "admin"`.
Registration documents contain `eventId` and `registeredAt`; `eventId` must
match the deterministic document ID.

## Registration and community creation

Student registration creates the Auth account and user profile with
`accountType = "student"`.

Community Admin registration creates the Auth account, user profile with
`accountType = "admin"`, and owned community with `createdBy = uid`. The
profile and community writes occur in one Firestore batch or transaction after
Auth succeeds. The operation must be retry-safe.

## Queries and indexes

Student discovery:

```kotlin
firestore.collection("events")
    .whereEqualTo("isPublished", true)
    .orderBy("startAt")
```

Admin dashboard and My Events:

```kotlin
firestore.collection("events")
    .whereEqualTo("communityId", adminCommunityId)
    .orderBy("startAt")
```

Admin registration counts:

```kotlin
firestore.collectionGroup("registrations")
    .whereEqualTo("eventId", eventId)
    .count()
```

Before issuing this query, verify that the event belongs to the authenticated
admin's owned community. The query must not expose unrelated registration
records. Students alone may read, create, and delete their own registrations
and bookmarks; admins cannot use those student operations directly. Students
must query published events, while admins may query only their owned
community's events, including unpublished events. Unauthenticated event reads
are denied.

Likely composite indexes:

- `events(isPublished, startAt)`
- `events(communityId, startAt)`
- collection-group `registrations(eventId)`

Community deletion is deferred and is denied by the initial Security Rules.

Create indexes only for implemented queries. Queries must constrain data to
what Security Rules permit because Rules are not query filters.

## Configuration and testing

Add the project-specific `google-services.json` to the Android application
module during local setup. Do not commit credentials or secrets.

The documented rules and queries require Firebase Emulator Suite testing.
Test student and admin accounts, invalid account types, cross-owner community
writes, cross-community event writes, unpublished-event visibility,
registrations, bookmarks, missing profiles, and failed registration recovery.
