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

## 0.6 Safety and inclusivity principles (launch-blocking, not optional)

- **The weight-loss goal must be optional, not a default everyone passes through.** A meaningful group of real users shouldn't have a numeric weight target at all: people at a normal weight who are still insulin resistant, people for whom a weight-loss goal is actively harmful (disordered eating history), and people who simply want fiber and activity without weight being the frame. Onboarding must offer "focus on fiber and activity only" as a real, equally-supported choice, not a buried option.
- **No calorie-deficit framing, ever, anywhere** — no "calories remaining today," no countdown language. This was already true by design (fiber-first, not calorie-first) — this principle makes it an explicit rule rather than an incidental side effect.
- **A quiet, non-judgmental support resource mention belongs in Settings or Learn** — not intrusive, not a popup, just present for anyone who needs it. This app is not a clinical screening tool and shouldn't attempt to detect or diagnose disordered eating; the responsible move at this scale is removing known risk patterns (weight-loss defaults, calorie framing, streaks) and making support easy to find, not building a detection system.
- **Dietary restrictions and allergies must be captured in onboarding and respected in every food suggestion.** A specific food-swap nudge that ignores an allergy or a vegan/vegetarian/gluten-free preference is worse than no suggestion at all — it breaks the "this buddy actually knows me" trust the whole design depends on.
- **This app explicitly welcomes people on GLP-1 medications — it does not position against them.** Earlier positioning framed this app in contrast to GLP-1 companion apps, but a real, sizable share of the actual audience is either currently on one or transitioning off one, and silence on this isn't neutral — it means the app behaves identically for someone with GLP-1-related appetite suppression and GI sensitivity as for someone without, which is a mismatch either way. Concretely: if `UserGoal.takesGLP1Medication` is true, soften fiber-increase suggestions (smaller increments, awareness that common GI side effects make a sudden large fiber jump uncomfortable rather than helpful) and never frame the fiber target as something to hit despite reduced appetite. This is a tone and suggestion-intensity adjustment, not medical guidance — the app still isn't offering medication-specific advice.

---

## 0.7 Why "AI-first" means architecture, not a feature list

The differentiation here isn't "this app has AI and competitors don't" — they're adding AI too. The real advantage is having no legacy architecture or business model to protect while doing it:

- **On-device AI is the default architecture, not a premium bolt-on** — this is *why* zero-cost is structurally true here and hard for a cloud-infrastructure incumbent to cheaply replicate, not just a promise
- **Photo/voice logging is the primary path, not a secondary button next to a decades-old manual-search UI** — no legacy UX to maintain in parallel
- **The data model is designed for future personalization/correlation from day one**, not retrofitted onto a schema built for simple calorie summation
- **One photo can map to multiple logged foods natively**, since there's no existing "one search, one selection" assumption to preserve

**The equally important other half: AI-first means restraint, not maximum AI usage.** Deterministic logic (fiber-gap math, trend calculations, consistency counts) stays deterministic — running it through a model would be slower, costlier, and less trustworthy, not more impressive. The rules-engine-first, AI-only-for-open-ended-conversation architecture already in this spec *is* the AI-first decision. A real anti-pattern to avoid at every phase: don't reach for an LLM call to solve something arithmetic already solves better.

**Upgrade path worth being deliberate about:** Phase 0/1 nudge phrasing is an honest template ("you're at Xg — [swap] adds about Yg") — reliable and testable, but still templated. Once Phase 2's on-device model exists, the phrasing (not the underlying math) is a good candidate to become genuinely generated and context-specific, so it stops reading as a fill-in-the-blank the longer someone uses the app.

## 1. Phased scope

### Phase 0 — ship this first (genuinely weekend-sized)
The smallest thing that is honestly a complete, useful product on its own:

| Screen | Purpose |
|---|--------|
| Onboarding | Leads with a default path — **"just here to build better habits"** — straight to goal-setting, no quiz. The real CDC/ADA Prediabetes Risk Test is offered as an optional second choice ("curious about your risk factors?"), never a gate. Either way: connect Health Connect, set a weekly activity target (with a non-step-based option — see §5) and a fiber target (14g/1,000 kcal); **weight-loss goal is an equally-presented optional choice** ("include a weight goal, or focus on fiber and activity only?"); capture dietary restrictions/allergies (vegetarian, vegan, gluten-free, dairy-free, nut allergy, other) so food suggestions never violate them; one optional free-text question, **"what's making you want to do this?"** (`UserGoal.personalWhy`) — captured now even though it's only referenced starting Phase 2's buddy chat, since it's cheap to ask once and expensive to retrofit later; one optional yes/no question, **"are you currently taking a GLP-1 medication (Ozempic, Wegovy, Zepbound, or similar)?"** — resolves an explicit stance decision, see §0.6 |
| Today | Logged meals, weight, synced steps/sleep; the day's fiber-gap nudge |
| Add meal / activity | Fast text search against the local food list; manual weight/activity entry |
| Progress | Fiber gap vs. target (headline metric), weight trend, streak-free consistency view |
| Settings | Health Connect sync, estimate calibration (low/balanced/high for fiber and portion estimates), delete my data |

**No AI, no device tiering, no buddy chat, no walk/food-order nudges yet.** Just: set targets, log fast, get one honest fiber nudge a day, see the trend. This alone is a real, shippable, useful app.

**Why the risk test is optional, not the front door:** the whole point of this app, down to the name, is that it has value regardless of anyone's diabetes risk — it's preventative and general, not a diagnosis tool. Making a diabetes screening quiz the mandatory first thing every user sees contradicted that positioning. The test itself didn't change (still the real, validated CDC/ADA version, family-history question included) — it just stopped being a gate.

### Phase 1 — once Phase 0 is live and you've heard from real testers

**Top priority, do these two first — one closes a brand-promise gap, the other closes an internal inconsistency in what's already spec'd:**

- **The "Enough" moment — a real reset-day feature, not just an underlying design principle.** Every part of this spec avoids catch-up math and guilt by design, but there's currently no single concrete feature that makes the app's own name tangible. Add a specific, nameable card/screen: triggered either after 3+ days without logging, after a day where the fiber target was missed by a wide margin, or when the person tells the buddy (Phase 2) they had a rough day — the app shows a warm, simple message ("today's a fresh start — no need to make up for anything") with zero catch-up framing, no "you're behind" language, and an easy, optional "log something small if you want" — never required. This is the single highest-leverage gap relative to how much thought went into the name itself.
- **Fill in sleep and stress for real — they're currently labels, not features.** The weekly sequencing lists "fiber → walks → sleep," but sleep has no actual nudge design, and stress — one of the original evidence-based pillars alongside fiber and walking — quietly disappeared from the sequencing entirely somewhere along the way. Fix both:
  - **Sleep nudge:** pull duration from Health Connect (already read from Phase 0); when sleep is notably short or bedtime notably inconsistent, one gentle, non-judgmental nudge — same design pattern as the walk/food-order nudges, not a new architecture.
  - **Stress:** there's no reliable automatic signal for this (Health Connect doesn't have a real stress metric), so don't overbuild an automated detection system for something that can't be sensed honestly. Instead: a simple, optional, one-tap daily check-in (not required), stress content covered honestly in the Learn section, and stress as a topic the buddy (Phase 2) can actually talk about — the lighter-weight, more honest choice given the limits of what's actually detectable here.
  - Update the sequencing to the real four-lever version: fiber → walks → sleep → stress.

**Rules engine architecture — do this alongside the top-priority items above, since it becomes a real coordination problem the moment more than one nudge type exists:**

- **Arbitration layer.** By this point in the spec there are 8+ message/nudge types (fiber gap, walk, food-order, sleep, stress check-in, the reset-day moment, milestone views, checkpoint nudges, absence check-ins) with nothing deciding what happens when more than one is eligible the same day. Left unresolved, this is a near-certain collision — e.g. a walk nudge firing the same day as the reset-day moment would directly contradict it. Fix: an explicit priority order (the reset-day moment always wins over routine nudges) and a hard rule of one proactive message per day, period.
- **Cross-lever pattern insight, built as deterministic correlation, not gated behind AI.** Simple statistics over already-logged history (does hitting the fiber target correlate with an earlier bedtime, more steps, etc.) belongs in the rules engine itself, available to every user regardless of device tier — see the correction above.
- **Adaptive target calibration.** Targets are currently set once at onboarding and never revisited. Detect sustained over- or under-performance (e.g. 3+ weeks comfortably clearing the fiber target, or 3+ weeks consistently well short of it) and *offer*, never auto-apply, a target adjustment. This applies the "Enough" philosophy to the goals themselves, not just the tone around them.
- **More frequent, honest recognition.** Right now positive acknowledgment only happens at the 90/180/365-day milestones — a long gap with no signal in between. Recognize real sustained patterns (e.g. 3 straight weeks hitting the fiber target) when they happen, off history that's already being tracked.
- **Minimum sample size guardrail, wrapping around all of the above.** Any pattern callout, correlation, or target-adjustment suggestion needs a minimum number of data points before it's allowed to surface at all — otherwise this becomes false-pattern claims dressed up as insight, exactly the overclaiming this app is built to avoid. Set an explicit threshold (e.g. don't surface a correlation claim from fewer than ~2 weeks of relevant data) rather than leaving this to judgment call by call.

**The rest of Phase 1:**
- Post-meal walk nudge (with its Health Connect redundancy check and quiet-hours logic)
- Food-order nudge (pattern-based + inline meal-logging tip)
- Third onboarding path: "a doctor already told me" — captures their number and retest date instead of the risk test
- Pre-visit report screen tied to the retest date
- Learn section (short pages backing each nudge, including the sleep and stress content above)
- **Nudge-to-action.** Every specific food-swap suggestion gets a simple "add to list" tap, appending to a basic local grocery list — otherwise a good suggestion has nowhere to go and likely dies the moment it's read. Small, cheap, real behavior-change multiplier.
- **Eating-out quick-log.** Real meals often happen at restaurants or someone else's table, where matching to a precise database food doesn't work. Add a coarser entry path — a rough "how veggie/protein/carb-heavy was this" categorical log instead of an exact match — and treat it as inherently an estimate, respecting the `estimateCalibration` setting even more explicitly than a database-matched entry does. Pair this with a short, **evergreen** list of on-the-go fiber boosts shown as a tip alongside this entry type: ask for extra beans/legumes (free at most build-your-own bowl places), choose brown rice over white when offered, a veggie-loaded sandwich over meat-only, oatmeal over a pastry, avocado/guacamole as an add-on, fruit over fries as a side, a side salad added or swapped in. **Keep this list conceptual, not chain-specific with hardcoded gram counts** — fast food nutrition data drifts as menus change, and a stale specific number (e.g. "13g of fiber at X") is a real credibility risk the moment a chain reformulates. If specific chain examples are included at all, frame them as loose illustrations, not permanent guaranteed facts.
- **90-day "how far you've come" milestone view** — available to every onboarding path, not just the doctor-told one. Once 90 days have passed since onboarding, surfaces real trend data (weight change, % of days fiber target was hit, activity consistency) as a genuine accomplishment, not a manufactured streak or a clinical printout. This is the retention answer for the majority of users who don't have a doctor-given retest date to anchor a longer-horizon "did this actually matter" moment.
  - **The retention arc doesn't stop at day 90.** After the milestone, introduce the next focus lever (past fiber → walks → sleep, consider what a second cycle looks like — perhaps deepening the same three with higher targets, or introducing a new one like sleep consistency in earnest). The milestone view itself repeats at 180 and 365 days with a longer view each time, so there's always a next chapter rather than a single peak followed by silence.
  - **Shareable milestone card, fully opt-in.** Let someone export the 90/180/365-day milestone as a simple, genuinely nice image if they want to — never suggested, never prompted, no share sheet popping up uninvited. This is the one place organic reach can come from something already built, without bolting on a separate viral mechanic that would fight the app's no-pressure personality.
- **Optional 90-day real-world checkpoint nudge** — extends the `DoctorCheckIn` concept to every path, not just doctor-told. A single, gentle, dismissible suggestion around the 90-day mark: "it's been 90 days — want to check in with a doctor or an at-home test to see where things actually stand?" Never repeats more than once per ~90-day window, and "not right now" is a fully valid, easy response — see the buddy voice guidelines in `CLAUDE.md`.
- **Optional anonymous outcomes ping — a deliberate, narrow exception to the "no server" rule, not a reversal of it.** Local-first, zero-analytics architecture means there's no way to ever learn at scale whether this app actually helps anyone. The fix, held to strict limits: at the 90-day milestone, offer a fully optional one-tap prompt — "would you be willing to anonymously share whether this helped, so it can improve for others?" If accepted, the *only* thing that ever gets sent is a single incremented counter (helped / didn't / prefer not to say) to a minimal aggregate-only endpoint. No user ID, no device ID, no timestamp, no other field, ever. This is the one and only network call the app makes anywhere in this spec — adding a second one, of any kind, deserves the same level of scrutiny as this one got.

### Phase 2 — only once Phase 0 has real, sustained usage worth investing in
- Device-capability check + Gemini Nano integration (ML Kit GenAI: Prompt, Image Description, Speech Recognition) for flagship phones
- Fallback-tier photo/voice logging (basic ML Kit image labeling, platform speech-to-text) for everyone else
- Buddy chat screen — **must have an explicit safety design before shipping, not an assumption that warmth covers it.** If someone expresses distress, disordered eating thoughts, or a desire for extreme restriction, the buddy must never diagnose, lecture, or encourage restriction — it should respond with care and point toward real support resources, following the same principles in §0.6
  - **This is what separates a real accountability partner from a warm-sounding chatbot: memory and follow-through, not just tone.** Five concrete mechanisms, not just a personality:
    1. **Capture "your why" once, in onboarding** (free text, one line: "what's making you want to do this?"). Reference it sparingly — meaningful moments only (a low point, a milestone), never as daily filler, or it cheapens fast.
    2. **Context assembly, not model memory.** Gemini Nano is stateless per conversation — every buddy chat invocation must be fed real assembled context (recent logs, current consistency stats, active focus lever, stated why, anything notable the person mentioned) rather than relying on the model to "remember." This is the actual engineering backbone the other four mechanisms depend on.
    3. **Real pattern callouts from the person's own data** ("the last three times you hit your fiber goal, you'd also gone to bed before 11") — only possible because it's genuinely their data, not a template. **Correction from an earlier version of this spec: the correlation itself is arithmetic over already-logged data, not something that needs a model — it belongs in the deterministic Phase 1 rules engine (see the rules engine architecture section below), not gated behind Phase 2's flagship-only AI layer. Otherwise only flagship-device users would ever get real personalized insight, which is an equity problem, not just a technical one. Phase 2's AI adds more natural phrasing on top of this later; it doesn't create the insight.**
    4. **A distinct absence check-in, separate from the daily fiber nudge.** Tied to `daysSinceLastLog`: after several quiet days, one message — "haven't heard from you in a few days, everything okay? no pressure" — then it stops. Doesn't repeat daily like the fiber nudge does; this is about the person, not the metric.
    5. **Honest mirroring, not just validation.** When someone expresses low motivation, the buddy can gently reflect their own stated why back ("totally fine — just flagging you mentioned wanting X, your call") without guilt or pressure. Respects autonomy while still being honest, which is what separates a friend from a yes-man.

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
- **No backend, no cloud AI API, no hosting bill — in any phase, with one narrow, explicit exception:** the optional anonymous outcomes ping in the Phase 1 addendum, which sends a single aggregate counter increment and nothing else. That is the only network call anywhere in this app — see §1's Phase 1 list for the strict limits on it.

### Data model (build incrementally — add tables as each phase needs them)
```
# Phase 0
Food
  id, name, servingLabel, carbsG, fiberG, proteinG
MealEntry
  id, foodId, servingsMultiplier, timestamp, source (text/manual), logGroupId (nullable — links multiple MealEntry rows logged from a single action, e.g. one photo of a plate with several foods; null for Phase 0's one-food-per-log text entries, used starting Phase 2)
WeightEntry
  id, weightKg, timestamp
UserGoal
  weightLossPercent (nullable — optional, 5-7 target when set), weeklyActivityMinutes (150 target, or activityGoalType: steps/minutes/other),
  fiberGramsTarget (14g/1000kcal), estimateCalibration (low/balanced/high — default balanced; applies a roughly ±15% adjustment to fiber/portion estimates wherever the app is guessing rather than looking up an exact value),
  dietaryRestrictions (vegetarian/vegan/gluten-free/dairy-free/nut-allergy/other, multi-select),
  personalWhy (free text, optional, captured once at onboarding — only referenced starting Phase 2's buddy chat, but cheap to capture now and expensive to retrofit later),
  takesGLP1Medication (nullable bool, optional — softens fiber-increase suggestion intensity when true, see §0.6)
PrediabetesRiskResult
  score, dateTaken, source (CDC/ADA Prediabetes Risk Test)
RulesEngineState
  daysSinceLastLog, weightTrendDirection, activityMinutesThisWeek, fiberGapToday, lastNudgeType

# Phase 1 additions
DoctorCheckIn
  labValue, dateTaken, nextRetestDate, source (doctor-reported / risk test / self-scheduled 90-day checkpoint)
RulesEngineState += currentFocusLever (fiber/walks/sleep/stress), minutesSinceLastMeal, onboardingCompletedDate, hasSeenMilestoneReview (bool), lastCheckpointNudgeDate, lastMilestoneIntervalShown (90/180/365), hasRespondedToOutcomesPing (bool), lastResetMomentShown (date), daysSinceLastLogAtLastCheck, lastProactiveMessageDate (enforces one-per-day arbitration), lastProactiveMessageType, consecutiveWeeksOverTarget, consecutiveWeeksUnderTarget
StressCheckIn
  id, timestamp, selfReportedLevel (simple 1-5 tap, optional, not required)
GroceryListItem
  id, foodId, addedFromNudge (bool), timestamp, checkedOff (bool)
MealEntry += entryType (database-matched / eating-out-estimate) — eating-out entries are coarser, category-based (roughly veggie/protein/carb-heavy) rather than an exact food match, and respect estimateCalibration more explicitly

# Phase 2 additions
GlucoseReading
  id, mgdl, timestamp, source (Health Connect / manual)
MealEntry += source (photo/voice/text) — photo logging populates logGroupId to link multiple foods from one plate
RulesEngineState += lastAbsenceCheckInDate (separate from lastCheckpointNudgeDate — this is relationship-based, not milestone-based)
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

## 9. Known limitations (not blocking launch, worth tracking)

- Food database is US-only (USDA FoodData Central) — no localization yet
- No re-engagement design for someone who stops logging for weeks
- No feedback mechanism beyond the initial 12 closed testers once Phase 0 ships wider — the zero-analytics tradeoff solves the "before launch" problem, not the "six months in" problem
- Local data has no backup/export yet — real risk given the 90-day milestone feature depends on data continuity; prioritize an export/import feature (Phase 1, alongside the milestone view) sooner rather than later

## 10. Definition of done for Phase 0 (what "shipped" actually means)

- [ ] Onboarding complete: entry choice, activity and fiber goals set, weight goal offered as a real optional choice (not default), dietary restrictions captured
- [ ] Health Connect connected, reading steps/sleep/weight
- [ ] Meal, weight, and activity logging works via text/manual entry
- [ ] Fiber-gap nudge fires correctly against real logged data, and food-swap suggestions respect logged dietary restrictions
- [ ] Progress screen shows fiber, activity, and consistency trends (weight trend shown only if a weight goal was set)
- [ ] Privacy policy live, Play Store listing complete, no forced accounts anywhere
- [ ] A quiet, non-judgmental support resource is reachable from Settings or Learn
- [ ] Build in closed testing with 12 real testers, and you've personally talked to at least a few of them about what they logged and ignored

Everything past this line is Phase 1 or 2. This is the line that means you have a real, live, honest product.
