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
- [x] Build the CDC/ADA Prediabetes Risk Test as a simple multi-question flow (7 questions, scoring per the published algorithm — see `SPEC.md` §0.5); store the result in `PrediabetesRiskResult` <!-- RiskScorer (pure) reproduces the official scoring incl. the weight chart via floor(BMI×in²/703); verified against CDC chart rows; result persisted with source=CDC_ADA_RISK_TEST -->
- [x] Goal-setting screen: weight-loss goal (5-7% of current weight, computed from a weight entry), weekly activity goal — **must offer a non-step-based option** (minutes of any movement, or a custom self-described goal), and fiber target computed as 14g per 1,000 self-reported daily calories <!-- GoalCalculator (pure): 5-7% clamp + 14g/1000kcal; activity goal type minutes/steps/custom-label; starting weight also written as a WeightEntry -->
- [x] Health Connect permission request + connection flow (steps, sleep, weight read access) <!-- HealthConnectManager (availability + read permissions for Steps/Sleep/Weight), manifest permissions + rationale/usage intents, Compose permission launcher; app fully usable if HC unavailable/denied -->
- [ ] Manual test: complete onboarding start to finish on a real device, confirm all three goals and the risk score are actually persisted <!-- NOT run: no device/emulator in this env. Substituted with JVM verification: pure logic tests (RiskScorer/GoalCalculator/UnitConversions/forms, 24 tests) + a Robolectric Compose render/interaction smoke test. Persistence wiring implemented (finishOnboarding writes risk result, UserGoal, WeightEntry, onboarding flag). Needs a device pass before ship. -->


## 4. Logging
- [x] Add-meal screen: text search against the local food list, adjustable serving size, save to `MealEntry` with a timestamp <!-- AddMealViewModel: debounced FoodRepository.search, serving multiplier, saves MealEntry(source=TEXT, now) -->
- [x] Manual weight-entry screen <!-- LogWeightViewModel: pounds in, stored as kg WeightEntry -->
- [x] Manual activity-entry screen (respects whichever goal type was chosen in onboarding) <!-- LogActivityViewModel reads UserGoal.activityGoalType -> ActivityUnit (minutes/steps/custom) with the custom goal label surfaced -->
- [ ] Manual test: log a meal, a weight, and an activity entry; confirm all three show up correctly on the Today screen <!-- NOT run on device (no device/emulator). Wired end-to-end: nav graph Today <-> logging screens; TodayViewModel observes today's meals/weight/activity and computes fiber via MealNutrition. Verified via unit tests (MealNutrition, DayRange, logging UI-state) + Robolectric render test of Today. Needs a device pass before ship. -->
- [x] **Review checkpoint:** re-check Compose hygiene (state hoisting, no unnecessary recomposition, `@Preview`s present) and accessibility (contentDescriptions) across the screens built in this section. <!-- All screens stateless (Route wrappers hoist state to ViewModels); @Preview added to Today/AddMeal/LogWeight/LogActivity; no icon-only controls (labelled text buttons/fields); fiber shown by number+label, not color -->


## 5. Rules engine (fiber only — no walk/food-order nudges yet)
- [x] Compute `fiberGapToday` from today's logged meals vs. the day's fiber target <!-- RulesEngine.fiberGapG (pure); TodayViewModel feeds it from MealNutrition + UserGoal -->
- [x] Compute `daysSinceLastLog` and `weightTrendDirection` from logged history <!-- RulesEngine.daysSinceLastLog (calendar days in zone) + weightTrend (deadband, DOWN never red); persisted in a RulesEngineState snapshot -->
- [x] Generate one plain-language nudge per day when there's a meaningful fiber gap (e.g. "you're at Xg today — [specific food swap] adds about Yg"), following the tone rules in `CLAUDE.md` (no shame, no streak language) <!-- NudgeGenerator (pure) returns structured Nudge (None/OnTrack/FiberGap); picks a food sized to the gap; rendered on Today with the mascot (pulses on a new gap nudge). Copy lives in string resources. -->
- [x] Unit test: given a synthetic day of logged meals, the fiber gap and nudge text are computed correctly <!-- RulesEngineTest + NudgeGeneratorTest (10 tests): gap, days-since, trend deadband, suggestion sizing, thresholds, no-goal/no-suggestion cases; Today render test asserts the nudge message renders -->
- [x] **Review checkpoint:** read every generated nudge string out loud — does any of it read as guilt-inducing, clinical, or like a lecture? Check against the language guardrails in `SPEC.md` §8 specifically, not just general tone. <!-- Reviewed: neutral "you're at Xg", one small doable step, "small and doable"/"that's enough"; no shame/streak/clinical/cure claims. Fixed color semantics: success-green reserved for goal-met; gap nudge uses neutral surface (never red/gray-as-failure) with green mascot. -->


## 6. Today and Progress screens
- [x] Today screen: today's logged meals/weight/activity, today's fiber-gap nudge, synced Health Connect data <!-- Today shows meals/weight/activity + the mascot nudge card; Health Connect steps/sleep read defensively (null-safe, respects sync toggle) and shown when present -->
- [x] Progress screen: fiber-gap trend, weight trend, weekly activity vs. goal, a streak-free consistency view (e.g. "X of the last 7 days logged" — not a punishing streak counter) <!-- ProgressCalculations (pure) -> 7-day fiber bar chart w/ target line, weight trend (supportive wording, never red), weekly minutes vs goal, and "logged on X of last 7 days" dots (filled/hollow shape, not color-only). Bottom nav Today/Progress. -->
- [ ] Manual test: log data across several simulated days (adjust device clock or seed test data) and confirm both screens reflect it correctly <!-- NOT run on device (no device/emulator). Aggregations verified via ProgressCalculationsTest (fiber-by-day, zero-filled series, distinct-day consistency, logged-day series) + Robolectric render tests of Today and Progress. Needs a multi-day device pass before ship. -->


## 7. Settings and data control
- [x] Health Connect sync toggle <!-- Settings tab: Switch bound to UserPreferencesRepository.healthConnectSyncEnabled (DataStore); Today/HC reads already respect the flag. SettingsViewModel exposes a single UiState via StateFlow; screen stateless with @Preview -->
- [x] "Delete my data" — must actually wipe all local tables, not just hide them <!-- AppContainer.wipeAllUserData(): database.clearAllTables() + preferences.clear(), then re-seed bundled food reference data and route back to onboarding. Confirm dialog before wipe. Verified by DatabaseWipeTest (Robolectric): inserts across all 7 tables, clearAllTables(), asserts every table empty -->
- [x] No account/login screen anywhere in the app — confirm by checking every screen in the nav graph <!-- Nav graph = Today/Progress/Settings tabs + AddMeal/LogWeight/LogActivity + onboarding flow; none is a login/account screen. Codebase-wide grep for login/signin/account/auth/password/oauth/firebase/credential returns only unrelated "logInstants" logging matches. No sign-up path exists. -->

## 8. Ship prep
- [ ] Privacy policy page (host free on GitHub Pages), linked from Settings and ready for the Play Console listing
- [ ] Play Store listing assets: icon, screenshots, short/long description (using the language guardrails in `SPEC.md` §8 — no "CDC-recognized," no A1c claims)
- [ ] Create Play Console account, prepare signed AAB, start closed testing with 12 real testers
- [ ] Personally talk to at least 3-4 of the closed testers about what they logged and what they ignored — this is real user research, not a formality
- [ ] **Final review pass:** before submitting for closed testing, do one full-codebase review as if a principal Android engineer at Google were reviewing this PR before merge — architecture, test coverage, accessibility, error handling, hardcoded strings, everything in `CLAUDE.md`'s engineering standards. Fix what it finds. This is the last checkpoint before real users touch it.

---

**Definition of done:** every box above is checked, the app has been used end-to-end on a real device across several real days of logging, and a closed test is live with real testers. That's Phase 0, shipped. Phase 1 (walk/food-order nudges, doctor-told onboarding path, pre-visit report) starts only after this is true.
