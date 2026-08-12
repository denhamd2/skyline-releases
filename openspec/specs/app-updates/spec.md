## Purpose

Lets an installed Skyline build discover, download and install a newer build
of itself, so the family does not have to fetch APKs by hand.

## Requirements

### Requirement: Update source is a public releases repository

The app SHALL read releases from the public `denhamd2/skyline-releases`
repository — the same repository its source lives in.

The app sends no credentials, so it must read a public release API and
download publicly accessible assets.

#### Scenario: Checking for an update

- **WHEN** the app queries for the latest release
- **THEN** it requests `api.github.com/repos/denhamd2/skyline-releases/releases/tags/skyline-latest`
- **AND** it does not send an authorization header

#### Scenario: Downloading the APK

- **WHEN** the user accepts an available update
- **THEN** the APK is fetched from the rolling `skyline-latest` tag
- **AND** the download URL is built from the tag, never from the version
  string, because the asset only exists under the tag path

### Requirement: Release lookup uses the tag endpoint

The app SHALL resolve the release by tag rather than via `/releases/latest`.

`/releases/latest` excludes prereleases by definition. The rolling release is
published as a prerelease, so that endpoint returns 404 even for an
authenticated caller.

#### Scenario: Release is marked prerelease

- **WHEN** the published release has `prerelease: true`
- **THEN** the tag endpoint still resolves it
- **AND** the update check succeeds

### Requirement: Update detection compares build identity

The app SHALL decide whether an update exists by comparing a version marker
published in the release notes against its own `BuildConfig.VERSION_NAME`.

The git tag is rolling and identical in every release, so comparing it
reported an update on every launch forever. This was a shipped bug: the
condition `tagName == RELEASE_TAG` was unconditionally true.

#### Scenario: Installed build matches the published build

- **WHEN** the release notes marker equals the installed `VERSION_NAME`
- **THEN** no update is offered and no dialog is shown

#### Scenario: A newer build has been published

- **WHEN** the release notes marker differs from the installed `VERSION_NAME`
- **THEN** an update is offered, labelled with the published version

#### Scenario: Marker is absent

- **WHEN** the release notes contain no `build-version:` marker
- **THEN** no update is offered, because freshness cannot be established

### Requirement: Automatic checks fail silently

The app SHALL suppress error dialogs for the update check it runs itself on
launch, and SHALL surface errors only for checks the user initiated.

An automatic check the user did not ask for must not greet them with a modal
on every start.

#### Scenario: Background check fails

- **WHEN** the launch check fails (offline, rate limited, non-200)
- **THEN** the state returns to idle with no dialog

#### Scenario: User-initiated check fails

- **WHEN** the user explicitly asks to check for updates and it fails
- **THEN** an error is shown, including the HTTP status when there is one

### Requirement: Installation via FileProvider

The app SHALL hand the downloaded APK to the system installer through a
`FileProvider`, declared with `android:grantUriPermissions="true"`.

`FileProvider.attachInfo()` throws `SecurityException: Provider must grant
uri permissions` without that attribute. ContentProviders initialise before
`Application.onCreate()`, so the throw killed the process on every launch,
before any crash handler existed and before any Activity started — the app
vanished instantly with nothing recorded. Do not remove the attribute.

#### Scenario: Installing a downloaded update

- **WHEN** the APK download completes
- **THEN** a content URI is produced via `FileProvider.getUriForFile`
- **AND** an `ACTION_VIEW` intent is launched with
  `FLAG_GRANT_READ_URI_PERMISSION`

#### Scenario: App launches with the provider declared

- **WHEN** the app process starts
- **THEN** provider initialisation succeeds and the app reaches its first screen
