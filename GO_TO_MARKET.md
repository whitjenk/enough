# Enough — Go-to-Market Plan: the CDC-DPP coach channel

*Product review, 2026-07-12. A distribution thesis with an explicit falsification test — not a marketing checklist. Companion to `SPEC.md` and the §7.5 resequence in `IMPLEMENTATION_PLAN.md`.*

---

## The bet, in one paragraph

Enough cannot use any modern consumer-app growth mechanic — no analytics, no paid acquisition with attribution, no referral loop, no lifecycle email — because it has no server by design. That looks like a fatal distribution problem. It isn't, **if** we stop trying to acquire users one at a time and instead acquire *cohorts* through people who already have a trusted, recurring channel to exactly our audience: **CDC-recognized Diabetes Prevention Program lifestyle coaches.** The property that kills our growth-marketing options — nothing leaves the phone, no account, free — is the single most valuable property a coach or clinic can find in a recommendable app, because it means **nothing to sign, no BAA, no security review, no data liability.** Our biggest weakness and our core constraint are the same fact, and that fact is a wedge in exactly one channel. Point the first 90 days of go-to-market there.

**What would falsify this bet:** after introducing the app to real DPP cohorts, fewer than 3 of the first ~8 coaches say they'd keep recommending it, or they can't articulate a concrete reason why. If coaches won't re-recommend it unprompted, the channel is not real and we should stop investing in it — see *Success criteria* below.

---

## Why this channel, specifically

**The audience is a bullseye, not an approximation.** The National DPP lifestyle-change program serves adults with prediabetes or high diabetes risk — *the exact population in the positioning statement*. These people have already self-selected into a year-long behavior-change program; they are pre-warmed, motivated, and retained by an existing structure. We are not manufacturing intent — we're reinforcing intent someone else already created.

**Our targets are their curriculum.** The program's behavioral goals are **150 minutes/week of activity** and **5–7% weight loss** — the exact numbers already in `UserGoal` and the language guardrails. Fiber-first daily action is a clean, safe, between-session reinforcement of the PreventT2 curriculum. We are almost purpose-built as supplemental engagement for a program that already exists.

**The unit of distribution is a cohort, not a user.** One coach introduces the app to a cohort of ~15–25 participants at intake. That's 1-to-many, zero-CAC, and the trust is transferred from a person the participant already meets weekly. Ten coaches ≈ 150–250 target users who arrive pre-trusted.

**The channel doubles as the PMF panel.** Coaches talk to participants every week. The qualitative feedback loop we otherwise lack (no analytics) *is* the coach relationship. **The distribution asset and the learning asset are the same asset** — which is why this is the highest-leverage move, not just one of several.

---

## Why the constraints stop being a tax here

| Enough's constraint | Growth-marketing cost | DPP-channel value |
|---|---|---|
| No server, no data leaves device | No analytics, no attribution | **No BAA, no PHI liability, no security review** — a coach can recommend it like a water bottle |
| Free, no account | No paywall, no email capture | **No procurement, no contract, no budget line** — a coach just says it at intake |
| No streaks, no shame, weight-optional | No addictive engagement hooks | **Matches DPP's supportive, non-judgmental pedagogy** and the §0.6 disordered-eating safeguards |
| Not "CDC-recognized," non-diagnostic | Weaker marketing claims | **Lowers the coach's/org's liability** in recommending it; it supplements their recognized program, never replaces it |

This is the crux: everything that makes Enough a hard consumer-growth product makes it an *easy recommend* inside a clinical trust channel.

---

## Motion: bottom-up through individual coaches (not top-down enterprise sales)

Do **not** start by selling to health systems or YMCA corporate. Enterprise procurement is slow and triggers exactly the security/data review our architecture is designed to make unnecessary — we'd be volunteering into the friction we just avoided. Instead:

1. **Go to coaches with recommendation autonomy first** — independent/community-based lifestyle coaches and ADCES-active coaches who routinely suggest supplemental tools without needing org sign-off. Sources: the ADCES community and its DPP/diabetes-prevention network, LinkedIn DPP-coach groups, local YMCA DPP coordinators, and the public CDC DPRP registry of recognized organizations.
2. **Offer a 20-minute call, not a pitch deck.** The ask is small: "would you hand this card to your next cohort at intake and tell me what they said?" A free, no-data, no-signup tool is a near-zero-effort yes for a coach who likes it.
3. **Land 5–10 coaches.** Each introduces it to their next cohort. This simultaneously satisfies the Play closed-test requirement (12 real testers) — the beachhead and the compliance milestone are the same work.
4. **Clinicians (primary care / endocrinology) are GTM phase two, not phase one.** Physicians reach the same population but are harder to access and more liability-cautious; the "prescription-pad" handout works there once the coach channel has proof and testimonials. Sequence coaches → clinicians, not both at once.

---

## What we need (assets, not app features)

None of these are engineering. They gate the motion, so they're the real week-1 work.

- [ ] **Coach one-pager:** what Enough is, what it explicitly is *not* (not CDC-recognized, not a diagnosis tool, not a replacement for the program), and the no-data story up top — "there is nothing for you or your org to sign." Uses the §8 language guardrails verbatim.
- [ ] **Participant intake card:** a printable half-page with a Play Store QR, one honest sentence of value, and the "everything stays on your phone" line. This is what the coach physically hands out.
- [ ] **Curriculum crosswalk:** a short table mapping Enough to PreventT2 (fiber → healthy eating module; activity minutes → the 150-min goal; optional 5–7% weight → the program's weight goal). Makes the coach's "why this fits my program" instant.
- [ ] **The privacy policy as proof** (already built, `docs/index.html`): the artifact that closes the "is my participants' data safe" question by being readable, not by being promised.
- [ ] **A coach feedback cadence:** a lightweight shared doc + one 15-min debrief per coach at ~week 4 and ~week 10. This is the analytics replacement — treat it as instrumentation, not a courtesy.

---

## Objection handling (the coach's real questions)

- **"Do we need a data agreement / is participant data safe?"** → Nothing leaves the phone; there's nothing to breach and no BAA to sign. *(Strongest card — lead with it.)*
- **"Is this CDC-recognized?"** → No, and we never claim it. It's built on the same framework and *supplements* your recognized program; it doesn't touch your recognition status or reporting.
- **"Does it cost anything / go through procurement?"** → Free, no account, nothing to sign.
- **"Does it replace what I do?"** → No — it reinforces between-session behavior, which is where programs lose people. You stay the program.
- **"How much work is it for me?"** → Hand out a card at intake. That's the entire ask.

---

## Success criteria (constraint-legal — we can't read a dashboard)

**Leading indicators (activity):** # coaches recruited, # cohorts introduced, # cards distributed.

**Proxy adoption:** aggregate Play Store install count and ratings/reviews — this is *store-level* data in Play Console, not in-app analytics, so it's fully consistent with the no-server rule. It is our only quantitative install signal; treat it as directional, not precise.

**The real signal is qualitative:** structured coach debriefs — did participants keep logging, what did they log vs. ignore, did the fiber nudge or the reset-day moment ever land, did anyone mention it unprompted in a session.

**The one scaled quantitative outcome read** is the already-spec'd optional anonymous outcomes ping (§21, "helped / didn't") at day 90 — small, but the only at-scale quantitative signal the architecture permits.

**90-day PMF threshold for the channel:** *not* an install number. It's **"do 3+ of the first ~8 coaches say they'd keep recommending it, and can they say why in a concrete sentence?"** If yes, the channel is real — scale coach recruitment and start the clinician phase. If no, the DPP channel is falsified as a primary vector and we reassess before pouring in more effort.

---

## Honest risks (what could kill this)

- **Org policy may gatekeep even free tools.** Some health systems prohibit any external-app recommendation regardless of data posture. *Mitigation:* start with autonomous/independent coaches; treat institutional programs as phase two.
- **Android-only excludes iPhone participants.** Real and unfixable in current scope. *Mitigation:* be upfront with coaches; target Android-heavier populations; accept partial cohort coverage. Do not fake an iOS promise.
- **No attribution.** We won't know which coach drove which install. *Mitigation:* this is the qualitative-loop tradeoff we already accepted — lean on debriefs, not tracking.
- **Liability of recommending a health app inside a clinical program.** The non-diagnostic, no-data, disclaimer-present design lowers it, but this channel makes a lawyer's read (already flagged in `SPEC.md` §8) more clearly worth doing *before* coach outreach, not after.
- **Ceiling.** This is a high-trust, hand-to-hand channel — it seeds PMF and credibility; it does **not** produce hockey-stick growth on its own. It is deliberately a beachhead. Compounding reach comes later from the front-door utility (SEO/organic) and opt-in milestone shares — but those only matter once this channel proves the product retains. Don't over-promise the channel as the whole growth story.

---

## First 90 days

| Weeks | Focus |
|---|---|
| 1–2 | Build the three coach-facing assets; finish the pending on-device manual tests; get the closed test installable. |
| 3–6 | Recruit 5–10 coaches (ADCES community, LinkedIn DPP groups, local YMCA coordinators, DPRP registry). Warm intros over cold. 20-min calls. |
| 6–10 | Coaches introduce it to their next cohort at intake → in front of ~150–250 target users. Doubles as the 12-tester closed test. |
| 10–13 | Structured debriefs with 3–4 coaches + a few participants. Feed §7.5 learnings back into product. Evaluate against the 90-day PMF threshold and decide: scale coaches + start clinicians, or reassess the channel. |

---

## The second channel: consumer counter-positioning (Option C, 2026-08-03)

This clinical channel is the **beachhead** — deliberately a high-trust, hand-to-hand, non-scaling vector that seeds PMF and doubles as the qualitative panel. It is **not** the whole growth story, and a later strategy review made the second channel explicit: a **consumer counter-position inside the "fibermaxing" moment** — *"the fiber app that won't make you crazy about it"* (full plan in `IMPLEMENTATION_PLAN.md` §7.6, positioning in `SPEC.md` §0.8).

The two channels are **sequenced, not parallel bets competing for week-1 attention:**

- **Clinical DPP = PMF/beachhead + learning loop.** Proves the product retains, with a built-in qualitative feedback panel. Runs first — this doc's first 90 days.
- **Consumer counter-position = scale lever, gated on retention.** The opt-in shareable card (§7.6 Step 2) is the zero-CAC acquisition surface; fiber's fast felt-feedback loop (§7.6 Step 1) is the retention it depends on. **Do not pour organic/social effort in until the retention hypothesis (H1) clears in the closed test** — a viral loop on a leaky bucket just accelerates churn.

**Proxy funnel for the consumer channel (constraint-legal):** Play Console **install count, rating, and review sentiment** — store-level data, not in-app analytics, so fully consistent with the no-server rule. Pair it with the on-device opt-in feedback summary (§7.5, extended in §7.6 Step 3) for the two signals that reveal whether Option C is landing: did people use the felt check-in, and did anyone share a card.

**Why this doesn't dilute the clinical thesis:** the counter-position is the *same* anti-shame, food-first, forgiving product the coaches are recommending — it's the consumer-facing name for the values §0.6 already mandates. One product, two doors. The clinical door opens first because it's where trust is cheapest to transfer and where the learning loop is richest; the consumer door opens wider, later, once H1 says the room is worth filling.

## How this moves the grade

In the two-axis read from the product review, this is the lever on the **distribution/PMF axis** (the one holding the app below its A− craft ceiling). It doesn't make distribution "solved" — it converts a *D, no-vector* into a *real, zero-CAC, constraint-aligned first channel with a built-in learning loop and an explicit kill test.* That's exactly what a pre-PMF product needs: not scale, but a credible, falsifiable path to fit. The A+ is earned when a handful of coaches re-recommend it unprompted — because at that point the constraint isn't the ceiling, it's the moat.
