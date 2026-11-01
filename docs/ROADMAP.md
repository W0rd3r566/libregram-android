# Roadmap

This roadmap records planned LibreGram-specific work. A checked item should mean the change is implemented and verified, not merely designed.

## Phase 0: establish a reliable fork

- [x] Create `W0rd3r566/libregram-android` as a fork of exteraless.
- [x] Document project identity, upstream relationship, and attribution policy.
- [x] Document the inherited build setup.
- [ ] Produce and record a successful baseline debug build.
- [ ] Add a repeatable pull-request build check.

## Phase 1: LibreGram identity

- [ ] Audit the complete Android application ID and package migration.
- [ ] Change user-visible application names to LibreGram.
- [ ] Integrate the supplied LibreGram logo as the primary launcher icon.
- [ ] Replace inherited launcher-icon choices with a curated LibreGram icon collection.
- [ ] Verify icon switching, launcher labels, shortcuts, widgets, and notifications.
- [ ] Verify that LibreGram can coexist with the original exteraless installation.

## Phase 2: Status Center

Create one coherent place for activity profiles and notification behavior.

### Built-in profiles

- [ ] **Online:** normal Telegram/client behavior.
- [ ] **Idle:** allow notifications only from selected contacts, groups, and channels.
- [ ] **Do Not Disturb:** suppress Telegram notifications while preserving messages in chats.
- [ ] **Invisible:** activate the existing upstream Ghost Mode configuration.

### Profile controls

- [ ] Per-contact, group, and channel allowlists.
- [ ] Optional rules for direct messages, mentions, and replies.
- [ ] Temporary activation (for example, 30 minutes, 1 hour, or until manually disabled).
- [ ] Schedules and automatic profile switching.
- [ ] User-created profiles with custom names and rule sets.
- [ ] A clear indicator of the currently active local profile.
- [ ] Restore the previous Ghost Mode state when leaving Invisible, where the existing implementation allows it.

### Reliability and privacy requirements

- [ ] Do not silently delete, hide, or mark messages as read.
- [ ] Make it clear that notification suppression is local to this client/device.
- [ ] Do not promise complete invisibility: Telegram may expose activity through server-side behavior outside the client's control.
- [ ] Keep Ghost Mode integration aligned with upstream changes.

## Phase 3: customization

- [ ] LibreGram launcher icon collection.
- [ ] Curated themes and accent colors.
- [ ] Export/import for LibreGram settings and profiles.
- [ ] A clear reset-to-default action.

## Phase 4: release readiness

- [ ] Verify debug and release builds.
- [ ] Verify Firebase and push-notification configuration.
- [ ] Verify signing and update path between releases.
- [ ] Publish source and corresponding build artifacts in accordance with GPL-3.0.
- [ ] Publish release notes that distinguish LibreGram changes from upstream changes.

## Out of scope for the first milestones

- Rewriting the Telegram protocol/client core.
- Duplicating existing exteraless features under new names.
- Removing upstream credits or third-party notices.
- Publishing an APK before app identity, signing, and update behavior are verified.
