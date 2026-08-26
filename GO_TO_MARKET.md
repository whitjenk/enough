# Enough — how this actually reaches people

*Rewritten 2026-08-24. Replaces the DPP-coach channel plan of 2026-07-12, which was retired
unexecuted. Companion to `SPEC.md` and `IMPLEMENTATION_PLAN.md`.*

---

## What success is

**A handful of real people use this and it genuinely helps some of them.**

Not a user count. Not a channel. Not a scale story. If ten people use it for three months and
two of them eat more fiber and feel better, this project succeeded. That is the whole bar, and
it is deliberately, honestly small.

This is stated first because every previous version of this document optimized for reach, and
reach was never the goal.

## What actually reaches people

Three things, in order of how much they matter:

1. **Ten people you already know.** Text them, hand them a link, watch them use it. This is the
   real distribution and the real research at the same time — you can ask them what they logged,
   what they ignored, and why they stopped, which no amount of instrumentation would tell you.
2. **Soft sharing with friends as it comes up.** Mentioning it, showing it, sending the link when
   someone asks what you've been building. Not a campaign, not a cadence — just not hiding it.
3. **The Play Store listing, working passively.** Someone searches "fiber," finds it, installs it.
   Low volume, zero effort, runs while you sleep.

That's it. There is no fourth thing, and adding one is not on the roadmap.

## The Play listing is the entire passive funnel — treat it that way

With no promotion motion, the listing isn't marketing collateral; it *is* the top of funnel.
Store search, the icon, the first two lines of the short description, and the screenshots are
the only things doing acquisition work at all. That makes the listing copy in `docs/play-listing.md`
disproportionately important relative to how small it looks — and makes the searchable word in
the app name ("fiber") a real decision, not packaging.

The counter-position — *"the fiber app that won't make you crazy about it"* — survives here, but
its job has changed. It is no longer a growth play riding a trend. It is (a) the product's actual
soul, per `SPEC.md` §0.6, and (b) the thing that makes the listing distinct to someone who is
already searching. Both of those are true regardless of what any trend does next.

## The one thing worth measuring

**H1 — does surfacing fiber's same-day felt effects make people come back, and does the app
genuinely help?**

Read it by talking to the ten people. Not through the on-device feedback summary, not through
Play Console numbers — through conversation. Aggregate signals were designed as an analytics
substitute for strangers at scale; with a handful of known people, asking is strictly better.

H2 (acquisition via the share card), H3 (GLP-1 segment behavior), and H4 (message A/B) are
retired. All three required a funnel to read, and there is no funnel. The features they were
attached to stay — the share card is still a nice thing to hand someone, the GLP-1 stance is
still the right tone for a real slice of the audience — they just stop carrying hypotheses.

## The stranger problem (why re-entry is in scope)

People who find the app through store search are the one group you cannot talk to. If one of them
installs it, likes it, and then forgets it exists, the app dies on their phone silently and you
never learn a thing. They have no other path back.

That is the argument for building a local, on-device re-entry mechanism — a gentle daily
notification and/or a home-screen widget — and it holds even though the ten known people don't
need one. It stays subject to every constraint in `CLAUDE.md`: no server, nothing leaves the
device, never escalating, never guilt-based, genuinely dismissible. See `IMPLEMENTATION_PLAN.md` §7.8.

## What was retired, and why it's recorded here

The previous plan bet distribution on **CDC-recognized DPP lifestyle coaches** — a cohort-level,
zero-CAC channel whose economics genuinely fit this app's constraints (no BAA, no procurement,
no security review, because there is no server). The analysis was sound. It was retired for one
reason: **it required sustained cold outreach — forums, LinkedIn groups, 20-minute calls, week-4
and week-10 debriefs — that the person building this is not going to do.**

A channel nobody will work is worth zero no matter how well it maps to the audience. Recording
that plainly is more useful than leaving a 90-day campaign plan in the repo as a standing debt.

**What would reopen it:** a coach or clinician approaching *you*, or the ten-person round showing
something strong enough to make outreach feel worth it. Both are pull, not push. The reasoning
above is preserved for that case — it doesn't need to be re-derived.

## The actual risk

Not competition, not trend timing, not distribution. This project has no server, no runway, and
no burn rate, so it cannot die of any of those.

**It can only die if you lose interest.** That argues for small, fast, high-contact work with
real people in it — and against long build stretches with no one on the other end. If a task
doesn't either help someone you can talk to or make the app better for the person who finds it
by accident, it is probably not the next task.
