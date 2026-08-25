# Play Store listing copy — Enough

Draft copy for the Google Play Console listing. Written against the language guardrails in
`SPEC.md` §8 and the tone rules in `CLAUDE.md`. Every claim here is deliberately behavioral,
never clinical — no "CDC-recognized/approved," no A1c-outcome promise, no "detects insulin
resistance," no "reverses" language, no comparison to other people, no streak framing.

Character limits are Play's: app name ≤ 30, short description ≤ 80, full description ≤ 4000.

---

## App name (≤ 30 chars)

```
Enough: fiber, gently
```
(21 chars. The tail carries the counter-position — the calm, un-anxious fiber app —
and makes the otherwise un-searchable name discoverable. Fallback: `Enough` — 6 chars.)

---

## Short description (≤ 80 chars)

```
The fiber app that won't make you crazy about it. No account, all on your phone.
```
(79 chars. Leads with the counter-position inside the "fibermaxing" moment — SPEC §0.8.)

---

## Full description (≤ 4000 chars)

```
Fiber is having a moment — and most of it is exhausting. Hit 40g, chase the perfect gut, never miss a day. Enough is the opposite of that. It helps you improve weight and metabolic health with one small, specific action a day, starting with fiber instead of calories, at a pace that won't make you crazy.

Most healthy-eating apps hand you a calorie budget and a countdown. Enough doesn't. It's built on the approach used in the CDC's diabetes prevention program, and it turns "eat healthier and move more" into a single doable thing you can act on today.

WHY FIBER
Most adults get about half the fiber that's generally recommended. Fiber supports healthy blood sugar and gut health, and it helps you feel full — often the same day. It's one of the gentlest changes you can make: no cutting, no counting calories, no off-limits foods. Enough gives you a daily fiber target (based on the widely used 14g per 1,000 calories guideline) and one easy suggestion to get a little closer.

NOTICE HOW YOU FEEL
Fiber pays off fast. A quick, optional daily check-in lets you note how the day felt — steady, full, energized — so you get something real today, not just a number to chase for months.

BUILT TO BE EASY, NOT DEMANDING
- Log a meal in one tap with rough categories, or search an exact food when you want to.
- Get one specific fiber swap a day — useful even on days you don't log anything.
- See your progress as a calm trend, not a punishing streak. Missing a day never erases what you've done.
- The weight-loss goal is completely optional. You can focus on fiber and activity only, and that's a real, fully supported choice — not a buried setting.

RESPECTS HOW YOU EAT
Tell Enough about dietary restrictions or allergies once — vegetarian, vegan, gluten-free, dairy-free, nut allergy, or your own — and food suggestions respect them from then on. If you're on a GLP-1 or coming off one, you can say so in Settings and fiber tips stay gentle and appetite-aware.

NOTHING TO HIDE
Everything stays on your phone. No account required, no login, no subscription, nothing sold, no ads. There is no server — the app can't send your data anywhere, and you can verify that. If you connect Health Connect, Enough reads steps, sleep, and weight to show alongside your logs; that access is read-only and you can turn it off anytime. "Delete my data" actually wipes everything on your device.

A KINDER APPROACH
Enough is named for what it believes: one small action a day is enough. You don't need to be fixed to start, and a missed day is just a day. There are no streaks to break, no shame, no guilt, and no comparison to anyone else — ever.

Enough is free. The app provides general wellness information and is not medical advice or a substitute for your doctor. It doesn't diagnose any condition or measure clinical values like A1c. If you have questions about your health, talk with a healthcare professional.
```

---

**Revised 2026-08-24:** the standalone "FOR THE GLP-1 ERA" section was cut to a single line inside
"Respects how you eat." It was a billboard for hypothesis H3, which is retired (`IMPLEMENTATION_PLAN.md`
§7.6), and it made a medication angle the third thing a stranger read about a free app. The capability
is unchanged — it just stopped leading.

## Notes for whoever finalizes the Play Console listing

- **Data safety form** is easy here and should reflect the real architecture: no data collected,
  no data shared, no data leaves the device, no account. Health Connect reads are on-device only.
- **Category:** Health & Fitness. **Content rating:** Everyone (complete the questionnaire honestly —
  no user-generated content, no data sharing).
- **Privacy policy URL:** the GitHub Pages page at `docs/index.html` (must be enabled + the
  placeholder contact email replaced first — see IMPLEMENTATION_PLAN §8).
- **Graphics still needed (not copy):** 512×512 app icon, feature graphic (1024×500), and at least
  2–8 phone screenshots. Keep any on-screenshot text inside the same §8 guardrails.
- A lawyer's quick read before real testers is worth it (SPEC §8).

---

## First-run voice (in-app)

The counter-position now lives in the very first screen: the onboarding welcome reads
*"…at your own pace. No streaks, no pressure to be perfect."* (`onboarding_welcome_body`). Keep any
future first-run copy in this register — calm and forgiving, never optimizing — so the promise the
store makes is the first thing the app keeps.

---

## Organic-social creative concepts (the shareable card is the hero)

These ride the "fibermaxing" conversation by counter-positioning inside it (SPEC §0.8). The in-app
opt-in share card (§7.6 Step 2) is the hero asset — it's the zero-CAC surface, so the creative points
at it. All three stay inside the §8 guardrails: no clinical claims, no comparison, no shame.

1. **"The calm one."** Split screen: a frantic fibermaxing checklist (40g! every day! don't break the
   streak!) vs. the Enough card — one gentle line, "small and steady, that's enough." Caption: *the
   fiber app that won't make you crazy about it.*
2. **"Notice how you feel."** Short clip of the daily felt check-in → the week's share card. Copy leans
   on fiber's same-day payoff (fuller, steadier), not a number. Caption: *fiber, but for how you
   actually feel.*
3. **"Coming off the shot."** Speaks to the GLP-1-transition segment: fiber as the satiety bridge as
   appetite returns. Card shows the gentle, no-number-to-hit swap. Caption: *the food-first fiber
   companion for the GLP-1 era — including coming off it.* (Tone/support only; never medication advice.)

Hero asset = the user's own generated card, so the creative is literally the product's viral loop, not
a mock. Do not fabricate testimonials or numbers.
