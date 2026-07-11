# Implementation plan — Phase 0 (Enough)

Work through these tasks in order, one at a time. For each task: implement it, **review your own diff against the "Engineering standards" section in `CLAUDE.md` before doing anything else**, then build and verify it actually runs correctly (not just compiles), check the box, commit, move on. Stop and ask if anything's ambiguous rather than guessing. Do not start Phase 1 items — they aren't in this list on purpose.

## 1. Project setup
- [x] Create the Android Studio project (Kotlin, Jetpack Compose, min SDK appropriate for Health Connect, target API 35). Application ID: `com.enough.app` (adjust the domain prefix to whatever you actually control once the domain is secured)
- [x] Add Room, Health Connect Jetpack SDK dependencies
- [x] Confirm a blank app builds and runs on an emulator or device before writing any feature code <!-- Builds cleanly (AGP 9.2.1 / Gradle 9.6.1 / Kotlin 2.3.10, compileSdk 36, targetSdk 35, minSdk 26); debug APK is valid & launchable (aapt2 badging shows launcher MainActivity). Literal on-device launch pending a connected device / emulator system image. -->

## 2. Data layer
- [x] Create Room entities: `Food`, `MealEntry`, `WeightEntry`, `ActivityEntry`, `UserGoal`, `PrediabetesRiskResult`, `RulesEngineState` (fields as listed in `SPEC.md` §3, Phase 0 subset only)
- [x] Bundle a starter food list (~150-300 common foods with carbs/fiber/protein per serving) as a JSON asset, sourced from public-domain USDA FoodData Central data; write the one-time seed logic that loads it into Room on first launch <!-- 172 foods in assets/foods.json; FoodSeeder (pure parse + idempotent seedIfEmpty) run from EnoughApplication on first launch -->
- [x] Write basic DAO methods for insert/query on each entity
- [x] Unit test: seeding the food list produces the expected row count and a spot-check food (e.g. "banana") has plausible fiber/carb values <!-- FoodSeedDataTest (pure JVM) + FoodSeederRoomTest (Robolectric, real Room seeding + idempotency + search); 7 tests green -->
- [x] **Review checkpoint:** re-read every file touched in this section against `CLAUDE.md`'s engineering standards (architecture, no hardcoded strings, error handling) before moving to section 3. Fix anything that falls short now, not later. <!-- Added seeding error handling (logged, retries next launch) in EnoughApplication; confirmed no user-facing hardcoded strings in the data layer; parse logic kept Android-free/testable -->


## 3. Onboarding
- [x] Build the entry choice screen: **"just here to build better habits"** (default, skips to goal-setting) vs. **"curious about your risk factors?"** (optional, leads to the risk test) <!-- WELCOME step is now an entry choice: default (emphasized primary) -> startDefaultPath() straight to GOALS; optional (secondary) -> startRiskTestPath(). riskTestPathChosen tracks the path so Back from GOALS returns to WELCOME (default) or RISK_RESULT (optional). Render test drives both buttons. -->
- [x] Build the CDC/ADA Prediabetes Risk Test as a simple multi-question flow (7 questions, scoring per the published algorithm — see `SPEC.md` §0.5), reached only via the optional path; store the result in `PrediabetesRiskResult` <!-- Risk test flow + scoring (RiskScorer, verified against the CDC chart) now reached ONLY via the optional entry choice. finishOnboarding no longer requires risk answers and persists PrediabetesRiskResult only when riskTestPathChosen; the default path shows zero quiz screens. -->
- [x] Goal-setting screen: weekly activity goal — **must offer a non-step-based option** (minutes of any movement, or a custom self-described goal); fiber target computed as 14g per 1,000 self-reported daily calories; **weight-loss goal presented as an equally-weighted optional choice** — "include a weight goal (5-7%), or focus on fiber and activity only?" — never defaulted to on <!-- Weight goal is now opt-in: GoalsForm.includeWeightGoal defaults false, rendered as an equal two-option ChoiceList ("focus on fiber & activity" vs "include a weight goal (5-7%)"). When included, a weight field appears (prefilled from the risk test if that path was taken) + the 5-7% slider. UserGoal weight columns made nullable (hasWeightGoal), migration v1->v2 rebuilds user_goal; a WeightEntry is logged only when a weight goal is set. Non-step activity + 14g/1000kcal fiber unchanged. -->
- [ ] Dietary restrictions/allergies screen: simple multi-select (vegetarian, vegan, gluten-free, dairy-free, nut allergy, other/free text), stored on `UserGoal.dietaryRestrictions` <!-- Schema provisioned in the v1->v2 migration (UserGoal.dietaryRestrictions + dietaryRestrictionOther, converter added); onboarding screen not yet built. -->
- [ ] Dietary restrictions/allergies screen: simple multi-select (vegetarian, vegan, gluten-free, dairy-free, nut allergy, other/free text), stored on `UserGoal.dietaryRestrictions`
- [ ] One optional free-text question: "what's making you want to do this?" — stored on `UserGoal.personalWhy`. Not used by anything yet in Phase 0, but cheap to capture now, expensive to retrofit once real users exist without it <!-- Column provisioned in the v1->v2 migration (UserGoal.personalWhy, nullable); onboarding question not yet built. -->
- [ ] One optional yes/no question: "are you currently taking a GLP-1 medication (Ozempic, Wegovy, Zepbound, or similar)?" — stored on `UserGoal.takesGLP1Medication`. Resolves an explicit stance rather than leaving the app silently mismatched for a real share of users <!-- Column provisioned in the v1->v2 migration (UserGoal.takesGLP1Medication, default false); onboarding question not yet built. -->
- [x] Health Connect permission request + connection flow (steps, sleep, weight read access) <!-- HealthConnectManager (availability + read permissions for Steps/Sleep/Weight), manifest permissions + rationale/usage intents, Compose permission launcher; app fully usable if HC unavailable/denied -->
- [ ] Manual test: complete onboarding via the default path with zero quiz screens shown, and separately via the optional risk-test path; confirm both end up at the same goal-setting flow and persist correctly, including a run where the weight goal is explicitly skipped


## 4. Logging
- [x] Add-meal screen: text search against the local food list, adjustable serving size, save to `MealEntry` with a timestamp <!-- AddMealViewModel: debounced FoodRepository.search, serving multiplier, saves MealEntry(source=TEXT, now) -->
- [x] Manual weight-entry screen <!-- LogWeightViewModel: pounds in, stored as kg WeightEntry -->
- [x] Manual activity-entry screen (respects whichever goal type was chosen in onboarding) <!-- LogActivityViewModel reads UserGoal.activityGoalType -> ActivityUnit (minutes/steps/custom) with the custom goal label surfaced -->
- [ ] Manual test: log a meal, a weight, and an activity entry; confirm all three show up correctly on the Today screen <!-- NOT run on device (no device/emulator). Wired end-to-end: nav graph Today <-> logging screens; TodayViewModel observes today's meals/weight/activity and computes fiber via MealNutrition. Verified via unit tests (MealNutrition, DayRange, logging UI-state) + Robolectric render test of Today. Needs a device pass before ship. -->
- [x] **Review checkpoint:** re-check Compose hygiene (state hoisting, no unnecessary recomposition, `@Preview`s present) and accessibility (contentDescriptions) across the screens built in this section. <!-- All screens stateless (Route wrappers hoist state to ViewModels); @Preview added to Today/AddMeal/LogWeight/LogActivity; no icon-only controls (labelled text buttons/fields); fiber shown by number+label, not color -->


## 5. Rules engine (fiber only — no walk/food-order nudges yet)
- [ ] Compute `fiberGapToday` from today's logged meals vs. the day's fiber target, applying the user's `estimateCalibration` setting as a ±15% adjustment to each meal's fiber value before summing (low = -15%, balanced = 0%, high = +15%) <!-- PARTIAL: base fiber-gap math already built (RulesEngine.fiberGapG, fed by MealNutrition + UserGoal). NEW/MISSING: apply the estimateCalibration ±15% per-meal adjustment before summing. -->
- [x] Compute `daysSinceLastLog` and `weightTrendDirection` from logged history <!-- RulesEngine.daysSinceLastLog (calendar days in zone) + weightTrend (deadband, DOWN never red); persisted in a RulesEngineState snapshot -->
- [ ] Generate one plain-language nudge per day when there's a meaningful fiber gap (e.g. "you're at Xg today — [specific food swap] adds about Yg"), following the tone rules in `CLAUDE.md` (no shame, no streak language). **Every specific food suggestion must be filtered against `UserGoal.dietaryRestrictions` first** — never suggest a food that violates a logged restriction or allergy. **If `UserGoal.takesGLP1Medication` is true, soften the suggestion**: smaller increments, no framing that implies the target should be hit despite reduced appetite <!-- PARTIAL: base nudge generation already built (NudgeGenerator pure -> None/OnTrack/FiberGap, food sized to the gap, copy in string resources). NEW/MISSING: filter suggestions against UserGoal.dietaryRestrictions, and soften increments/framing when takesGLP1Medication is true. -->
- [x] Unit test: given a synthetic day of logged meals, the fiber gap and nudge text are computed correctly <!-- RulesEngineTest + NudgeGeneratorTest (10 tests): gap, days-since, trend deadband, suggestion sizing, thresholds, no-goal/no-suggestion cases; Today render test asserts the nudge message renders -->
- [ ] Unit test: the same logged day produces three different (but all reasonable) fiber-gap values across low/balanced/high calibration settings
- [x] **Review checkpoint:** read every generated nudge string out loud — does any of it read as guilt-inducing, clinical, or like a lecture? Check against the language guardrails in `SPEC.md` §8 specifically, not just general tone. <!-- Reviewed: neutral "you're at Xg", one small doable step, "small and doable"/"that's enough"; no shame/streak/clinical/cure claims. Fixed color semantics: success-green reserved for goal-met; gap nudge uses neutral surface (never red/gray-as-failure) with green mascot. Re-run this once the dietary-filter/GLP-1 nudge variants land. -->


## 6. Today and Progress screens
- [x] Today screen: today's logged meals/weight/activity, today's fiber-gap nudge, synced Health Connect data <!-- Today shows meals/weight/activity + the mascot nudge card; Health Connect steps/sleep read defensively (null-safe, respects sync toggle) and shown when present -->
- [x] Progress screen: fiber-gap trend, weight trend, weekly activity vs. goal, a streak-free consistency view (e.g. "X of the last 7 days logged" — not a punishing streak counter) <!-- ProgressCalculations (pure) -> 7-day fiber bar chart w/ target line, weight trend (supportive wording, never red), weekly minutes vs goal, and "logged on X of last 7 days" dots (filled/hollow shape, not color-only). Bottom nav Today/Progress. -->
- [ ] Manual test: log data across several simulated days (adjust device clock or seed test data) and confirm both screens reflect it correctly <!-- NOT run on device (no device/emulator). Aggregations verified via ProgressCalculationsTest (fiber-by-day, zero-filled series, distinct-day consistency, logged-day series) + Robolectric render tests of Today and Progress. Needs a multi-day device pass before ship. -->


## 7. Settings and data control
- [x] Health Connect sync toggle <!-- Settings tab: Switch bound to UserPreferencesRepository.healthConnectSyncEnabled (DataStore); Today/HC reads already respect the flag. SettingsViewModel exposes a single UiState via StateFlow; screen stateless with @Preview -->
- [ ] Estimate calibration control: a simple three-way choice (lean low / balanced / lean high), default balanced, stored in `UserGoal.estimateCalibration`. Frame it neutrally in the copy — this is a personal preference about how to handle uncertainty, not a "cheat" setting and not a right answer <!-- EstimateCalibration enum (LOW/BALANCED/HIGH with ±15% fiberMultiplier) + column provisioned in the v1->v2 migration (default BALANCED, converter added); Settings control not yet built. -->
- [x] "Delete my data" — must actually wipe all local tables, not just hide them <!-- AppContainer.wipeAllUserData(): database.clearAllTables() + preferences.clear(), then re-seed bundled food reference data and route back to onboarding. Confirm dialog before wipe. Verified by DatabaseWipeTest (Robolectric): inserts across all 7 tables, clearAllTables(), asserts every table empty -->
- [ ] A quiet, non-judgmental support resource, reachable but not intrusive (Settings or Learn) — see `SPEC.md` §0.6
- [x] No account/login screen anywhere in the app — confirm by checking every screen in the nav graph <!-- Nav graph = Today/Progress/Settings tabs + AddMeal/LogWeight/LogActivity + onboarding flow; none is a login/account screen. Codebase-wide grep for login/signin/account/auth/password/oauth/firebase/credential returns only unrelated "logInstants" logging matches. No sign-up path exists. -->

## 8. Ship prep
- [x] Privacy policy page (host free on GitHub Pages), linked from Settings and ready for the Play Console listing <!-- Self-contained policy at docs/index.html (GitHub Pages from main /docs -> https://whitjenk.github.io/enough/); reflects the real architecture (no account/server/analytics, on-device only, HC read-only + revocable, "Delete my data"/uninstall wipe) with the SPEC §8 medical disclaimer. Linked from Settings > Privacy via LocalUriHandler. TWO human steps before Play submission: (1) enable GitHub Pages on main /docs, (2) replace the placeholder you@example.com contact in docs/index.html with a real address. -->
- [ ] Play Store listing assets: icon, screenshots, short/long description (using the language guardrails in `SPEC.md` §8 — no "CDC-recognized," no A1c claims)
- [ ] Create Play Console account, prepare signed AAB, start closed testing with 12 real testers
- [ ] Personally talk to at least 3-4 of the closed testers about what they logged and what they ignored — this is real user research, not a formality
- [x] **Final review pass:** before submitting for closed testing, do one full-codebase review as if a principal Android engineer at Google were reviewing this PR before merge — architecture, test coverage, accessibility, error handling, hardcoded strings, everything in `CLAUDE.md`'s engineering standards. Fix what it finds. This is the last checkpoint before real users touch it. <!-- Reviewed all 83 Kotlin files + resources/manifest/gradle. Architecture is clean UDF (pure/tested domain, stateless Compose, single-UiState StateFlow VMs, repos over Room/HC); no hardcoded UI strings; converters store Instant as epoch millis consistent with DayRange queries; success-role color + in-app medical disclaimers + accessible controls all present; backups excluded, HC read-only/optional. FIXED: TodayViewModel/ProgressViewModel captured now/today at construction, so a retained VM showed a stale day across midnight — now resolved at collection time (rolls over on foreground re-subscribe), now injectable as () -> Instant. Also refreshed two stale docstrings. Full unit suite + debug APK green. Residual (documented): a session held continuously in the foreground across midnight still won't roll over until backgrounded; the pending on-device multi-day manual tests should confirm rollover. NOTE: re-run this pass after the newly-added Phase 0 work (section 3 onboarding restructure, section 5 calibration/dietary/GLP-1, section 7 additions) lands. -->

---

**Definition of done:** every box above is checked, the app has been used end-to-end on a real device across several real days of logging, and a closed test is live with real testers. That's Phase 0, shipped. Phase 1 (walk/food-order nudges, doctor-told onboarding path, pre-visit report) starts only after this is true.

---

# Phase 1 addendum: engagement features

Do not start this section until Phase 0's definition of done above is fully met and you've heard from real closed testers. This isn't in Phase 0 on purpose — it depends on having real usage history to be meaningful at all.

## 9. 90-day milestone view
- [ ] Compute `onboardingCompletedDate` (store it once, at the end of onboarding, don't infer it from first log)
- [ ] Once 90 days have passed since `onboardingCompletedDate`, surface a "how far you've come" view: weight change over the period, % of days the fiber target was hit, activity consistency — pull from existing `WeightEntry`/`MealEntry`/`ActivityEntry` history, no new data sources needed
- [ ] Design this as a genuine accomplishment view, not a clinical printout and not a manufactured streak — follow `DESIGN.md` and the buddy voice section of `CLAUDE.md`
- [ ] Repeat the milestone at 180 and 365 days, tracked via `lastMilestoneIntervalShown`, each time with a longer view than the last; introduce the next focus lever (past fiber → walks → sleep) after the 90-day milestone rather than leaving the person with no next chapter
- [ ] "Export as image" on the milestone view: a simple, genuinely nice shareable card, generated only on explicit tap — never auto-suggested, never a share-sheet popup
- [ ] Set `hasSeenMilestoneReview` once shown, so it doesn't repeat as a surprise every session afterward — make it reachable again from Progress at any time, just not repeatedly pushed
- [ ] Unit test: given 90+ days of synthetic history, the trend calculations are correct; given fewer than 90 days, the view correctly doesn't trigger yet

## 10. Optional 90-day real-world checkpoint nudge
- [ ] Extend `DoctorCheckIn` to be usable from any onboarding path, not just the doctor-told one
- [ ] Around the 90-day mark, surface one gentle, dismissible nudge: "it's been 90 days — want to check in with a doctor or an at-home test to see where things actually stand?"
- [ ] "Not right now" must be a real, easy, one-tap response — track `lastCheckpointNudgeDate` and never repeat more than once per ~90-day window regardless of what they chose
- [ ] Review checkpoint: read the actual nudge copy against the buddy voice guidelines in `CLAUDE.md` — no guilt, no urgency language, a genuine offer someone can decline without friction

## 11. Data export / import (protects everything above)
- [ ] "Export my data" in Settings: writes all local tables to a single file the person can save wherever they choose (Google Drive, email to themselves, etc.)
- [ ] "Import my data" in Settings: restores from that same file format, with a clear warning if it would overwrite existing data
- [ ] Unit test: export then import round-trips without data loss
- [ ] This exists specifically because local-first, zero-server storage means a lost or replaced phone otherwise erases everything, including the 90-day milestone this app is designed around — treat this as high priority within Phase 1, not a someday item

## 12. The "Enough" moment — reset-day feature (do this first)
- [ ] Build the trigger logic: 3+ days since last log (`daysSinceLastLogAtLastCheck`), OR a day where the fiber target was missed by a wide margin, OR user-initiated via buddy chat expressing a rough day (Phase 2 — stub the trigger point now, wire it up once buddy chat exists)
- [ ] Design the actual screen/card: warm, simple, zero catch-up math, zero "you're behind" language — e.g. "today's a fresh start — no need to make up for anything" — with an easy, optional way to log something small, never required
- [ ] Track `lastResetMomentShown` so it doesn't repeat every session once triggered
- [ ] Review checkpoint: this is the single feature most tied to the app's actual name — read the copy specifically checking it delivers on "Enough" as a felt experience, not just a slogan

## 13. Sleep and stress — fill in the actual design (do this second)
- [ ] Sleep nudge: pull duration from Health Connect (already reading this since Phase 0); when sleep is notably short or bedtime notably inconsistent, fire one gentle nudge — same pattern as the walk/food-order nudges, no new architecture needed
- [ ] Stress: no automated detection — Health Connect has no reliable stress signal, and building a fake one would be dishonest. Instead: a simple optional daily check-in (`StressCheckIn`, 1-5 tap, never required), stress content in the Learn section, and treat it as a topic the Phase 2 buddy can discuss rather than something the rules engine tries to sense
- [ ] Update `currentFocusLever` sequencing to the real four-lever version: fiber → walks → sleep → stress
- [ ] Unit test: sleep nudge fires correctly against synthetic Health Connect sleep data; stress check-in persists correctly and is confirmed genuinely optional (skipping it has no visible penalty anywhere in the app)

## 14. Nudge-to-action (grocery list)
- [ ] Add an "add to list" tap on every specific food-swap suggestion, appending to a simple local `GroceryListItem` list
- [ ] Basic list screen: view, check off, remove items
- [ ] Unit test: tapping "add to list" from a nudge correctly creates a `GroceryListItem` linked to the suggested food

## 15. Eating-out quick-log
- [ ] Add a coarser logging path alongside the database-search flow: a simple categorical entry (roughly veggie-heavy / protein-heavy / carb-heavy / mixed) for meals that don't match a precise database food, stored as `MealEntry.entryType = eating-out-estimate`
- [ ] This entry type should visibly respect `estimateCalibration` more explicitly than a database-matched entry — the uncertainty here is real and larger, and the UI should reflect that honestly rather than presenting a fake-precise gram number
- [ ] Unit test: an eating-out entry contributes a reasonable fiber estimate to the day's total under each calibration setting

## 16. Optional anonymous outcomes ping (the one deliberate exception to "no server")
- [ ] At the 90-day milestone, offer a single optional prompt: "would you be willing to anonymously share whether this helped, so it can improve for others?" with three choices (helped / didn't / prefer not to say) and a clear, easy way to decline entirely
- [ ] If accepted, send exactly one increment to a minimal aggregate-only endpoint — no user ID, no device ID, no timestamp, no other field. Set up the simplest possible free-tier serverless function for this (a single counter increment); do not build anything more capable than that
- [ ] Track `hasRespondedToOutcomesPing` so this is asked at most once per person, ever
- [ ] Review checkpoint: confirm by reading the actual network request that it contains nothing beyond the single counter increment — this is the one place in the whole app where a mistake would break the "nothing leaves your phone" promise, so verify it directly rather than assuming the implementation matches the spec
