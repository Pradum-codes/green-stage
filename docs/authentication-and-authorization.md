# Green Stage — Authentication and Authorization

## 1. Authentication Model

Firebase Authentication manages email/password credentials and sessions.
Firestore stores user profiles, communities, and events. Each account has
exactly one account type in `users/{userId}`:

```text
accountType: "student" | "admin"
```

The only account types are `student` and `admin`; multiple-community
administration is outside the initial scope.

## 2. Registration

### Student registration

1. Validate name, email, password, and confirmation.
2. Create the Firebase Authentication account.
3. Create `users/{uid}` with `accountType = "student"`.
4. Open Student Experience.

### Community Admin registration

1. Validate profile and community name, description, and category.
2. Create the Firebase Authentication account.
3. Create `users/{uid}` with `accountType = "admin"`.
4. Atomically create `communities/{communityId}` with `createdBy = uid`.
5. Open Admin Experience for the created community.

The creator automatically owns the community. Auth creation is separate from
Firestore atomicity. If Auth succeeds
but Firestore writes fail, show a recovery state, retry safely, and do not
open Admin Experience until the profile and community are valid.

## 3. Login and Startup Routing

| Scenario | Destination |
|---|---|
| Student registration succeeds | Student Experience |
| Admin registration and community creation succeed | Admin Experience |
| Existing student logs in | Student Experience |
| Existing admin logs in | Admin Experience |
| User logs out | Authentication screens |

DataStore may store convenience preferences, but local state is never proof of
authorization. On every startup, load `users/{uid}` from Firestore and verify
that `accountType` is exactly `student` or `admin`.

If the profile is missing, invalid, or unsupported, show a safe recovery/error
state. Do not route into either protected experience based only on local data.

Authentication state distinguishes loading, signed out, authenticated, profile
error, and registration recovery error. A temporary network error is not
treated as sign-out.

## 4. Authorization Requirements

### Profiles

- Users may read and update permitted fields only in their own profile.
- `accountType` is restricted to `student` or `admin`.
- Users cannot change their own `accountType` after creation.
- Users cannot modify another user's profile.
- Profile operations use the authenticated UID, not a caller-supplied UID.

### Communities

- Only an authenticated admin can create a community.
- `communities.createdBy` must equal the authenticated UID.
- Only the authenticated owner can update or delete that community.
- A student cannot create or modify a community.
- An admin cannot modify another admin's community.
- No additional community-participation data is created.
- Community deletion is disabled in the initial rules; events remain
  associated with their community.

### Events

- Students can read published events.
- Admins can create events only for their owned community.
- Admins can update, publish, unpublish, or delete events only when
  `event.communityId` identifies their owned community.
- Students cannot create, update, publish, unpublish, or delete events.
- `createdBy` is audit information; event ownership is verified through the
  associated community's `createdBy`.
- `communityId` and `createdBy` remain immutable during event updates.

### Registrations and bookmarks

- Users can read, create, and delete only their own records.
- Registration operations are student-only. Registration creation validates
  that the event exists, is published, and that the `eventId` field matches
  the document ID.
- Admins may read registration records only through an event-specific
  collection-group query for events owned by their community.
- Deterministic event IDs prevent duplicate subcollection records.

Firestore Rules enforce these requirements. UI visibility and locally stored
account type are not security controls.

## 5. Firestore Security Rules Design

The following is a design example, not a complete production ruleset. It
shows the account, ownership, immutable-field, registration, and bookmark
checks. Complete field validation and all application paths must be tested
with the Firebase Emulator Suite before release.

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() {
      return request.auth != null;
    }

    function self(userId) {
      return signedIn() && request.auth.uid == userId;
    }

    function currentAccountType(type) {
      return signedIn()
        && get(
          /databases/$(database)/documents/users/$(request.auth.uid)
        ).data.accountType == type;
    }

    function accountTypeAfter(type) {
      return signedIn()
        && getAfter(
          /databases/$(database)/documents/users/$(request.auth.uid)
        ).data.accountType == type;
    }

    function ownsCommunity(communityId) {
      return currentAccountType("admin")
        && get(
          /databases/$(database)/documents/communities/$(communityId)
        ).data.createdBy == request.auth.uid;
    }

    function ownsEvent(eventId) {
      return exists(
          /databases/$(database)/documents/events/$(eventId)
        )
        && ownsCommunity(
          get(
            /databases/$(database)/documents/events/$(eventId)
          ).data.communityId
        );
    }

    match /users/{userId} {
      allow read: if self(userId);

      allow create: if self(userId)
        && request.resource.data.accountType in ["student", "admin"]
        && request.resource.data.keys().hasOnly([
          "name", "email", "accountType", "interests",
          "createdAt", "updatedAt"
        ]);

      allow update: if self(userId)
        && request.resource.data.accountType == resource.data.accountType
        && request.resource.data.email == resource.data.email
        && request.resource.data.keys().hasOnly([
          "name", "email", "accountType", "interests",
          "createdAt", "updatedAt"
        ]);

      match /registrations/{eventId} {
        allow read: if (
            currentAccountType("student") && self(userId)
          )
          || (
            currentAccountType("admin")
            && ownsEvent(resource.data.eventId)
          );
        allow delete: if currentAccountType("student") && self(userId);
        allow create: if currentAccountType("student") && self(userId)
          && request.resource.data.keys().hasOnly([
            "eventId", "registeredAt"
          ])
          && request.resource.data.eventId == eventId
          && request.resource.data.registeredAt is timestamp
          && exists(/databases/$(database)/documents/events/$(eventId))
          && get(/databases/$(database)/documents/events/$(eventId))
              .data.isPublished == true;
      }

      match /bookmarks/{eventId} {
        allow read, delete: if currentAccountType("student") && self(userId);
        allow create: if currentAccountType("student") && self(userId)
          && request.resource.data.keys().hasOnly(["savedAt"])
          && request.resource.data.savedAt is timestamp;
      }
    }

    match /communities/{communityId} {
      allow read: if signedIn();
      allow create: if accountTypeAfter("admin")
        && !exists(
          /databases/$(database)/documents/users/$(request.auth.uid)
        )
        && request.resource.data.keys().hasOnly([
          "name", "description", "category", "createdBy",
          "createdAt", "updatedAt"
        ])
        && request.resource.data.createdBy == request.auth.uid;
      allow update: if ownsCommunity(communityId)
        && request.resource.data.createdBy == resource.data.createdBy;
      allow delete: if false;
    }

    match /events/{eventId} {
      allow read: if (
          currentAccountType("student")
          && resource.data.isPublished == true
        )
        || ownsCommunity(resource.data.communityId);
      allow create: if ownsCommunity(request.resource.data.communityId)
        && request.resource.data.createdBy == request.auth.uid;
      allow update: if ownsCommunity(resource.data.communityId)
        && request.resource.data.communityId == resource.data.communityId
        && request.resource.data.createdBy == resource.data.createdBy;
      allow delete: if ownsCommunity(resource.data.communityId);
    }
  }
}
```

Student event queries must include `whereEqualTo("isPublished", true)`.
Admin event queries must constrain `communityId` to the authenticated admin's
owned community. Registration-count queries must use
`collectionGroup("registrations").whereEqualTo("eventId", eventId).count()`
only after ownership of the event is verified. Firestore Rules are not query
filters; the Emulator Suite must confirm that this collection-group query is
compatible with the rule evaluation. If the query cannot be safely authorized
under the deployed Rules, the count query must not be used.

The community-registration operation must create the admin profile and
community in one Firestore batch or transaction after Auth succeeds. Rules
must be tested to ensure a student cannot create a community, an admin cannot
modify another owner's community, and an event cannot be moved across
communities. The example is not a substitute for Emulator testing.

## 6. Logout and Testing

Logout signs out Firebase Authentication, clears navigation and user-specific
ViewModel state, and does not delete Firestore data.

Emulator tests must cover student and admin accounts, invalid account types,
account-type changes, cross-owner community writes, cross-community event
writes, unpublished-event visibility, registrations, bookmarks, missing
profiles, and invalid startup routing.
