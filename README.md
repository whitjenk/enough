# Enough

A free, local-first Android companion for weight and metabolic health, built around fiber as the primary daily metric instead of calories — positioned as the calm, food-first, forgiving alternative in a market of shame-based trackers. See `SPEC.md` for the full positioning, evidence, and phased plan — start with §0.8 (the counter-position) and §0.9 (what this project is actually for, and the deliberately small definition of success it is built against).

## Before a wide launch (human steps, not code)

These can't be delegated to Claude Code — worth doing after Phase 0/1 are built and closed testing has gone well, before pushing for real reach:

- [ ] **Registered dietitian review.** Have an actual RD spend a few hours reviewing the food-swap suggestions, fiber-gap thresholds, and nudge copy. The CDC framework underneath is validated; the specific content built on top of it hasn't been reviewed by a clinician yet, and this app's credibility depends on that gap being closed before wide reach.
- [ ] **Accessibility audit.** Run Android's built-in Accessibility Scanner against every screen; do a full pass with TalkBack through onboarding and daily logging; verify layouts hold up at the largest system font size; check color contrast (WCAG AA) on the fiber ring and success/warning states specifically, since those carry real meaning, not just decoration.
- [ ] **Decide on the optional anonymous outcomes ping** (spec'd in `SPEC.md` and `IMPLEMENTATION_PLAN.md` task 20) with full awareness that it's the one deliberate exception to the zero-server architecture — read that section closely before building it, not just before shipping it.

## Getting started

- [ ] **Verify the name.** Search "Enough" directly on the Play Store, run it through USPTO's trademark search (tmsearch.uspto.gov) for software/health classes, and check domain availability. Do this before anything else — cheapest to fix now.
- [ ] **Install Android Studio and the Android SDK locally**, if not already set up. Claude Code runs real shell/build commands against your actual environment.
- [ ] **This repo already has what it needs:** `CLAUDE.md` (project context and constraints), `SPEC.md` (full product spec, evidence, phased scope), `DESIGN.md` (visual system and Compose tokens), `IMPLEMENTATION_PLAN.md` (the Phase 0 task checklist).
- [ ] **Open Claude Code in this folder.** Kick it off with: *"Read CLAUDE.md and IMPLEMENTATION_PLAN.md, then work through the implementation plan top to bottom, one task at a time — build and verify each one actually runs before checking it off and committing, then move to the next."*
- [ ] **Check in periodically.** Glance at the emulator every few tasks — a passing build isn't the same as a working screen, especially for onboarding and Health Connect permission flows.
- [ ] **When every box in `IMPLEMENTATION_PLAN.md` is checked:** create a Play Console account, get the build into closed testing with 12 real testers, and actually talk to several of them about what they logged and what they ignored. That conversation is the only feedback loop this project has, by design — there's no server, no analytics dashboard.

## Rules that don't change as this grows
No backend. No forced accounts. No subscription traps. No streaks or shame-based copy. No claims of diagnosing or reversing insulin resistance. Full list in `CLAUDE.md` and the language guardrails in `SPEC.md`.
