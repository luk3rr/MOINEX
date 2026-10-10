---
description: Bump the version, commit it and create the annotated release tag
argument-hint: "[major|minor|patch] (optional override)"
---

Create a new MOINEX release following the "Releasing (version tags)" section of CLAUDE.md.

1. Run `git status --short`. If there are uncommitted changes, stop and tell me; do not commit them on your own.
2. Find the last tag (`git tag --sort=-v:refname | head -1`) and list the commits since it (`git log <tag>..HEAD --oneline`). If there are none, stop.
3. Decide the bump from the most significant commit: `feat:` -> minor, `fix:`/`chore:`-only -> patch. If `$ARGUMENTS` is given, use it instead. If the choice is ambiguous (e.g. a `feat`/`fix` that is only internal tooling), ask me instead of bumping mechanically.
4. Update `version = "X.Y.Z"` in `build.gradle.kts` and commit it alone with the message `chore: bump version to X.Y.Z`.
5. Create the annotated tag `vX.Y.Z` with a short, human-readable title summarizing the release (not the raw commit message), e.g. `git tag -a v2.13.1 -m "Fix Yahoo Finance rate limiting and BRL currency conversion bug"`.
6. The `Co-Authored-By:` trailer is allowed on the commit; never add a session link (`Claude-Session:` or similar) to the commit or the tag.
7. Do not push. Report the new version, the commit, the tag message, and remind me that the Windows installer build needs a GitHub Release (`release: created`) or a manual `workflow_dispatch`; offer to push `main` and the tag.
