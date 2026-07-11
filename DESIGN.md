# Design system — for implementation (Enough)

Built on Material 3 Expressive (Android's current design language, shipped 2025-2026). Use `androidx.compose.material3` — do not hand-roll custom theming where an M3 Expressive component already does the job.

## Color
- Generate the color scheme from a seed color using M3's dynamic color APIs (`dynamicColorScheme` / `rememberDynamicColorScheme` or `ColorScheme` derived via the Material Theme Builder from a seed).
- Seed color: a muted, warm green (growth/fiber association, not a literal leaf icon anywhere). Suggested seed hex: `#4E7A5C` — adjust in the Material Theme Builder until primary/secondary/tertiary tones feel warm rather than clinical.
- Respect dynamic color (Material You) when the device supports it — let the app tint toward the user's own wallpaper-derived palette rather than forcing the seed color always. This is a real "feels like it belongs on my phone" win, not just a nice-to-have.
- Success/positive states (fiber goal met, weight trend down) use the `success` role, not the primary color — keep primary for navigation/identity, not for "you did it" moments.
- Never use red/error color for a missed goal or low fiber day. Missed goals are neutral-toned, not red.

## Shape
- Use an expanded M3 `Shapes` scale — bigger corner radii than default M3: extraSmall 8dp, small 12dp, medium 16dp, large 24dp, extraLarge 32dp.
- Cards and containers default to `large` or `extraLarge`. Avoid sharp corners anywhere in the main flow.
- The mascot element (see below) uses an organic, asymmetric rounded shape — not a perfect circle, not a literal illustrated character.

## Typography
- Roboto Flex (variable font) via Compose's variable font support, or the default M3 type scale if Roboto Flex integration adds too much complexity for Phase 0 — don't block shipping on this.
- Numbers that update live (fiber grams, weight) get slightly heavier/larger weight than surrounding labels to draw the eye without needing color to do it.

## Motion
- Use Compose's spring-based animation specs (`spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)`) for the fiber progress ring filling, card entrances, and the buddy-chat send button — this is the "springy" M3 Expressive motion signature, not a custom animation curve.
- Keep motion under ~400ms. Springy does not mean slow.
- No motion on data the person didn't just interact with — don't animate the whole screen on every app open, only the specific thing that changed.

## The mascot
- A single abstract, organic blob shape (asymmetric rounded shape, not a circle, not an illustrated animal/character) rendered in the success color, used as a small living accent near greetings and nudges.
- It has exactly two states: still (default) and a gentle scale-pulse (when there's a new nudge worth noticing). No face, no eyes, no literal illustration — keep it abstract enough that it reads as "a bit of life" rather than a cartoon mascot that risks feeling twee or dating badly.

## What NOT to do
- No red/orange for missed goals or gaps — neutral gray, always.
- No confetti/celebration animations that could read as gamification (tension with the no-streaks, no-shame principle already in `SPEC.md`) — a subtle mascot pulse and a warm color shift are enough.
- No literal medical iconography (pill bottles, stethoscopes, clipboards) anywhere in the UI — this reinforces the "buddy, not clinic" positioning.
