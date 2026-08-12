## Purpose

Surfaces recent videos from YouTube channels and playlists the family
follows, alongside the IPTV content, and plays them.

## Requirements

### Requirement: Video listing uses RSS feeds

The app SHALL fetch videos from YouTube's public RSS feed endpoint, which
requires no API key.

The same endpoint serves channels and playlists under different parameters
(`channel_id` and `playlist_id`), which is what lets both be stored and
refreshed through one path with no schema difference.

#### Scenario: Refreshing a subscription

- **WHEN** a subscribed channel or playlist is refreshed
- **THEN** its latest videos are stored locally
- **AND** the feed's own title is used to label the subscription

#### Scenario: Cached data is fresh

- **WHEN** a subscription was refreshed under 30 minutes ago
- **THEN** the network is not hit again unless refresh is forced

### Requirement: Subscriptions are per family member

Subscriptions SHALL be stored per family member, and the home rail SHALL show
the selected member's videos.

#### Scenario: Member with subscriptions is selected

- **WHEN** a member with subscriptions is selected on the home page
- **THEN** their videos appear in a rail directly beneath the selector

#### Scenario: Member has no subscriptions

- **WHEN** the selected member has no subscriptions
- **THEN** no YouTube rail is shown

### Requirement: Subscriptions are manageable in-app

Settings SHALL provide a screen to add and remove subscriptions for any
family member.

#### Scenario: Adding by link

- **WHEN** a channel URL, playlist URL or bare id is entered
- **THEN** the id is extracted and the subscription is added

#### Scenario: Adding by search

- **WHEN** free text is entered and search is available
- **THEN** matching channels and playlists are offered to subscribe to

#### Scenario: Removing

- **WHEN** a subscription is removed
- **THEN** it disappears from that member's home rail

### Requirement: Search is optional and degrades cleanly

Search SHALL use the YouTube Data API when a key is compiled in, and the
feature SHALL remain usable without one.

The RSS feeds need no key but cannot search; the Data API can search but
needs one. The key is injected from CI and is extractable from the public
APK, so it must be restricted to the YouTube Data API — exposure is then
capped at quota use rather than account access.

#### Scenario: No key configured

- **WHEN** no API key is present in the build
- **THEN** search is not offered and adding by link still works

#### Scenario: Handle cannot be resolved without a key

- **WHEN** an `@handle` is entered and no key is present
- **THEN** the user is told to paste the `/channel/UC...` link instead,
  because a handle carries no id

### Requirement: Playback uses the official embed

Phone playback SHALL use YouTube's IFrame embed in a WebView. It SHALL NOT
route YouTube URLs through ExoPlayer.

A `youtube.com/watch?v=` URL serves an HTML page, not a media stream.
Passing it to the VOD player meant playback could never start. YouTube
publishes no direct stream URLs; embedding is its supported route.

#### Scenario: Playing a video on phone

- **WHEN** a video is selected
- **THEN** it plays fullscreen in the embedded player
- **AND** leaving the screen stops playback rather than leaving audio running

#### Scenario: Uploader disallows embedding

- **WHEN** a video cannot be embedded
- **THEN** an option to open it in the YouTube app remains available

#### Scenario: Playing on Android TV

- **WHEN** a video is selected on TV
- **THEN** it is handed to the YouTube app, since a D-pad-driven WebView is a
  poor 10-foot experience
