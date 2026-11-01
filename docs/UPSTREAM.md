# Upstream maintenance

LibreGram Android is a fork of [exteraless/exteraless](https://github.com/exteraless/exteraless). Keeping a clear upstream relationship makes it possible to receive fixes and security updates without losing LibreGram-specific work.

## Remote layout

- `origin`: `https://github.com/W0rd3r566/libregram-android.git`
- `upstream`: `https://github.com/exteraless/exteraless.git`

Add the upstream remote once:

```bash
git remote add upstream https://github.com/exteraless/exteraless.git
git remote -v
```

If the remote already exists, do not add it again.

## Sync the fork

Fetch upstream changes:

```bash
git fetch upstream
```

Update the local main branch:

```bash
git switch main
git merge --no-edit upstream/main
```

Review the merge carefully, run the build and relevant tests, then push:

```bash
git push origin main
```

If Git reports conflicts, resolve them deliberately. The LibreGram README and project documentation are expected to remain LibreGram-specific; upstream technical changes should still be reviewed and incorporated where relevant.

## Development workflow

For new LibreGram features, create a short-lived branch from the current main branch:

```bash
git switch main
git pull --ff-only origin main
git switch -c feature/short-description
```

Implement and test the change, then open a pull request into `main`.

Keep changes focused. Avoid mixing large upstream merges, application-ID changes, icon work, and new feature logic in one commit.

## Important rules

- Do not force-push to `main`.
- Do not reset the fork to upstream in a way that discards LibreGram commits.
- Preserve all upstream copyright notices, licenses, third-party notices, and design credits.
- Mark LibreGram modifications clearly in release notes and relevant source headers.
- Rebuild and test after upstream merges, especially when Ghost Mode, notifications, plugins, or Android services change.
