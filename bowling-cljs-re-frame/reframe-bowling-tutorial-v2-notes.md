# Reframe Bowling Tutorial V2 Notes

Great observation — learning is much more effective when you get fast feedback loops. Let me restructure the tutorial so every small addition is immediately verifiable in the browser.

__Created a file, read a file_

Here's what changed and why each checkpoint is placed where it is.

### The restructuring logic

The tutorial now has 10 checkpoints, each introduced as soon as the smallest meaningful thing can be verified:

| Checkpoint | What you verify          | Re-frame concept introduced                                             |
|------------|--------------------------|-------------------------------------------------------------------------|
| 1          | Static HTML loads        | Nothing -- just shadow-cljs serving files                               |
| 2          | ClojureScript runs       | Raw DOM from CLJS                                                       |
| 3          | Reagent renders          | Hiccup / components                                                     |
| 4          | app-db initializes       | `reg-event-db`, `dispatch-sync` -- verified in the browser console      |
| 5          | Button updates the db    | :roll-ball handler -- verified via console inspection                   |
| 6          | Rolls list visible to UI | `reg-sub` + `subscribe` -- the full loop closes here for the first time |
| 7          | Frame parsing correctly  | Pure logic -- verified in the shadow-cljs CLJS REPL                     |
| 8          | Scoring correct          | More pure logic - REPL tests for 300, 0, 150                            |
| 9          | Running totals in UI     | Layer-3 `:<-` subscriptions, valid-pin disabling                        |
| 10         | Full styled scorecard    | Views namespace, final component comparison                             |

A few other deliberate choices worth noting:

- **Checkpoints 7 and 8 use the shadow-cljs CLJS REPL**, not the browser UI. Logic is pure and fast to test interactively — you don't need to click through a UI to know if `(running-totals (frame-scores (repeat 12 10)))` returns 300.
- **Checkpoint 5 intentionally uses the browser console** before subscriptions exist — this teaches you that the db is a real atom you can inspect, not a black box.
- **The debug app-view in Checkpoint 9 is explicitly temporary** — it's replaced by the real scorecard in step 19, so you see both the debug style and the production style.
