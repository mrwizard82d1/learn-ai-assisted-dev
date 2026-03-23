# Reframe Bowling Tutorial Notes

_Check available skills_

This is a substantial tutorial — let me craft it carefully as a well-structured markdown document.

_Created a file, read a file_

Here's your complete tutorial. Let me walk you through what it covers and why it's structured the way it is.

### What the tutorial does, section by section

**Sections 1–2** get your mental model right before any code. Re-frame's unidirectional data flow is the hardest conceptual hurdle, so the tutorial draws the loop explicitly and maps each concept to a bowling analogy upfront.

**Sections 3–6** scaffold the project step by step:

- `deps-new` generates a `deps.edn`-based project (the modern Clojure standard)
- `deps.edn` is updated to pull in `shadow-cljs`, `re-frame`, and `reagent`
- `package.json` adds React and the shadow-cljs npm package
- `shadow-cljs.edn` wires the build: `:deps true` tells it to defer to `deps.edn` (no duplication), and `:devtools` sets up the hot-reload server at port 8080

**Section 7** is the app bootstrap — notably it uses `dispatch-sync` (not `dispatch`) at startup to guarantee the db is initialized before the first render.

**Sections 9–11** introduce each re-frame building block in the correct order — db → events → subscriptions — with the bowling domain driving every example. The subscription section explicitly teaches the layer 2 / layer 3 pattern, which most tutorials skip but which becomes essential as apps grow.

**Section 12** is pure `logic.cljs` — no re-frame at all. Keeping domain logic in a separate, framework-free namespace makes it trivially unit-testable and is a habit worth building early.

**Section 13** covers views with emphasis on the two patterns you'll use constantly: `@(rf/subscribe [...])` for reading and `rf/dispatch` in `:on-click` for writing.

**Section 15** covers dev workflow, hot reload, production builds, and the three canonical unit tests (perfect game, gutter game, all-spares).
Let me know if you'd like me to expand any section — for example, adding an HTTP effects example with `reg-event-fx`, integrating `re-frame-10x` for debugging, or adding Malli schema validation on the app-db.

