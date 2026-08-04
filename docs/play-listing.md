# Play Store listing copy — Enough

Draft copy for the Google Play Console listing. Written against the language guardrails in
`SPEC.md` §8 and the tone rules in `CLAUDE.md`. Every claim here is deliberately behavioral,
never clinical — no "CDC-recognized/approved," no A1c-outcome promise, no "detects insulin
resistance," no "reverses" language, no comparison to other people, no streak framing.

Character limits are Play's: app name ≤ 30, short description ≤ 80, full description ≤ 4000.

---

## App name (≤ 30 chars)

```
Enough: fiber-first health
```
(26 chars. Fallback if a shorter name is wanted: `Enough` — 6 chars.)

---

## Short description (≤ 80 chars)

```
One small fiber goal a day. No account, no calorie counting, all on your phone.
```
(78 chars.)

---

## Full description (≤ 4000 chars)

```
Enough helps you improve weight and metabolic health with one small, specific action a day — starting with fiber instead of calories.

Most healthy-eating apps hand you a calorie budget and a countdown. Enough doesn't. It's built on the approach used in the CDC's diabetes prevention program, and it turns "eat healthier and move more" into a single doable thing you can act on today.

WHY FIBER
Most adults get about half the fiber that's generally recommended. Fiber supports healthy blood sugar and gut health, helps you feel full, and is one of the gentlest changes you can make — no cutting, no counting calories, no off-limits foods. Enough gives you a daily fiber target (based on the widely used 14g per 1,000 calories guideline) and one easy suggestion to get a little closer.

BUILT TO BE EASY, NOT DEMANDING
- Log a meal in one tap with rough categories, or search an exact food when you want to.
- Get one specific fiber swap a day — useful even on days you don't log anything.
- See your progress as a calm trend, not a punishing streak. Missing a day never erases what you've done.
- The weight-loss goal is completely optional. You can focus on fiber and activity only, and that's a real, fully supported choice — not a buried setting.

RESPECTS HOW YOU EAT
Tell Enough about dietary restrictions or allergies once — vegetarian, vegan, gluten-free, dairy-free, nut allergy, or your own — and food suggestions respect them from then on. If you're on a GLP-1 medication, suggestions stay gentle and small rather than pushing you to hit a number despite reduced appetite.

NOTHING TO HIDE
Everything stays on your phone. No account required, no login, no subscription, nothing sold, no ads. There is no server — the app can't send your data anywhere, and you can verify that. If you connect Health Connect, Enough reads steps, sleep, and weight to show alongside your logs; that access is read-only and you can turn it off anytime. "Delete my data" actually wipes everything on your device.

A KINDER APPROACH
Enough is named for what it believes: one small action a day is enough. You don't need to be fixed to start, and a missed day is just a day. There are no streaks to break, no shame, no guilt, and no comparison to anyone else — ever.

Enough is free. The app provides general wellness information and is not medical advice or a substitute for your doctor. It doesn't diagnose any condition or measure clinical values like A1c. If you have questions about your health, talk with a healthcare professional.
```

---

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
```
