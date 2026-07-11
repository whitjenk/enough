# Enough — product spec and phased build plan

## Positioning statement (the north star — don't drift from this)
For the ~40% of US adults with insulin resistance and ~98 million with prediabetes — most of whom either don't know it, or know and have no concrete next step — who want to improve weight and metabolic health without medication, **Enough** is a free Android companion that turns "eat healthier and move more" into one specific, evidence-based action a day, starting with fiber. Unlike MyFitnessPal or Noom (never touch blood sugar) or mySugr/Gluroo (built for people already diagnosed, often on insulin), this sits in the underserved gap between them.

Four differentiators, each answering a specific competitor failure found in real user reviews:
1. **Fiber-first, not calorie-first** — safest, most under-addressed lever, zero risk profile, evidence-backed
2. **Nothing to hide** — no server, no account, no subscription trap, verifiable by reading the code
3. **Behavior design, not information dump** — one lever at a time, nudges tied to real moments, never a lecture
4. **Zero cost by architecture, not by promise** — on-device AI, local storage, structurally free, not a pricing decision that can change later

**Core idea:** a genuinely helpful Android companion for weight and A1c — fast logging, evidence-based coaching, zero recurring cost to you. Built on the CDC diabetes prevention framework, running entirely on-device.

**Cost reality:** the only unavoidable expense is the one-time $25 Google Play Console registration fee. Everything else in this design — the coaching logic, the storage, eventually the AI — runs for free, indefinitely, at any number of users, because none of it touches a server you pay for.

**Status:** the previous version of this doc described everything as one build. It wasn't one build — it was three. This version re-slices the same plan into phases so Phase 0 is genuinely shippable in a weekend, and everything else has an honest place to live until it's earned.

---

## 0. Validated market gap (why this is worth building)

- Logging friction is the #1 reason people quit tracking apps; fast, low-friction logging is table stakes, not a nice-to-have.
- Trust and billing damage (forced accounts, surprise charges, forced data migrations) is actively burning goodwill in this category — local-first, zero-account, zero-subscription is a real answer to a real problem.
- Even respected diabetes apps haven't closed the loop between glucose readings and actual food insight.
- Bot-like, scripted-feeling coaching is a specific, named complaint against paid competitors.
- The real underserved seam: ~115 million U.S. adults with prediabetes, sitting between generic calorie counters (no blood sugar awareness) and diagnosed-diabetic tools (insulin dosing, bolus calculators — overkill for this group).

## 0.5 The insulin resistance / fiber gap (why fiber is the first lever)

- **40% of US adults 18–44 are insulin resistant by HOMA-IR** (NHANES-based analysis), global prevalence ~26.5%. Standard checkups test fasting glucose/A1c, not fasting insulin or HOMA-IR — the body compensates with more insulin for years before glucose numbers move, so people can look "normal" on a routine panel while already resistant.
- **~90% of the ~98 million Americans with prediabetes don't know they have it** (CDC) — same detection gap, one step further along.
- **The CDC/ADA already built the right screening tool.** The Prediabetes Risk Test is validated, public-domain, 7 questions, no blood draw.
- **Fiber is the safest, most under-addressed lever available.** ~25g/day (women) to ~38g/day (men), roughly 14g per 1,000 calories. The average American gets ~15–16g/day — ~95% of adults fall short. Slows glucose absorption, feeds the gut microbiome, improves satiety, with none of the risk of a glycemic scoring formula.

---

## 1. Phased scope

### Phase 0 — ship this first (genuinely weekend-sized)
The smallest thing that is honestly a complete, useful product on its own:

| Screen | Purpose |
|---|--------|
| Onboarding | The real CDC/ADA Prediabetes Risk Test only (one path — skip the doctor-told branch for now); connect Health Connect; set a 5–7% weight-loss goal, a weekly activity target (with a non-step-based option — see §5), and a fiber target (14g/1,000 kcal) |
| Today | Logged meals, weight, synced steps/sleep; the day's fiber-gap nudge |
| Add meal / activity | Fast text search against the local food list; manual weight/activity entry |
| Progress | Fiber gap vs. target (headline metric), weight trend, streak-free consistency view |

**No AI, no device tiering, no buddy chat, no walk/food-order nudges yet.** Just: set targets, log fast, get one honest fiber nudge a day, see the trend. This alone is a real, shippable, useful app.

### Phase 1 — once Phase 0 is live and you've heard from real testers
- Post-meal walk nudge (with its Health Connect redundancy check and quiet-hours logic)
- Food-order nudge (pattern-based + inline meal-logging tip)
- Weekly single-focus sequencing (fiber → walks → sleep) to gate when Phase 1 nudges turn on
- Second onboarding path: "a doctor already told me" — captures their number and retest date instead of the risk test
- Pre-visit report screen tied to the retest date
- Learn section (short pages backing each nudge)

### Phase 2 — only once Phase 0 has real, sustained usage worth investing in
- Device-capability check + Gemini Nano integration (ML Kit GenAI: Prompt, Image Description, Speech Recognition) for flagship phones
- Fallback-tier photo/voice logging (basic ML Kit image labeling, platform speech-to-text) for everyone else
- Buddy chat screen

This is the single most engineering-expensive piece in the whole plan relative to how many users can even access the flagship path — it's deliberately last.

### Optional upgrade path (any phase, still free to you)
People who want deeper personalization can connect a fingerstick glucometer or an OTC CGM (Stelo/Lingo) via Health Connect to unlock a personal meal-to-glucose pattern view. Their hardware, not your cost.

### Deferred indefinitely, revisit only if this takes off
The peer/accountability circle — the one feature in this whole plan that requires a server, and therefore the one thing that isn't free past a certain scale.

---

## 2. What you're not building (any phase)

- The "CDC-recognized" label or its reporting obligations
- Streaks, badges, punishing gamification, shame-based notifications
- Forced accounts, forced data migrations, or auto-renewal billing of any kind
- Any hardware requirement to use the core app

---

## 3. Tech stack

- **Kotlin + Jetpack Compose**
- **Room (SQLite)** — 100% local storage
- **Health Connect Jetpack SDK** — read steps/sleep/weight/glucose, write logged data back
- **Phase 2 only:** ML Kit GenAI APIs (Gemini Nano via AICore) on flagship devices; ML Kit on-device Image Labeling + platform SpeechRecognizer as the fallback tier
- **Target API level 35 now** (36 required from Aug 31, 2026)
- **No backend, no cloud AI API, no hosting bill — in any phase**

### Data model (build incrementally — add tables as each phase needs them)
```
# Phase 0
Food
  id, name, servingLabel, carbsG, fiberG, proteinG
MealEntry
  id, foodId, servingsMultiplier, timestamp, source (text/manual)
WeightEntry
  id, weightKg, timestamp
UserGoal
  weightLossPercent (5-7 target), weeklyActivityMinutes (150 target, or activityGoalType: steps/minutes/other),
  fiberGramsTarget (14g/1000kcal)
PrediabetesRiskResult
  score, dateTaken, source (CDC/ADA Prediabetes Risk Test)
RulesEngineState
  daysSinceLastLog, weightTrendDirection, activityMinutesThisWeek, fiberGapToday, lastNudgeType

# Phase 1 additions
DoctorCheckIn
  labValue, dateTaken, nextRetestDate, source (doctor-reported / risk test)
RulesEngineState += currentFocusLever (fiber/walks/sleep), minutesSinceLastMeal

# Phase 2 additions
GlucoseReading
  id, mgdl, timestamp, source (Health Connect / manual)
MealEntry += source (photo/voice/text)
```

---

## 4. Build order (realistic, not "weekend one/two")

**Weekend 1 — Phase 0, part A**
1. Project setup, Room DB, food list bundled from USDA data
2. Onboarding: risk test, goal setting (including the non-step activity option), Health Connect connect
3. Meal/weight/activity logging (text + manual only)

**Weekend 2 — Phase 0, part B (this is the actual "ship it" milestone)**
4. Rules engine: fiber-gap nudges only
5. Progress screen
6. Privacy policy page (free to host on GitHub Pages), Play Store listing assets
7. Create Play Console account, get a build into closed testing — **treat these 12 testers as real user research, not a box to check.** Ask them directly what they logged, what they ignored, whether the fiber nudge ever felt useful.

**Later, once Phase 0 is live and you've actually heard from testers**
8. Phase 1 features, in the order listed in §1
9. Phase 2 features, only if Phase 0/1 usage justifies the engineering cost

---

## 5. Accessibility fix (small, do it in Phase 0)

Activity goals default to minutes/steps, which assumes typical mobility — a documented miss in at least one competitor. In the goal-setting screen, let the activity goal be one of a few types (steps/minutes, seated/adaptive movement, or a custom self-described goal) rather than hard-coding step count. One extra dropdown in onboarding, meaningful difference for who the app actually works for.

---

## 6. The local-first tradeoff (decide this on purpose)

Going fully local-first and zero-cost means no server, no aggregate usage data, no analytics dashboard — by design, and worth keeping. The real consequence: your only feedback loop is direct conversation with real people (the closed testers, later reviews), not usage metrics. That's a fine trade for trust and cost, but it's a decision, not an accident — don't expect to "check the dashboard" to know if this is working. Ask people.

---

## 7. Play Store submission reminders

- $25 one-time registration fee + identity verification (the only real cost in this whole plan)
- New personal accounts need a 12-tester, 14-consecutive-day closed test before production access — start the moment Phase 0 is installable
- Signed AAB, target API 35 (36 from Aug 31, 2026)
- Store listing, privacy policy URL, Data safety section, health apps declaration form for Health Connect data types used
- Content rating questionnaire

## 8. Language guardrails

- "Built on the approach used in the CDC's diabetes prevention program" — never "CDC-recognized" or "CDC-approved"
- "Helps you build toward the 5–7% weight loss and 150 minutes/week activity goals shown to reduce diabetes risk" — never "reduces your A1c" as a guaranteed outcome
- "Many people have insulin resistance without knowing it, since standard checkups don't always test for it" — never "this app detects your insulin resistance"
- "Fiber supports healthy blood sugar and gut health" — never "fiber reverses insulin resistance" or similar cure language
- Plain, visible disclaimer: not medical advice, not a substitute for your doctor
- "Everything stays on your phone — no account required, nothing sold" — say this directly in the listing
- Not legal advice — a lawyer's quick read is worth it before real users are involved

---

## 9. Definition of done for Phase 0 (what "shipped" actually means)

- [ ] Risk test onboarding + three goals set (weight, activity incl. non-step option, fiber)
- [ ] Health Connect connected, reading steps/sleep/weight
- [ ] Meal, weight, and activity logging works via text/manual entry
- [ ] Fiber-gap nudge fires correctly against real logged data
- [ ] Progress screen shows fiber, weight, and consistency trends
- [ ] Privacy policy live, Play Store listing complete, no forced accounts anywhere
- [ ] Build in closed testing with 12 real testers, and you've personally talked to at least a few of them about what they logged and ignored

Everything past this line is Phase 1 or 2. This is the line that means you have a real, live, honest product.
