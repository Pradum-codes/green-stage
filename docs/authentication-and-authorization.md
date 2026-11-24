# Green Stage — Authentication and Authorization

## 1. Authentication Model

Firebase Authentication manages email/password credentials and sessions.
Firestore stores user profiles, communities, and events. Each account has
exactly one account type in `users/{userId}`:

```text
accountType: "student" | "admin"
```

The only account types are `student` and `admin`.

Key rules:
- An email address can belong to only one Firebase Authentication account in the same Firebase project. Therefore, an email registered as a Student cannot also be registered as a Community Admin.
- Account type is selected during registration and remains immutable. There is no role upgrade, downgrade, or role transfer.
- Community Admin registration is public self-registration. Do not introduce invitations, manual admin provisioning, approval workflows, or a separate admin-creation process.

## 2. Registration

### Student registration

1. Validate registration input (name, email, password, and confirmation).
2. Create the Firebase Authentication account.
3. Obtain the authenticated UID.
4. Create `users/{uid}` with `accountType = "student"`.
5. Route to Student Experience.

No community document is created for student registration.

### Community Admin registration

1. Validate user and community details (name, email, password, community name, description, category).
2. Create the Firebase Authentication account.
3. Obtain the authenticated UID.
4. Create `users/{uid}` with `accountType = "admin"`.
5. Create `communities/{uid}` with `createdBy = uid` (community document ID equals the admin UID).
6. Write the user profile and community together in one Firestore batch or transaction.
7. Route to Admin Experience only after both documents are valid.

The creator automatically owns the community. Firebase Authentication account creation and Firestore writes are separate operations. If Auth succeeds but Firestore writes fail, show a recovery state, retry missing writes safely, and do not route an incomplete admin account into Admin Experience.

## 3. Login and Startup Routing

| Scenario | Destination |
|---|---|
| Student registration succeeds | Student Experience |
| Admin registration and community creation succeed | Admin Experience |
| Existing student logs in | Student Experience |
| Existing admin logs in | Admin Experience |
| User logs out | Authentication screens |

Firebase Authentication establishes the session. On every startup:
- Load `users/{uid}` from Firestore to provide the trusted account profile and role (`accountType`).
- Student profiles (`accountType == "student"`) route to Student Experience.
- Admin profiles (`accountType == "admin"`) must also have a valid `communities/{uid}` document before entering Admin Experience.
- Missing, invalid, or incomplete profiles must enter a safe recovery/error state. Never route into either protected experience based only on local data.
- DataStore may store convenience preferences, but local state is never proof of authorization.

Authentication state distinguishes loading, signed out, authenticated, profile
error, and registration recovery error. A temporary network error is not
treated as sign-out.

## 4. Authorization Requirements

### Profiles

- A user can create only their own profile (`users/{userId}` where `userId == request.auth.uid`).
- `accountType` must be exactly `student` or `admin`.
- `accountType` cannot change after creation (immutable).
- A user cannot modify another user's profile.
- Profile operations use the authenticated UID, not a caller-supplied UID.

### Student registration

- A student profile can be created without a community document.
- A student cannot create a community.
- A student can create registrations and bookmarks only under their own UID.

### Admin registration and communities

- An admin profile and its community must be created together in one Firestore batch or transaction.
- The community document ID must equal the admin UID (`community.id == adminUid`).
- `createdBy` must equal the admin UID (`createdBy == adminUid`).
- The profile and community must agree on the admin identity (`users/{uid}` has `accountType == "admin"` and `communities/{uid}` has `createdBy == uid`).
- An existing admin cannot create another community; one admin owns exactly one community.
- Community ownership and document identity cannot be changed (`communities/{adminUid}`).
- Any authenticated user can read community information (allowing students to resolve community details when displaying events), but only the owning admin can update their community.
- Community deletion remains deferred.

### Events

- Students can read published events.
- Admins can manage events only for their own community.
- `events.communityId` must identify the admin's community (`events.communityId == adminUid`).
- Ownership must be validated through the community document `communities/{adminUid}`.
- `communityId` and `createdBy` remain immutable during event updates.

### Registrations and bookmarks

- Users can read, create, and delete only their own records.
- Registration operations are student-only. Registration creation validates
  that the event exists, is published, and that the `eventId` field matches
  the document ID.
- Admins may read registration records only through an event-specific
  collection-group query for events owned by their community (`communityId == adminUid`).
- Deterministic event IDs prevent duplicate subcollection records.

Firestore Rules enforce these requirements. UI visibility and locally stored
account type are not security controls.

## 5. Firestore Security Rules Design

The following is a design example, not a complete production ruleset. It
shows the account, ownership, immutable-field, registration, and bookmark
checks. Complete field validation and all application paths must be tested
with the Firebase Emulator Suite before release, including atomic admin registration using `getAfter()` and `existsAfter()`.

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

    function communityExistsAfter(userId) {
      return existsAfter(
        /databases/$(database)/documents/communities/$(userId)
      );
    }

    function communityMatchesAdminAfter(userId) {
      return communityExistsAfter(userId)
        && getAfter(
          /databases/$(database)/documents/communities/$(userId)
        ).data.createdBy == userId;
    }

    function ownsCommunity(communityId) {
      return currentAccountType("admin")
        && communityId == request.auth.uid
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
        ])
        && (
          (
            request.resource.data.accountType == "student"
            && !communityExistsAfter(userId)
          )
          ||
          (
            request.resource.data.accountType == "admin"
            && communityMatchesAdminAfter(userId)
          )
        );

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
      allow create: if self(communityId)
        && accountTypeAfter("admin")
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
owned community (`communityId == adminUid`). Registration-count queries must use
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
account-type immutability, cross-owner community writes, cross-community event
writes, unpublished-event visibility, registrations, bookmarks, missing
profiles, and invalid startup routing.
