# Project memory
<!-- Approved rules only. One bullet per rule, newest last. Retire, don't delete. -->
- M-001 · 2026-10-29 · src: codekb walkthrough · Rule: Preserve the MOINEX visual identity in any theme change: Dracula palette in dark mode, pill buttons (radius 20, 2px border) and the sidebar green (#0a614e, hover #106e5b). · Why: defined by the project owner as the app's identity.
- M-002 · 2026-10-29 · src: gate 2.2 · Rule: Work that touches several screens goes in a typed branch (feat/*, refactor/*, chore/*) merged into main; only small changes go straight to main. · Why: established pattern in the repository history, confirmed by the owner.
- M-003 · 2026-10-29 · src: gate 2.2 · Rule: UI changes are validated by running the app; deliver a checklist of screens and windows to check in both themes. · Why: there are no UI tests.
- M-004 · 2026-10-29 · src: gate 2.2 · Rule: Do not commit before the owner has seen the diff; passing tests are not enough. · Why: owner reviews the diff before every commit.
- M-005 · 2026-10-29 · src: retrospective Inception (java-25-upgrade) · Rule: Pushing a branch and triggering CI workflows (including the Windows installer) requires an explicit yes at that moment. · Why: outward-facing actions; approval in one context does not extend to the next.
