# LibreGram Android

**LibreGram Android** is an independent community project built on top of [exteraless](https://github.com/exteraless/exteraless), an open-source Telegram client fork.

The repository is the home for LibreGram-specific development: app identity, carefully integrated improvements, and features that extend the upstream project without erasing its history or authorship.

> **Development status:** early development. This repository is a source fork, not a finished or officially supported app release. The LibreGram app-name, package-ID, icon, and release-signing migration is not complete yet.

## Project goals

- **Respect upstream.** Preserve the original license, copyright notices, design credits, and third-party acknowledgements.
- **Keep a maintainable fork.** Track upstream changes and avoid unnecessary rewrites of working exteraless functionality.
- **Build a distinct identity.** Introduce the LibreGram name and the supplied LibreGram logo, then replace inherited launcher-icon choices with a clearly maintained LibreGram collection.
- **Add useful features.** Prioritize a Status Center with configurable activity profiles, notification rules, schedules, and integration with the existing Ghost Mode implementation.
- **Verify before release.** Build and test changes before publishing APKs. No build is considered verified until CI or a local build succeeds.

## Planned first-party work

See the [roadmap](docs/ROADMAP.md) for the current plan.

The initial feature direction is:

1. **Status Center:** Online, Idle, Do Not Disturb, and Invisible profiles.
2. **Notification rules:** allowlists for selected contacts, groups, and channels, plus schedules and temporary modes.
3. **Ghost Mode integration:** Invisible should control the existing upstream Ghost Mode rather than introduce a competing implementation.
4. **Launcher icon collection:** keep the icon-picker experience, but curate LibreGram icons and use the supplied LibreGram logo as the primary identity.

These are development goals, not claims that the features are already implemented.

## Building

Start with the [build guide](docs/BUILDING.md). It documents the current inherited build requirements and configuration.

The current upstream baseline still contains its original Android application ID and some internal exteraless identifiers. Do not assume the app has already been rebranded just because the repository is named LibreGram Android. The package-ID and app-identity migration will be handled as a separate, audited change.

## Keeping up with upstream

This repository is a GitHub fork of [exteraless/exteraless](https://github.com/exteraless/exteraless). The recommended workflow for syncing changes is documented in [UPSTREAM.md](docs/UPSTREAM.md).

## Project documentation

- [Roadmap](docs/ROADMAP.md)
- [Project status and decisions](docs/PROJECT_STATUS.md)
- [Building from source](docs/BUILDING.md)
- [Upstream sync workflow](docs/UPSTREAM.md)

## Origin, credits, and license

LibreGram Android is based on [exteraless](https://github.com/exteraless/exteraless), which in turn builds on work from [NagramX](https://github.com/risin42/NagramX) and other projects. Please consult the source files and original project documentation for the complete attribution chain.

The upstream project is distributed under the **GNU General Public License v3.0**. The original [LICENSE](LICENSE) file is retained. Copyright notices, third-party licenses, and design credits must remain with the code and assets they cover.

LibreGram Android is an independent community project. It is not an official Telegram app and is not affiliated with Telegram, exteraless, or exteraGram.

---

## English summary

LibreGram Android is an independent community fork built on [exteraless](https://github.com/exteraless/exteraless). The goal is to maintain a clean upstream relationship while adding a distinct LibreGram identity and carefully selected features such as a configurable Status Center, notification profiles, Ghost Mode integration, and a curated launcher-icon collection.

The project is in early development. App branding, package identifiers, signing, and release infrastructure are being audited before the first LibreGram-branded APK is published. The original GPL-3.0 license and upstream attributions are retained.
