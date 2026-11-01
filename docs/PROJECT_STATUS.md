# Project status

Last updated: 2026-11-02

## Repository

- Repository: [W0rd3r566/libregram-android](https://github.com/W0rd3r566/libregram-android)
- Upstream: [exteraless/exteraless](https://github.com/exteraless/exteraless)
- Default branch: `main`
- License inherited from upstream: GPL-3.0

This repository is a real GitHub fork. Its original commit history and parent relationship are preserved.

## Confirmed project decisions

| Area | Decision |
| --- | --- |
| Project/repository name | LibreGram Android / `libregram-android` |
| App-facing identity | LibreGram |
| Base | Continue from exteraless; do not rewrite working upstream systems without a reason |
| Main logo | Use the LibreGram logo supplied by the project owner |
| Launcher icons | Keep the existing icon-picker concept, but build a LibreGram-owned collection |
| Activity modes | Add a Status Center with configurable profiles |
| Invisible mode | Integrate the existing Ghost Mode; do not create a parallel ghost implementation |
| Attribution | Preserve upstream and third-party copyright, license, and design credits |

## Current baseline findings

The repository is still based on the original exteraless Android project. The following items are known to be inherited and have **not** yet been migrated:

- Android application ID in `gradle.properties`: `com.exteraless.app`
- Launcher/application label in the manifest resources still resolves to the original exteraless name.
- Internal namespaces and implementation packages contain upstream identifiers, including `app.exteraless.*`.
- Firebase configuration, signing, app links, providers, and notification integrations must be checked against the final application ID before a branded APK is released.

The repository name and README have been updated; that does not mean the installed Android app has already been fully rebranded.

## Work completed

- [x] Created the GitHub fork under the LibreGram Android repository name.
- [x] Preserved the upstream fork relationship and original license.
- [x] Replaced the repository README with a project-specific overview.
- [x] Added initial roadmap, build, and upstream-maintenance documentation.

## Not yet verified

- [ ] A clean local debug build.
- [ ] GitHub Actions behavior and required repository secrets.
- [ ] Final Android application ID and full package migration.
- [ ] Firebase push notifications under the new application ID.
- [ ] Final launcher icon resources and Android icon-switching behavior.
- [ ] Installation/update behavior alongside the original exteraless app.

Do not describe a build, release, or runtime feature as working until it has been tested.

## Next engineering steps

1. Build the unmodified fork once and record any baseline failures.
2. Audit every application-ID, package, provider-authority, deep-link, Firebase, and signing reference.
3. Choose the final application ID and migrate it in one reviewed change.
4. Apply the LibreGram display name and supplied main logo, then replace inherited launcher-icon options with the new collection.
5. Implement Status Center by extending existing notification and Ghost Mode systems.
6. Add CI checks for pull requests before enabling automated release publishing.
