# Integrated alpha checkpoint

This workflow includes WS diagnostics fixes, opt-in startup, the quick menu, duration controls, confirmed settings reset, build identity in reports, and launcher/intro/settings branding corrections. Filters, message reminders, screen-sharing privacy mode and private message notes are NOT implemented.

Build from a clean checkout: run `python3 Tools/prepare-alpha.py` ONCE before Gradle. Source generation remains a temporary CI preparation step; Gradle no longer mutates sources itself. Re-running the older startup script on alpha-prepared settings is unsupported. Directly committing all generated Java sources remains follow-up technical debt.

Only the APK artifact is uploaded by this workflow to avoid confusion with standalone JNI libraries. The artifact name begins `LibreGram-APK-arm64-` followed by the commit SHA.

Host checks verified the combined preparation on a checkout snapshot. Android compilation and runtime tests are still required. Timer stop may be delayed during Android deep sleep; it checks elapsed time on execution. Existing storage folders are not moved or deleted. Matching signing identity is required to update an existing installation.
