# Implementation plan — Phase 0 (Enough)

Work through these tasks in order, one at a time. For each task: implement it, **review your own diff against the "Engineering standards" section in `CLAUDE.md` before doing anything else**, then build and verify it actually runs correctly (not just compiles), check the box, commit, move on. Stop and ask if anything's ambiguous rather than guessing. Do not start Phase 1 items — they aren't in this list on purpose.

## 1. Project setup
- [ ] Create the Android Studio project (Kotlin, Jetpack Compose, min SDK appropriate for Health Connect, target API 35). Application ID: `com.enough.app` (adjust the domain prefix to whatever you actually control once the domain is secured)
- [ ] Add Room, Health Connect Jetpack SDK dependencies
- [ ] Confirm a blank app builds and runs on an emulator or device before writing any feature code

## 2. Data layer
- [ ] Create Room entities: `Food`, `MealEntry`, `WeightEntry`, `ActivityEntry`, `UserGoal`, `PrediabetesRiskResult`, `RulesEngineState` (fields as listed in `SPEC.md` §3, Phase 0 subset only)
- [ ] Bundle a starter food list (~150-300 common foods with carbs/fiber/protein per serving) as a JSON asset, sourced from public-domain USDA FoodData Central data; write the one-time seed logic that loads it into Room on first launch
- [ ] Write basic DAO methods for insert/query on each entity
- [ ] Unit test: seeding the food list produces the expected row count and a spot-check food (e.g. "banana") has plausible fiber/carb values
- [ ] **Review checkpoint:** re-read every file touched in this section against `CLAUDE.md`'s engineering standards (architecture, no hardcoded strings, error handling) before moving to section 3. Fix anything that falls short now, not later.

## 3. Onboarding
- [ ] Build the CDC/ADA Prediabetes Risk Test as a simple multi-question flow (7 questions, scoring per the published algorithm — see `SPEC.md` §0.5); store the result in `PrediabetesRiskResult`
- [ ] Goal-setting screen: weight-loss goal (5-7% of current weight, computed from a weight entry), weekly activity goal — **must offer a non-step-based option** (minutes of any movement, or a custom self-described goal), and fiber target computed as 14g per 1,000 self-reported daily calories
- [ ] Health Connect permission request + connection flow (steps, sleep, weight read access)
- [ ] Manual test: complete onboarding start to finish on a real device, confirm all three goals and the risk score are actually persisted

## 4. Logging
- [ ] Add-meal screen: text search against the local food list, adjustable serving size, save to `MealEntry` with a timestamp
- [ ] Manual weight-entry screen
- [ ] Manual activity-entry screen (respects whichever goal type was chosen in onboarding)
- [ ] Manual test: log a meal, a weight, and an activity entry; confirm all three show up correctly on the Today screen
- [ ] **Review checkpoint:** re-check Compose hygiene (state hoisting, no unnecessary recomposition, `@Preview`s present) and accessibility (contentDescriptions) across the screens built in this section.

## 5. Rules engine (fiber only — no walk/food-order nudges yet)
- [ ] Compute `fiberGapToday` from today's logged meals vs. the day's fiber target
- [ ] Compute `daysSinceLastLog` and `weightTrendDirection` from logged history
- [ ] Generate one plain-language nudge per day when there's a meaningful fiber gap (e.g. "you're at Xg today — [specific food swap] adds about Yg"), following the tone rules in `CLAUDE.md` (no shame, no streak language)
- [ ] Unit test: given a synthetic day of logged meals, the fiber gap and nudge text are computed correctly
- [ ] **Review checkpoint:** read every generated nudge string out loud — does any of it read as guilt-inducing, clinical, or like a lecture? Check against the language guardrails in `SPEC.md` §8 specifically, not just general tone.

## 6. Today and Progress screens
- [ ] Today screen: today's logged meals/weight/activity, today's fiber-gap nudge, synced Health Connect data
- [ ] Progress screen: fiber-gap trend, weight trend, weekly activity vs. goal, a streak-free consistency view (e.g. "X of the last 7 days logged" — not a punishing streak counter)
- [ ] Manual test: log data across several simulated days (adjust device clock or seed test data) and confirm both screens reflect it correctly

## 7. Settings and data control
- [ ] Health Connect sync toggle
- [ ] "Delete my data" — must actually wipe all local tables, not just hide them
- [ ] No account/login screen anywhere in the app — confirm by checking every screen in the nav graph

## 8. Ship prep
- [ ] Privacy policy page (host free on GitHub Pages), linked from Settings and ready for the Play Console listing
- [ ] Play Store listing assets: icon, screenshots, short/long description (using the language guardrails in `SPEC.md` §8 — no "CDC-recognized," no A1c claims)
- [ ] Create Play Console account, prepare signed AAB, start closed testing with 12 real testers
- [ ] Personally talk to at least 3-4 of the closed testers about what they logged and what they ignored — this is real user research, not a formality
- [ ] **Final review pass:** before submitting for closed testing, do one full-codebase review as if a principal Android engineer at Google were reviewing this PR before merge — architecture, test coverage, accessibility, error handling, hardcoded strings, everything in `CLAUDE.md`'s engineering standards. Fix what it finds. This is the last checkpoint before real users touch it.

---

**Definition of done:** every box above is checked, the app has been used end-to-end on a real device across several real days of logging, and a closed test is live with real testers. That's Phase 0, shipped. Phase 1 (walk/food-order nudges, doctor-told onboarding path, pre-visit report) starts only after this is true.
