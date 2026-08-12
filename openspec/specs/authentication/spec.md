## Purpose

Governs how a device is associated with the family's Xtream account and how
those credentials are stored.

## Requirements

### Requirement: Credentials are entered per device

The app SHALL obtain credentials from the user and SHALL NOT ship any
compiled-in account.

A `BakedCredentials` object previously seeded the account so no one saw a
login screen. It was removed when APKs began publishing to a public
repository, since anything compiled in is extractable from the download.

#### Scenario: First launch on a new device

- **WHEN** the app starts with no stored account
- **THEN** the login screen is shown

#### Scenario: Subsequent launches

- **WHEN** the app starts and an account is stored
- **THEN** the login screen is skipped

### Requirement: Credentials are stored encrypted on device

Credentials SHALL be persisted via `EncryptedSharedPreferences` with the key
held in the Android Keystore.

The honest limitation: Xtream sends username and password in the query string
of every request, so this protects the at-rest copy only.

#### Scenario: Account persists across updates

- **WHEN** a new build is installed over an existing one
- **THEN** the stored account survives and is not re-requested

### Requirement: Credential loading cannot prevent launch

Loading stored credentials SHALL degrade to "signed out" on failure rather
than propagating an exception.

This runs during `Application.onCreate()`, before any UI exists. An
exception there kills the process before any screen — including the crash
reporting screen — can render.

#### Scenario: Keystore access fails

- **WHEN** reading `EncryptedSharedPreferences` throws
- **THEN** the app starts with no account and shows the login screen
- **AND** the failure is logged rather than crashing the process
