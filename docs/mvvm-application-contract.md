# Green Stage — Commit 02.3: MVVM and Application Contracts

**Status:** Proposed for review  
**Phase:** 02 — Architecture & Contracts  
**Purpose:** Define presentation-layer responsibilities and integration contracts before UI implementation. This document specifies boundaries and expected behavior; it does not implement screens, ViewModels, navigation, or dependency wiring.

## 1. Scope and principles

Green Stage uses MVVM:

```text
Compose Screen <-> ViewModel -> Repository Interface -> Data Source
```

- Compose screens render state and forward user actions.
- ViewModels coordinate screen workflows and own screen UI state.
- ViewModels depend on repository interfaces, never concrete mock or Firebase classes.
- ViewModels and Compose screens do not access Firebase SDKs.
- Repositories own data access and return the contracts already defined in the data layer.
- Navigation and application-level authentication routing are owned by the navigation/application layer, not individual screens.
- Do not introduce a UseCase layer, AppContainer, Hilt, Dagger, Koin, or another dependency-injection container.
- ViewModels are not required for navigation-only screens.

## 2. ViewModel responsibility mapping

The following mapping is the agreed responsibility proposal derived from `screens-and-navigation.md`.

| Screen / flow | ViewModel | Responsibilities |
|---|---|---|
| Login | `LoginViewModel` | Accept credentials, perform basic input validation, call `AuthRepository.login`, expose operation state and errors. Successful authentication is handled by root auth-state routing. |
| Registration choice | None required | Navigate to Student or Community Admin registration. |
| Student registration | `StudentRegistrationViewModel` | Accept student registration fields, validate input, call `AuthRepository.registerStudent`, expose registration and recovery errors. |
| Community Admin registration | `AdminRegistrationViewModel` | Accept user and community fields, validate input, call `AuthRepository.registerCommunityAdmin`, expose registration/recovery state. |
| Forgot Password | `ForgotPasswordViewModel` | Accept email, call `AuthRepository.sendPasswordResetEmail`, expose success and failure feedback. |
| Home | `HomeViewModel` | Load published active/upcoming events and available recommendations/category shortcuts. |
| Explore Events | `ExploreEventsViewModel` | Load published events and apply initial search/category filtering locally. |
| Student Event Details | `EventDetailsViewModel` | Load event and community details; check and perform registration and bookmark operations. |
| Saved Events | `SavedEventsViewModel` | Load current user's bookmarks and associated events; support bookmark removal. |
| Student Profile | `ProfileViewModel` | Load current user, display account type, update permitted profile fields and interests. |
| Dashboard | `DashboardViewModel` | Load owned community, active/upcoming events, basic event counts and registration counts where available. |
| Create Event | `CreateEventViewModel` | Manage event form state and create an event with the selected save/publish state. |
| My Events | `MyEventsViewModel` | Load events belonging to the admin's owned community and coordinate publish, unpublish and delete actions. |
| Admin Event Details | `AdminEventDetailsViewModel` | Load an owned event and expose relevant management details, including registration count where available. |
| Edit Event | `EditEventViewModel` | Load an existing event and submit permitted changes. |
| Admin Profile | `ProfileViewModel` | Display admin account, account type and owned community information. |

### Shared and application-level responsibilities

- `ProfileViewModel` may serve both Student and Admin profile screens. It must expose only role-appropriate data and actions.
- Logout is initiated through `AuthRepository.logout()`. The root application observes the resulting auth state and routes to authentication.
- `AppNavigation` owns auth-state observation and selection of the authentication, Student or Admin graph.
- A screen ViewModel must not independently decide which top-level role graph is active.
- Registration choice is navigation-only unless later UI requirements introduce screen-specific state.

## 3. Screen-level UI state contracts

These are required state contents, not mandatory Kotlin property names or prescribed class implementations. The UI developer may organize the state types while preserving the information and behavior.

### 3.1 Authentication

| Screen | State must represent |
|---|---|
| Login | Email, password, loading state, input validation errors, authentication error. |
| Student Registration | Name, email, password, loading state, validation errors, registration error. |
| Community Admin Registration | Name, email, password, community name, community description, community category, loading state, validation errors, registration/recovery error. |
| Forgot Password | Email, loading state, success feedback, error feedback. |

The registration-choice screen is navigation-only and has no required ViewModel state.

### 3.2 Student experience

| Screen | State must represent |
|---|---|
| Home | Active and upcoming published events, recommendations when available, category shortcuts, loading state, error and empty state where applicable. |
| Explore Events | Published events, search query, selected category, loading state, error and empty filtered-result state. |
| Event Details | Event details, resolved community information, event status, registration status, bookmark status, action-specific loading state and errors. |
| Saved Events | Saved events with their event details, loading state, empty state, removal/action status and error. |
| Profile | User details, interests, read-only account type, loading state, update status and error. |

### 3.3 Admin experience

| Screen | State must represent |
|---|---|
| Dashboard | Owned community, active/upcoming events, basic event counts, registration counts where available, loading state and error. |
| Create Event | Title, description, category, venue, start/end time, optional image URL, save/publish selection, loading state, validation errors and submission error. |
| My Events | Owned-community events, loading state, empty state, operation status and error. |
| Event Details | Event details, community information, registration count where available, publishing status, loading state and error. |
| Edit Event | Existing event data, editable fields, loading state, update status, validation errors and submission error. |
| Profile | Admin account information, read-only account type, owned community information, loading state and error. |

The account type is displayed but cannot be edited. Role transfers and multiple-community selection are outside the initial scope.

## 4. UI state and event conventions

### 4.1 Persistent screen state

- Screen state should be immutable.
- ViewModels expose screen state as `StateFlow`.
- Compose collects state using lifecycle-aware collection (`collectAsStateWithLifecycle`).
- The ViewModel is the owner of screen state; Compose does not maintain a competing copy of server/repository data.

### 4.2 Loading states

Distinguish where useful between:
- Initial screen loading.
- Refresh/loading of screen data.
- Action-specific loading, such as registering, bookmarking, publishing or deleting.

An action-specific operation should not unnecessarily replace the entire screen with a full-screen loading state.

### 4.3 Error states

- Errors required for persistent screen rendering belong in UI state.
- Clear or replace stale errors when a new operation begins.
- Convert technical failures into user-readable messages in the presentation layer.
- Do not expose raw Firebase exceptions or SDK objects to Compose.
- Preserve enough error context for the screen to provide an appropriate retry or recovery action.

### 4.4 One-time UI events

Transient effects should be communicated separately from persistent screen state, using an event stream such as `SharedFlow` where appropriate.

Examples:
- Snackbar messages.
- One-time confirmation messages.
- One-time screen effects that are not already driven by application state.

Do not model a transient snackbar or navigation command as a permanent Boolean in `UiState`.

### 4.5 Success handling

Success is represented by updated state/data or a one-time event when appropriate. Authentication success is not routed by a Login screen's local navigation command; the root observes the authenticated application state and selects the correct graph.

## 5. Repository-to-ViewModel integration

The repository contracts already use `Result<T>` for one-shot operations and `Flow<AuthState>` for continuous authentication state.

- ViewModels call repository interfaces only.
- Suspend repository operations are invoked from ViewModel-owned coroutine scopes.
- A successful `Result<T>` is mapped to the relevant screen state.
- A failed `Result<T>` is mapped to a user-readable error state or recovery event.
- Compose screens do not call repositories directly and do not interpret `Result<T>`.
- Continuous auth state is observed from `AuthRepository.observeAuthState()`.
- No Firebase SDK types cross the data-layer boundary.

A ViewModel coordinates screen workflows; it does not duplicate repository data-access logic.

## 6. Authentication-state propagation

`AuthState` is the existing application authentication contract:

| AuthState | Application-level behavior |
|---|---|
| `Loading` | Keep the application in an initialization/loading state; do not prematurely show a protected graph. |
| `SignedOut` | Show the authentication experience. |
| `Authenticated(user)` | Determine the role from the trusted user profile and select the corresponding Student or Admin experience. |
| `ProfileError(message)` | Show a safe profile recovery/error state; do not enter a protected graph. |
| `RegistrationRecoveryError(message)` | Show the registration recovery experience; do not enter the Admin graph until required profile and community documents are valid. |

Additional rules:

- The trusted account type is the Firestore user profile's `accountType`, not a user-editable preference.
- DataStore may hold convenience preferences but is not the source of truth for account type or authentication.
- The root application/navigation coordinator owns auth-state observation and top-level graph selection.
- Admin access requires a valid admin profile (`users/{uid}`) AND the required owned community (`communities/{uid}`). Community document ID equals the authenticated admin's UID (`community.id == adminUid` and `communities.createdBy == adminUid`), allowing direct retrieval via `communities/{authenticatedUid}`.
- Logout clears the authenticated session and returns the application to authentication screens. It does not delete persisted user or event data.
- A missing, invalid or unsupported profile must lead to a safe recovery/error state, never a protected graph.

## 7. Role-specific screen boundaries

### Student

The Student graph exposes exactly four bottom-navigation destinations:
1. Home
2. Explore Events
3. Saved Events
4. Profile

Event Details is a nested screen, not a fifth bottom-navigation destination.

### Community Admin

The Admin graph exposes exactly four bottom-navigation destinations:
1. Dashboard
2. Create Event
3. My Events
4. Profile

Admin Event Details and Edit Event are nested screens, not additional bottom-navigation destinations.

No membership-management, join-community, member-request or multiple-community screens are part of the initial scope.

## 8. Out of scope for Commit 02.3

This commit defines presentation contracts only. It does not implement:
- Compose screens or visual components.
- ViewModel classes or UI state classes.
- Navigation graphs or route implementations.
- Repository implementations or Firebase integration.
- Manual dependency wiring.

Navigation destinations, root navigation responsibilities and manual object ownership are finalized under Commit 02.4.

## 9. Review and completion criteria

Commit 02.3 can be marked complete when both developers agree that:

- Each screen has a clear ViewModel responsibility or is explicitly navigation-only.
- Required screen state contents are understood.
- Loading, error, success and one-time event conventions are understood.
- Repository interfaces and `Result<T>` handling boundaries are understood.
- Authentication-state propagation and role routing responsibilities are understood.
- No presentation-layer implementation has been pulled into this architecture commit.

**Suggested commit message:**

```text
docs: define MVVM and application contracts
```