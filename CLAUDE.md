# Enough — project context for Claude Code

## What this is
An Android app named **Enough** that helps people improve weight and metabolic health, with fiber intake as the primary daily metric instead of calories. Built on the framework behind the CDC's National Diabetes Prevention Program. Free, local-first, no account, no subscription, no server.

## The name
"Enough" is the actual positioning, not just a label — one small action a day is enough, missing a day doesn't erase progress, and the person doesn't need to be fixed to start. Let this inform tone: copy, nudges, and error states should never contradict what the name promises. If a string of copy reads as guilt-inducing or demanding, it's wrong for this app regardless of how accurate it is.

## Non-negotiable constraints — do not violate these while building
- **No backend, no cloud API calls, no server of any kind.** Everything runs and stores data on-device (Room/SQLite). If a task seems to need a server, stop and flag it instead of building one.
- **No forced account creation, no login wall, no auto-renewal billing.** The app must be fully usable with zero sign-up.
- **No streaks, badges, punishing gamification, or shame-based copy.** Nudges are supportive, never guilt-based.
- **Never claim "CDC-recognized," "CDC-approved," or that the app diagnoses or reverses insulin resistance.** See the language guardrails in `SPEC.md`.
- **Never claim the app measures or estimates A1c.** It tracks logged behavior, not clinical values.

## Current phase: Phase 0 only
Build **only** what's in the "Phase 0" section of `SPEC.md` right now. Phase 1 and Phase 2 features (walk/food-order nudges, the doctor-told onboarding path, buddy chat, Gemini Nano) are explicitly out of scope until Phase 0 is complete and working. If you find yourself building toward those, stop.

## Tech stack
- Kotlin + Jetpack Compose
- Room (SQLite) for all local storage
- Health Connect Jetpack SDK for reading steps/sleep/weight
- Target API level 35

## Working style for this project
- Work through `IMPLEMENTATION_PLAN.md` top to bottom, one task at a time.
- After each task: build it, verify it actually runs (don't just assume compiling means correct), check the box in `IMPLEMENTATION_PLAN.md`, commit with a message naming the task, then move to the next one.
- If a task is ambiguous or seems to require a decision not covered in `SPEC.md`, stop and ask rather than guessing.
- Keep commits small and scoped to one task each — this is what makes it safe to roll back if something goes wrong later.

## Reference
Full product spec, evidence citations, and all phases: see `SPEC.md` in this same directory.
Visual design system and Compose tokens: see `DESIGN.md` in this same directory. Follow it for color, shape, typography, and motion — don't invent a separate visual language.
