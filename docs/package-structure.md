# Green Stage — Package Structure

```text
com.greenstage/
|
├── data/
│   ├── model/
│   │   ├── User.kt
│   │   ├── AccountType.kt
│   │   ├── Community.kt
│   │   ├── Event.kt
│   │   ├── Registration.kt
│   │   └── Bookmark.kt
│   │
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── UserRepository.kt
│   │   ├── CommunityRepository.kt
│   │   ├── EventRepository.kt
│   │   ├── RegistrationRepository.kt
│   │   └── BookmarkRepository.kt
│   │
│   └── source/
│       ├── mock/
│       │   ├── MockAuthRepository.kt
│       │   ├── MockUserRepository.kt
│       │   ├── MockCommunityRepository.kt
│       │   ├── MockEventRepository.kt
│       │   ├── MockRegistrationRepository.kt
│       │   └── MockBookmarkRepository.kt
│       │
│       └── firebase/
│           ├── FirebaseAuthRepository.kt
│           ├── FirebaseUserRepository.kt
│           ├── FirebaseCommunityRepository.kt
│           ├── FirebaseEventRepository.kt
│           ├── FirebaseRegistrationRepository.kt
│           └── FirebaseBookmarkRepository.kt
│
├── ui/
│   ├── auth/
│   ├── home/
│   ├── events/
│   ├── saved/
│   ├── profile/
│   ├── admin/
│   ├── components/
│   └── theme/
│
├── navigation/
├── recommendation/
├── notifications/
└── MainActivity.kt
```

## Package responsibilities

| Package | Responsibility |
|---|---|
| `data/model` | User, account type, community, event, registration, and bookmark models |
| `data/repository` | Repository interfaces used by ViewModels |
| `data/source/mock` | In-memory implementations for UI development |
| `data/source/firebase` | Firebase Authentication and Firestore implementations |
| `ui/auth` | Login and student/admin registration paths |
| `ui/home` | Student Home and event discovery |
| `ui/events` | Event list, details, registration, and bookmarks |
| `ui/saved` | The current user's saved events |
| `ui/profile` | Student or admin profile and interests |
| `ui/admin` | Admin Dashboard, Create Event, My Events, and Profile |
| `navigation` | Authentication, student, and admin navigation graphs |
| `recommendation` | Local event recommendation logic |
| `notifications` | Local event reminder handling |

Repository interfaces remain independent of Firebase SDK classes. ViewModels
depend on interfaces, and Compose screens depend on ViewModel state. Manual
object creation selects mock or Firebase implementations; no AppContainer,
Hilt, Dagger, Koin, or other dependency-injection container is introduced.

No additional community-participation package is required in the initial
implementation.
