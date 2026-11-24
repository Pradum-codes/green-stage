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
communities/{adminUid}
events/{eventId}
```

`users/{userId}` contains `accountType: "student" | "admin"`. Account type is selected during registration and remains immutable. An email address can belong to only one Firebase Authentication account in the project. Community Admin registration is public self-registration.
The community document ID in `communities/{adminUid}` equals the owning admin's Firebase Authentication UID (`adminUid`), and `createdBy` equals `adminUid`.
Registration documents contain `eventId` and `registeredAt`; `eventId` must
match the deterministic document ID.

## Registration and community creation

Student registration creates the Auth account and user profile with
`accountType = "student"`. No community document is created.

Community Admin registration creates the Auth account, user profile with
`accountType = "admin"`, and owned community `communities/{uid}` with `createdBy = uid` (where community document ID equals admin UID). The
profile and community writes occur in one Firestore batch or transaction after
Auth succeeds. The operation must be retry-safe. If Auth succeeds but Firestore writes fail, recovery handles missing writes before routing to Admin Experience.

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
    .whereEqualTo("communityId", adminUid)
    .orderBy("startAt")
```

Admin registration counts:

```kotlin
firestore.collectionGroup("registrations")
    .whereEqualTo("eventId", eventId)
    .count()
```

Before issuing this query, verify that the event belongs to the authenticated
admin's owned community (`communityId == adminUid`). The query must not expose unrelated registration
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


# Green Stage --- Firebase Emulator Setup & Developer Guide

This document explains how to configure the Firebase Local Emulator
Suite for Green Stage, connect an Android device to it, and run the
setup during development.

It is intended for developers setting up the project on a new machine.

------------------------------------------------------------------------

## 1. Overview

Green Stage uses Firebase Authentication and Cloud Firestore. During
development, use the Firebase Local Emulator Suite rather than writing
test data to the production Firebase project.

The configured emulators are:

  ------------------------------------------------------------------------
Service                                 Local port Purpose
  --------------------- ---------------------------- ---------------------
Authentication                              `9099` Local email/password
Emulator                                           authentication

Firestore Emulator                          `8080` Local Firestore
database

Emulator UI                                 `4000` Browser interface for
inspecting emulator
data
  ------------------------------------------------------------------------

The Firebase project associated with this repository is:

-   Firebase project ID: `green-stage-app`
-   Android application ID: `com.greenstage.app`

The Firebase Emulator Suite runs on the developer's computer. The
Android application can connect to it either from an Android Studio
emulator or from a physical Android phone.

### Physical-device connection used by this project

Green Stage developers may run the application on a physical Android
phone when the development machine cannot comfortably run an Android
Studio emulator. For that setup, use USB debugging and `adb reverse`.

Connection path:

``` text
Green Stage on Android phone
          |
          | 127.0.0.1:9099 / 127.0.0.1:8080
          |
       ADB reverse
          |
          v
Developer machine
  Auth Emulator :9099
  Firestore     :8080
```

------------------------------------------------------------------------

## 2. Prerequisites

Install the following on the development machine:

-   Node.js and npm
-   Java Development Kit (JDK)
-   Android Studio and Android SDK platform tools (`adb`)
-   Git
-   Access to the Green Stage Firebase project

The Firebase CLI and emulator binaries are managed separately from the
Android Gradle build.

Check that the required command-line tools are available:

``` bash
node --version
npm --version
java -version
adb version
```

The Android phone must have Developer options and USB debugging enabled
when using the physical-device procedure.

------------------------------------------------------------------------

## 3. Install and authenticate Firebase CLI

Install the Firebase CLI globally using npm:

``` bash
npm install -g firebase-tools
```

Verify the installation:

``` bash
firebase --version
```

Sign in:

``` bash
firebase login
```

Check which Firebase projects the signed-in account can access:

``` bash
firebase projects:list
```

Find the Green Stage project and confirm its project ID is
`green-stage-app`. Do not select a different Firebase project by
mistake.

> **Access note:** Each developer must sign in with an account that has
> the required access to the Green Stage Firebase project. Do not share
> Firebase account credentials.

------------------------------------------------------------------------

## 4. Initialize the Emulator Suite in the repository

Run the following commands from the root of the Green Stage repository:

``` bash
cd path/to/green-stage
firebase init emulators
```

In the interactive setup:

1.  Select **Use an existing project**.

2.  Select **`green-stage-app (green-stage)`** as the default project.

3.  Select only these emulators:

  -   Authentication Emulator
  -   Firestore Emulator

4.  Configure the ports:

    Prompt                  Value
      -------------------- --------
    Authentication         `9099`
    Firestore              `8080`
    Enable Emulator UI      `Yes`
    Emulator UI            `4000`

5.  Choose to download the emulator binaries when prompted.

Other Firebase emulators (for example, Functions, Hosting, or Storage)
are not required for the current Green Stage setup.

The CLI may offer to install Firebase Agent Skills for AI coding agents.
This is optional and is not required to run the emulators.

### Expected generated files

The initialization creates or updates:

-   `firebase.json` --- emulator configuration
-   `.firebaserc` --- Firebase project alias configuration

The expected `firebase.json` configuration is:

``` json
{
  "emulators": {
    "auth": {
      "port": 9099
    },
    "firestore": {
      "port": 8080
    },
    "ui": {
      "enabled": true,
      "port": 4000
    },
    "singleProjectMode": true
  }
}
```

The expected `.firebaserc` configuration is:

``` json
{
  "projects": {
    "default": "green-stage-app"
  }
}
```

Check the files:

``` bash
cat firebase.json
cat .firebaserc
```

On Windows PowerShell, use:

``` powershell
Get-Content firebase.json
Get-Content .firebaserc
```

**Important:** The file is named `.firebaserc` (with the `rc` suffix),
not `.firebase`.

------------------------------------------------------------------------

## 5. Start and inspect the emulators

From the repository root, start the suite:

``` bash
firebase emulators:start
```

Keep this terminal open while developing. The emulator processes stop
when the command is stopped or the terminal is closed.

Open the Emulator UI on the development machine:

``` text
http://localhost:4000
```

The UI should show the Authentication and Firestore emulators.

Useful local addresses:

Service                            URL
  ---------------------------------- -------------------------
Emulator UI                        `http://localhost:4000`
Authentication Emulator endpoint   `http://localhost:9099`
Firestore Emulator endpoint        `http://localhost:8080`

The endpoints are for local development. Do not use them as production
service URLs.

### Stop the emulators

In the terminal running `firebase emulators:start`, press:

``` text
Ctrl+C
```

### Emulator data lifecycle

Treat emulator data as development/test data, not as durable production
data. If a team needs to preserve emulator data between sessions,
configure and document an explicit export/import workflow rather than
relying on the default local session state.

------------------------------------------------------------------------

## 6. Connect a physical Android phone using ADB reverse

Use this procedure when the app runs on a physical phone connected to
the development machine over USB.

### 6.1 Enable USB debugging

On the Android phone:

1.  Enable Developer options.
2.  Enable USB debugging.
3.  Connect the phone to the development machine using a USB data cable.
4.  Accept the debugging authorization prompt on the phone, if shown.

### 6.2 Confirm ADB detects the phone

Run:

``` bash
adb devices
```

The device should appear with status `device`, for example:

``` text
List of devices attached
RZCX417P3RN    device
```

If the status is `unauthorized`, unlock the phone and accept the USB
debugging authorization prompt.

### 6.3 Forward the emulator ports

Run:

``` bash
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
```

Verify the mappings:

``` bash
adb reverse --list
```

Expected mappings include:

``` text
UsbFfs tcp:9099 tcp:9099
UsbFfs tcp:8080 tcp:8080
```

`adb reverse` makes the phone's loopback ports forward to the
corresponding ports on the development machine.

If the USB/ADB connection is reset, run the two `adb reverse` commands
again.

------------------------------------------------------------------------

## 7. Configure the Android application to use the emulators

The emulator configuration belongs in the custom `Application` class,
not in an Activity, ViewModel, or Compose screen.

For Green Stage, this is `GreenStageApplication`.

Configure the Firebase SDK instances in `Application.onCreate()` and do
so before any Firebase Auth or Firestore operation is performed.

### 7.1 Required Android manifest permission

Ensure the manifest contains the Internet permission outside the
`<application>` element:

``` xml
<uses-permission android:name="android.permission.INTERNET" />
```

The application declaration must reference the custom Application class.
For the current package structure, verify the actual fully qualified
class name; a relative declaration may look like:

``` xml
<application
    android:name=".GreenStageApplication"
    ... >
```

### 7.2 Debug-only emulator configuration for a physical phone

Use the Android application's debuggable flag to ensure the emulator
endpoints are configured only in debug builds:

``` kotlin
import android.content.pm.ApplicationInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

override fun onCreate() {
    super.onCreate()

    val isDebuggable =
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    if (isDebuggable) {
        FirebaseAuth.getInstance()
            .useEmulator("127.0.0.1", 9099)

        FirebaseFirestore.getInstance()
            .useEmulator("127.0.0.1", 8080)
    }

    // Keep the rest of the application's existing initialization here.
}
```

The snippet shows the emulator configuration to integrate into the
existing `GreenStageApplication`; do not replace unrelated repository
initialization or application setup.

**Why `127.0.0.1`?** With ADB reverse active, the phone's loopback
address and forwarded ports reach the development machine's emulator
ports.

### Android Studio Emulator alternative

If a developer uses the Android Studio Emulator instead of a physical
phone, the host address is different:

``` kotlin
FirebaseAuth.getInstance()
    .useEmulator("10.0.2.2", 9099)

FirebaseFirestore.getInstance()
    .useEmulator("10.0.2.2", 8080)
```

`10.0.2.2` is the Android Studio Emulator's special alias for the
development machine's loopback interface. It is not the address to use
for a physical phone.

Do not configure both host choices at the same time. Use the address
that matches the device being used.

------------------------------------------------------------------------

## 8. Build and install the debug application

With the emulators running and ADB reverse configured, build and install
the debug variant:

``` bash
./gradlew :app:installDebug
```

Alternatively, build only:

``` bash
./gradlew :app:assembleDebug
```

Launch Green Stage on the phone.

The app should continue to use the existing mock repositories until
Firebase repository implementations are deliberately added and wired in.
Configuring Firebase SDK instances to use emulators does not
automatically switch the app from mock repositories to Firebase
repositories.

------------------------------------------------------------------------

## 9. Verify Firestore connectivity from the phone

A successful Gradle build or Firebase SDK initialization log does not
prove that Firestore can reach the local emulator. Perform a temporary
write test to verify the full connection.

Add the following temporary debug-only test after configuring the
Firestore emulator in `GreenStageApplication.onCreate()`:

``` kotlin
FirebaseFirestore.getInstance()
    .collection("connectivity_test")
    .document("android_device")
    .set(
        mapOf(
            "message" to "Connected to Firestore Emulator",
            "device" to android.os.Build.MODEL,
            "timestamp" to System.currentTimeMillis()
        )
    )
    .addOnSuccessListener {
        android.util.Log.d(
            "FirebaseTest",
            "Firestore emulator connection successful"
        )
    }
    .addOnFailureListener { exception ->
        android.util.Log.e(
            "FirebaseTest",
            "Firestore emulator connection failed",
            exception
        )
    }
```

This is temporary diagnostic code. Do not leave it in normal application
startup.

### 9.1 Install and launch

``` bash
./gradlew :app:installDebug
```

Launch Green Stage on the phone.

### 9.2 Inspect Logcat

For example:

``` bash
adb logcat -c
adb logcat -s GreenStageInit:D FirebaseTest:D
```

The expected success message is:

``` text
Firestore emulator connection successful
```

### 9.3 Inspect Firestore Emulator UI

Open:

``` text
http://localhost:4000
```

Navigate to **Firestore → Data**. The test should create:

``` text
connectivity_test
└── android_device
```

The document should contain:

-   `message` --- `Connected to Firestore Emulator`
-   `device` --- the phone model
-   `timestamp` --- epoch milliseconds

When both the success log and document are visible, the
physical-device-to-Firestore-emulator connection is verified.

### What this test verifies

``` text
Green Stage application
        |
        v
Firebase Firestore Android SDK
        |
        v
127.0.0.1:8080 on the phone
        |
        v
ADB reverse over USB
        |
        v
Firestore Emulator on development machine
        |
        v
Document visible in Emulator UI
```

This test verifies Firestore connectivity only. It does not by itself
verify Authentication operations.

### 9.4 Remove the temporary test

After successful verification:

-   Remove the temporary Firestore write.
-   Remove test-only logging/imports that are no longer needed.
-   Keep the debug-only emulator configuration.
-   Keep the Internet permission.
-   Do not add test writes to production application startup.

The test document can be deleted from the Emulator UI if desired.

------------------------------------------------------------------------

## 10. Daily development workflow

For a developer using a physical Android phone, the usual workflow is:

**Terminal 1 --- Start Firebase emulators**

``` bash
cd path/to/green-stage
firebase emulators:start
```

**Terminal 2 --- Connect the phone**

``` bash
adb devices
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
adb reverse --list
```

**Terminal 3 --- Build and install**

``` bash
./gradlew :app:installDebug
```

Then:

1.  Open `http://localhost:4000` on the development machine when
    emulator data needs inspection.
2.  Launch Green Stage on the phone.
3.  Develop and test against the local emulators.
4.  Keep the emulator terminal running during testing.

If the phone disconnects or ADB reverse mappings disappear, reconnect it
and repeat the ADB reverse commands.

------------------------------------------------------------------------

## 11. Troubleshooting

### `firebase: command not found`

Install the Firebase CLI:

``` bash
npm install -g firebase-tools
```

Check npm's global executable path if the command is still unavailable.

### Wrong Firebase project selected

Check:

``` bash
firebase projects:list
cat .firebaserc
```

The default project for this repository should be:

``` text
green-stage-app
```

### Emulator UI does not open

Check that `firebase emulators:start` is still running and that port
`4000` is available.

### `adb devices` shows `unauthorized`

Unlock the phone and accept the USB debugging authorization prompt.
Reconnect the USB cable if needed.

### ADB reverse mappings are missing

Run:

``` bash
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
adb reverse --list
```

### Firebase SDK initializes, but Firestore does not show the test document

Firebase initialization alone does not prove emulator connectivity.
Check, in order:

1.  The Firebase emulator process is running.
2.  The phone is detected by `adb devices`.
3.  Both ADB reverse mappings are present.
4.  The application is using `127.0.0.1` for a physical phone.
5.  Emulator configuration runs before the first Firestore operation.
6.  The temporary write test is actually executed.
7.  Logcat is filtered using the correct tag and package.

Also verify that the app has:

``` xml
<uses-permission android:name="android.permission.INTERNET" />
```

### `10.0.2.2` does not work on a physical phone

That address is for the Android Studio Emulator. Use `127.0.0.1` with
ADB reverse on a USB-connected physical phone.

### Firestore test succeeds but Authentication is not verified

The Firestore test proves only Firestore connectivity. Test
Authentication separately by performing an Auth operation against the
Authentication Emulator. Do not infer Auth connectivity solely from the
Firestore result.

------------------------------------------------------------------------

## 12. Repository and security practices

-   Do not commit `google-services.json` or credentials/secrets to the
    repository.
-   Keep emulator configuration guarded so release builds do not point
    to local emulator endpoints.
-   Do not use production user accounts or production data for emulator
    tests.
-   Do not assume that local emulator behavior proves production
    Security Rules or deployment configuration are correct; test and
    review those separately.
-   Keep Firebase SDK calls inside the project's Firebase
    data-source/repository implementation. Compose screens and
    ViewModels should not access Firebase SDKs directly.
-   Keep the mock and Firebase repository implementations behind the
    same repository interfaces.
-   Do not commit temporary connectivity-test code.

The `.firebaserc` file contains the project alias and project ID, not a
service-account private key. Review repository policy before committing
configuration changes; never put credentials in it.

------------------------------------------------------------------------

## 13. Current Green Stage setup reference

Setting                        Value
  ------------------------------ ----------------------
Firebase project ID            `green-stage-app`
Android application ID         `com.greenstage.app`
Auth Emulator port             `9099`
Firestore Emulator port        `8080`
Emulator UI port               `4000`
Physical phone host            `127.0.0.1`
Physical phone transport       USB + ADB reverse
Android Studio Emulator host   `10.0.2.2`
Firebase CLI project alias     `default`

### Verified during the initial Green Stage setup

-   Firebase CLI authentication and project selection succeeded.
-   Authentication and Firestore emulators were initialized.
-   Emulator UI was accessible.
-   The physical Android phone was detected by ADB.
-   ADB reverse mappings for ports `9099` and `8080` were active.
-   The debug application connected to the Firestore Emulator.
-   A test document appeared in the Emulator UI.

Authentication emulator connectivity still needs its own runtime
operation test.
