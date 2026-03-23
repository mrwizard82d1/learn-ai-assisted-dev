# A Re-frame Tutorial: Scoring a Bowling Game
### Incremental Edition — verify every step before moving on

**Audience:** Junior Clojure/ClojureScript developer, new to re-frame  
**Philosophy:** Each step adds *one small thing*, then asks you to verify it works before continuing. If something breaks you know exactly which change caused it.

---

## Table of Contents

1. [Prerequisites & Mental Model](#1-prerequisites--mental-model)
2. [Scaffold the Project](#2-scaffold-the-project)
3. [✅ Checkpoint 1 — Plain HTML loads in the browser](#-checkpoint-1--plain-html-loads-in-the-browser)
4. [Add ClojureScript and shadow-cljs](#4-add-clojurescript-and-shadow-cljs)
5. [✅ Checkpoint 2 — "Hello from ClojureScript" in the browser](#-checkpoint-2--hello-from-clojurescript-in-the-browser)
6. [Add Reagent — your first component](#6-add-reagent--your-first-component)
7. [✅ Checkpoint 3 — Reagent renders a heading](#-checkpoint-3--reagent-renders-a-heading)
8. [Add re-frame — app-db and initialize-db](#8-add-re-frame--app-db-and-initialize-db)
9. [✅ Checkpoint 4 — app-db initializes without errors](#-checkpoint-4--app-db-initializes-without-errors)
10. [Your first event handler — :roll-ball (stub)](#10-your-first-event-handler--roll-ball-stub)
11. [✅ Checkpoint 5 — clicking a button updates the db](#-checkpoint-5--clicking-a-button-updates-the-db)
12. [Your first subscription — :rolls](#12-your-first-subscription--rolls)
13. [✅ Checkpoint 6 — rolls list appears in the UI](#-checkpoint-6--rolls-list-appears-in-the-ui)
14. [Add bowling domain logic incrementally](#14-add-bowling-domain-logic-incrementally)
15. [✅ Checkpoint 7 — frame parsing works](#-checkpoint-7--frame-parsing-works)
16. [✅ Checkpoint 8 — scoring works](#-checkpoint-8--scoring-works)
17. [Wire logic into subscriptions](#17-wire-logic-into-subscriptions)
18. [✅ Checkpoint 9 — running totals appear in the UI](#-checkpoint-9--running-totals-appear-in-the-ui)
19. [Build the full scorecard view](#19-build-the-full-scorecard-view)
20. [✅ Checkpoint 10 — complete bowling app](#-checkpoint-10--complete-bowling-app)
21. [Production build & next steps](#21-production-build--next-steps)

---

## 1. Prerequisites & Mental Model

### Tools needed

| Tool | Min version | Check |
|------|------------|-------|
| Java JDK | 11+ | `java -version` |
| Clojure CLI | 1.11+ | `clj --version` |
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |

Install `deps-new` once, globally:

```bash
clj -Ttools install io.github.seancorfield/deps-new '{:git/tag "v0.7.0"}' :as new
```

### The re-frame data loop (read this once, refer back often)

```
View  ──dispatch──▶  Event Handler  ──updates──▶  app-db
 ▲                                                   │
 └──────────────  Subscription  ◀────────────────────┘
```

Every step of this tutorial adds one more piece of this loop. By Checkpoint 10 the full loop is running.

---

## 2. Scaffold the Project

```bash
clj -Tnew app :name myorg/bowling-score
cd bowling-score
```

You now have:

```
bowling-score/
├── deps.edn
├── build.clj
└── src/myorg/bowling_score.clj
```

**Replace `deps.edn`** entirely with:

```clojure
{:paths ["src" "resources"]

 :deps
 {org.clojure/clojure       {:mvn/version "1.12.0"}
  org.clojure/clojurescript {:mvn/version "1.11.132"}
  thheller/shadow-cljs      {:mvn/version "2.28.14"}
  re-frame/re-frame         {:mvn/version "1.4.3"}
  reagent/reagent           {:mvn/version "1.2.0"}}

 :aliases
 {:dev  {:extra-paths ["dev"]}
  :test {:extra-paths ["test"]}}}
```

**Create `package.json`** in the project root:

```json
{
  "name": "bowling-score",
  "version": "0.1.0",
  "private": true,
  "devDependencies": {
    "shadow-cljs": "^2.28.14"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1"
  }
}
```

**Install npm packages:**

```bash
npm install
```

**Create `shadow-cljs.edn`** in the project root:

```clojure
{:deps true   ; read Clojure deps from deps.edn — no duplication

 :builds
 {:app
  {:target     :browser
   :output-dir "resources/public/js/compiled"
   :asset-path "/js/compiled"

   :modules
   {:main {:init-fn myorg.bowling-score.core/init}}

   :devtools
   {:http-root  "resources/public"
    :http-port  8080
    :after-load myorg.bowling-score.core/on-js-reload}}}}
```

**Create the output directory** (shadow-cljs needs it to exist):

```bash
mkdir -p resources/public/js/compiled
```

---

## ✅ Checkpoint 1 — Plain HTML loads in the browser

Before writing a single line of ClojureScript, verify that shadow-cljs can serve a static page.

**Create `resources/public/index.html`:**

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <title>Bowling</title>
    <style>
      body { font-family: monospace; background: #1a1a2e; color: #eee; padding: 2rem; }
      h1   { color: #e94560; }
    </style>
  </head>
  <body>
    <div id="app">
      <h1>🎳 Bowling Scorecard</h1>
      <p>Static HTML — ClojureScript not loaded yet.</p>
    </div>
    <!-- We will add the compiled JS here in the next step -->
  </body>
</html>
```

**Start the dev server:**

```bash
npx shadow-cljs watch app
```

Open `http://localhost:8080`.

**What you should see:**  
A dark page with a red heading "🎳 Bowling Scorecard" and the static text. No JavaScript errors in the browser console.

**If you don't see this:** Check that `resources/public/index.html` exists and that the `http-root` in `shadow-cljs.edn` points to `"resources/public"`.

---

## 4. Add ClojureScript and shadow-cljs

Now create the ClojureScript source directory and a minimal entry point.

**Create the source directory:**

```bash
mkdir -p src/myorg/bowling_score
```

**Create `src/myorg/bowling_score/core.cljs`:**

```clojure
(ns myorg.bowling-score.core)

(defn init []
  ;; Locate the #app div and replace its content
  (let [app-el (.getElementById js/document "app")]
    (set! (.-innerHTML app-el)
          "<h1>🎳 Hello from ClojureScript!</h1>")))

(defn on-js-reload []
  ;; Called by shadow-cljs after every hot reload
  (init))
```

> **What's happening here?** We're not using re-frame yet — just raw DOM manipulation to confirm ClojureScript is running. We'll layer in Reagent and re-frame in the next steps.

**Update `resources/public/index.html`** to load the compiled JS — add this `<script>` tag just before `</body>`:

```html
    <script src="/js/compiled/main.js"></script>
```

Your full `index.html` body should now look like:

```html
  <body>
    <div id="app">Loading...</div>
    <script src="/js/compiled/main.js"></script>
  </body>
```

---

## ✅ Checkpoint 2 — "Hello from ClojureScript" in the browser

shadow-cljs should already be running from Checkpoint 1. If you stopped it, restart it:

```bash
npx shadow-cljs watch app
```

Wait for the console to print:

```
[:app] Build completed. (X files, ...)
```

Refresh `http://localhost:8080`.

**What you should see:**  
The heading changes to "🎳 Hello from ClojureScript!" — this text is being injected by your CLJS code, not the HTML.

**Verify hot reload:** Change the string in `core.cljs` to `"Hello again!"`, save the file, and watch the browser update *without* a manual refresh.

**If the page still shows "Loading...":** Open the browser console (F12). Look for a JS error. The most common cause is a namespace mismatch — confirm the `ns` form in `core.cljs` is `myorg.bowling-score.core` and that `:init-fn` in `shadow-cljs.edn` matches exactly.

---

## 6. Add Reagent — your first component

Instead of raw `innerHTML` manipulation, we'll use **Reagent** — a ClojureScript wrapper around React that lets you describe UI as plain Clojure data structures called **hiccup**.

Hiccup is simple:

| Hiccup | HTML |
|--------|------|
| `[:h1 "Hello"]` | `<h1>Hello</h1>` |
| `[:div {:class "box"} [:p "Hi"]]` | `<div class="box"><p>Hi</p></div>` |
| `[:button {:on-click f} "Click"]` | `<button onclick="...">Click</button>` |

**Replace the entire contents of `core.cljs`:**

```clojure
(ns myorg.bowling-score.core
  (:require [reagent.dom :as rdom]))

;; A Reagent component is just a function that returns hiccup.
(defn app-view []
  [:div
   [:h1 "🎳 Bowling Scorecard"]
   [:p "Reagent is rendering this."]])

(defn mount-root []
  ;; rdom/render takes a component and a DOM node
  (rdom/render [app-view] (.getElementById js/document "app")))

(defn init []
  (mount-root))

(defn on-js-reload []
  ;; Re-mount after hot reload so new component code takes effect
  (mount-root))
```

> **Why `[app-view]` with square brackets instead of `(app-view)`?**  
> Square brackets tell Reagent to treat the function as a *component* — it wraps it in React's lifecycle machinery so it re-renders when its data changes. Calling `(app-view)` directly just returns hiccup data without Reagent's change-tracking.

---

## ✅ Checkpoint 3 — Reagent renders a heading

Save `core.cljs`. The browser should hot-reload.

**What you should see:**  
"🎳 Bowling Scorecard" and "Reagent is rendering this." — now rendered by Reagent, not raw DOM manipulation.

**Experiment before continuing:**  
Try adding a second paragraph to `app-view` — save and confirm the browser updates immediately. This proves hot reload is working with Reagent components.

```clojure
(defn app-view []
  [:div
   [:h1 "🎳 Bowling Scorecard"]
   [:p "Reagent is rendering this."]
   [:p {:style {:color "gold"}} "This line proves hot reload works."]])
```

Remove the gold line before continuing.

---

## 8. Add re-frame — app-db and initialize-db

Time to introduce re-frame. We'll add the **app-db** (the single state atom) and the `:initialize-db` event that resets it to a known starting state.

### What is app-db?

It's just a Clojure map wrapped in a Reagent atom. Re-frame manages it for you — you never touch the atom directly. For our bowling app it will hold:

```clojure
{:rolls      []      ; flat vector of all pin counts, e.g. [10 7 3 9 0]
 :game-over? false}
```

**Create `src/myorg/bowling_score/db.cljs`:**

```clojure
(ns myorg.bowling-score.db)

(def default-db
  {:rolls      []
   :game-over? false})
```

**Create `src/myorg/bowling_score/events.cljs`:**

```clojure
(ns myorg.bowling-score.events
  (:require [re-frame.core :as rf]
            [myorg.bowling-score.db :as db]))

;; reg-event-db registers a handler for an event id.
;; The handler is a pure function: (current-db, event-vector) => new-db
(rf/reg-event-db
  :initialize-db
  (fn [_db _event]
    db/default-db))
```

**Update `core.cljs`** to require re-frame, dispatch the init event, and log the db to the console so we can verify it:

```clojure
(ns myorg.bowling-score.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [myorg.bowling-score.events]))  ; <-- side-effectful: registers handlers

(defn app-view []
  [:div
   [:h1 "🎳 Bowling Scorecard"]
   [:p "re-frame initialized — check the browser console."]])

(defn mount-root []
  (rdom/render [app-view] (.getElementById js/document "app")))

(defn init []
  ;; dispatch-sync: runs the handler immediately (synchronously).
  ;; Use this at startup so the db is ready before the first render.
  (rf/dispatch-sync [:initialize-db])
  ;; Peek at the app-db in the console to confirm initialization worked
  (js/console.log "app-db after init:" (clj->js @re-frame.db/app-db))
  (mount-root))

(defn on-js-reload []
  (mount-root))
```

> **Why `dispatch-sync` at startup?** Regular `dispatch` is asynchronous — it queues the event for the next tick. If the first render fires before `:initialize-db` runs, your views see `nil` instead of `default-db` and you get confusing errors. `dispatch-sync` avoids that race condition.

---

## ✅ Checkpoint 4 — app-db initializes without errors

Save all files and let shadow-cljs recompile.

**What you should see in the browser console (F12 → Console):**

```
app-db after init: {rolls: Array(0), game-over?: false}
```

**What to confirm:**  
- No red errors in the console  
- The `rolls` key is an empty array  
- `game-over?` is `false`

**If you see "No event handler registered for event: :initialize-db":**  
The `events` namespace wasn't loaded. Confirm the `[myorg.bowling-score.events]` require is in `core.cljs`. Note it has no `:refer` or `:as` — we're requiring it purely for its *side effect* of calling `reg-event-db`.

Remove the `js/console.log` line from `init` once you've confirmed it works — we won't need it anymore.

---

## 10. Your first event handler — :roll-ball (stub)

Now we'll add a `:roll-ball` event and a simple UI button. We won't do any bowling logic yet — we'll just append the pin count to the `:rolls` vector.

**Add to `events.cljs`** (append below the existing `:initialize-db` handler):

```clojure
;; [:roll-ball n] — record that the player knocked down n pins.
;; No bowling logic yet; we just append to the rolls vector.
(rf/reg-event-db
  :roll-ball
  (fn [db [_event-id pins]]
    (update db :rolls conj pins)))
```

> **Destructuring the event vector:**  
> The event `[:roll-ball 7]` arrives as a vector. `[_event-id pins]` destructures it — `_event-id` is `:roll-ball` (ignored, hence the `_` prefix) and `pins` is `7`.

**Update `app-view` in `core.cljs`** to show some buttons:

```clojure
(defn app-view []
  [:div
   [:h1 "🎳 Bowling Scorecard"]
   [:p "Click a button to roll the ball:"]
   ;; Render one button for each possible pin count 0–10
   (for [n (range 11)]
     [:button
      {:key      n      ; React requires a unique :key in lists
       :on-click #(rf/dispatch [:roll-ball n])
       :style    {:margin "4px" :padding "6px 12px" :cursor "pointer"}}
      n])])
```

> **Why `(for [...] ...)` instead of a list literal?**  
> `for` is a lazy sequence comprehension. Reagent knows how to render sequences of hiccup. The `:key` attribute is required by React to efficiently diff list items — always use a unique, stable key.

---

## ✅ Checkpoint 5 — clicking a button updates the db

**How to verify this without a UI display yet:**

Open the browser console and paste this to inspect the db after clicking buttons:

```javascript
// In the browser console:
cljs.core.clj__GT_js(re_frame.db.app_db.state)
```

Click a few buttons (say, 7, then 3), then run that line.

**What you should see:**

```javascript
{rolls: [7, 3], "game-over?": false}
```

**Alternatively,** add a temporary debug line to `app-view`:

```clojure
(defn app-view []
  [:div
   [:h1 "🎳 Bowling Scorecard"]
   [:p (str "Raw db rolls: " (pr-str (:rolls @re-frame.db/app-db)))]
   ...buttons...])
```

This directly dereferences the app-db atom — fine for debugging, but **not** how you should read state in production components (subscriptions are the right way, coming next).

Click a button and watch the paragraph update.

---

## 12. Your first subscription — :rolls

Directly dereferencing `app-db` in views is an anti-pattern — it bypasses re-frame's change-tracking and makes views hard to test. Instead, views should read state through **subscriptions**.

A subscription is a named query over app-db. Views `subscribe` to it and Reagent re-renders the component only when the subscription's value changes.

**Create `src/myorg/bowling_score/subs.cljs`:**

```clojure
(ns myorg.bowling-score.subs
  (:require [re-frame.core :as rf]))

;; Layer 2 subscription: extracts data directly from app-db.
;; The handler receives the whole db map and returns whatever the view needs.
(rf/reg-sub
  :rolls
  (fn [db _query]
    (:rolls db)))

(rf/reg-sub
  :game-over?
  (fn [db _query]
    (:game-over? db)))
```

**Update `core.cljs`** — require the subs namespace and use `subscribe` in the view:

```clojure
(ns myorg.bowling-score.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [myorg.bowling-score.events]
            [myorg.bowling-score.subs]))   ; <-- registers subscriptions

(defn app-view []
  ;; @(rf/subscribe [...]) returns the current value.
  ;; Reagent tracks this deref and re-renders when the value changes.
  (let [rolls @(rf/subscribe [:rolls])]
    [:div
     [:h1 "🎳 Bowling Scorecard"]
     [:p (str "Rolls so far: " (pr-str rolls))]
     (for [n (range 11)]
       [:button
        {:key      n
         :on-click #(rf/dispatch [:roll-ball n])
         :style    {:margin "4px" :padding "6px 12px" :cursor "pointer"}}
        n])]))

(defn mount-root []
  (rdom/render [app-view] (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))

(defn on-js-reload []
  (mount-root))
```

---

## ✅ Checkpoint 6 — rolls list appears in the UI

Save all files.

**What you should see:**  
The text "Rolls so far: []" on the page. Each button click adds to the list and the text updates immediately — no page refresh needed.

Try clicking: 10, 7, 3, 9, 0. You should see `[10 7 3 9 0]`.

**The re-frame loop is now complete for the first time:**

```
Button click
  → dispatch [:roll-ball n]
    → :roll-ball handler updates :rolls in app-db
      → :rolls subscription recalculates
        → app-view re-renders with new rolls list
```

**Reset between tests:** Refresh the page to clear the rolls (we'll add a Reset button later).

---

## 14. Add bowling domain logic incrementally

All bowling rules live in a separate `logic.cljs` namespace — pure functions with no re-frame dependency. We'll build it in two parts and verify each part in the browser console before moving on.

**Create `src/myorg/bowling_score/logic.cljs`** with just the frame-parsing logic first:

```clojure
(ns myorg.bowling-score.logic)

;; ── Part 1: Parsing a flat rolls vector into frames ──────────────────────────
;;
;; A frame map looks like:
;;   {:frame-num 1, :roll-idx 0, :balls [10], :type :strike}
;;   {:frame-num 2, :roll-idx 1, :balls [7 3], :type :spare}
;;   {:frame-num 3, :roll-idx 3, :balls [9 0], :type :open}

(defn rolls->frames
  "Parse a flat rolls vector into a seq of frame maps (up to 10 frames)."
  [rolls]
  (loop [idx       0
         frames    []
         frame-num 1]
    (if (or (= frame-num 11)
            (>= idx (count rolls)))
      frames
      (cond
        ;; 10th frame: collect up to 3 rolls
        (= frame-num 10)
        (let [balls (subvec rolls idx (min (count rolls) (+ idx 3)))]
          (conj frames {:balls     balls
                        :roll-idx  idx
                        :frame-num 10
                        :type      (cond
                                     (= 10 (first balls)) :strike
                                     (and (>= (count balls) 2)
                                          (= 10 (+ (first balls) (second balls)))) :spare
                                     :else :open)}))

        ;; Strike: only 1 roll consumed
        (= 10 (get rolls idx))
        (recur (inc idx)
               (conj frames {:balls     [(get rolls idx)]
                             :roll-idx  idx
                             :frame-num frame-num
                             :type      :strike})
               (inc frame-num))

        ;; Normal 2-ball frame
        :else
        (let [b1 (get rolls idx 0)
              b2 (get rolls (inc idx) 0)]
          (recur (+ idx 2)
                 (conj frames {:balls     [b1 b2]
                               :roll-idx  idx
                               :frame-num frame-num
                               :type      (if (= 10 (+ b1 b2)) :spare :open)})
                 (inc frame-num)))))))
```

---

## ✅ Checkpoint 7 — frame parsing works

We'll test this in the browser console using ClojureScript's REPL-over-browser that shadow-cljs provides.

**Connect to the shadow-cljs CLJS REPL:**

In a *second terminal* (leave `shadow-cljs watch` running):

```bash
npx shadow-cljs cljs-repl app
```

You'll get a `cljs.user=>` prompt. Test the parser:

```clojure
;; Require the namespace
(require '[myorg.bowling-score.logic :as logic])

;; Test a strike followed by a spare
(logic/rolls->frames [10 7 3 9 0])
;; Expected:
;; [{:frame-num 1, :roll-idx 0, :balls [10], :type :strike}
;;  {:frame-num 2, :roll-idx 1, :balls [7 3], :type :spare}
;;  {:frame-num 3, :roll-idx 3, :balls [9 0], :type :open}]

;; Test a perfect game (12 strikes)
(count (logic/rolls->frames (vec (repeat 12 10))))
;; Expected: 10  (exactly 10 frames)

;; Test the 10th-frame bonus rolls are captured
(last (logic/rolls->frames (vec (repeat 12 10))))
;; Expected: {:frame-num 10, :balls [10 10 10], :type :strike, ...}
```

**If you get "namespace not found":** Make sure you saved `logic.cljs` and shadow-cljs recompiled it (watch the first terminal for "Build completed").

---

## ✅ Checkpoint 8 — scoring works

Now add the scoring functions to the *same* `logic.cljs` file (append below `rolls->frames`):

```clojure
;; ── Part 2: Scoring ──────────────────────────────────────────────────────────

(defn frame-score
  "Score one frame. Returns nil if there aren't enough rolls yet."
  [rolls frame]
  (let [{:keys [frame-num type balls roll-idx]} frame]
    (cond
      ;; 10th frame: just sum whatever balls have been rolled
      (= frame-num 10)
      (when (seq balls) (apply + balls))

      ;; Strike: 10 + next 2 rolls
      (= type :strike)
      (when (>= (count rolls) (+ roll-idx 3))
        (+ 10 (get rolls (+ roll-idx 1) 0)
               (get rolls (+ roll-idx 2) 0)))

      ;; Spare: 10 + next 1 roll
      (= type :spare)
      (when (>= (count rolls) (+ roll-idx 3))
        (+ 10 (get rolls (+ roll-idx 2) 0)))

      ;; Open: sum of 2 balls
      :else
      (apply + balls))))

(defn frame-scores
  "Vector of per-frame scores (nil for frames not yet scoreable)."
  [rolls]
  (mapv #(frame-score rolls %) (rolls->frames rolls)))

(defn running-totals
  "Cumulative running total across frames. Frames not yet scoreable produce nil."
  [scores]
  (reduce (fn [totals score]
            (if (nil? score)
              (conj totals nil)
              (conj totals (+ (or (last (filter some? totals)) 0) score))))
          []
          scores))

(defn game-over?
  "True once all 10 frames are fully complete."
  [rolls]
  (let [frames (rolls->frames rolls)]
    (when (= 10 (count frames))
      (let [last-frame (last frames)]
        (cond
          (#{:strike :spare} (:type last-frame)) (= 3 (count (:balls last-frame)))
          :else                                   (= 2 (count (:balls last-frame))))))))

(defn valid-pin-counts
  "Legal values for the next roll given rolls so far."
  [rolls]
  (let [frames      (rolls->frames rolls)
        frame-count (count frames)
        in-10th?    (= frame-count 10)]
    (if in-10th?
      (let [balls         (:balls (last frames))
            ball-in-frame (count balls)]
        (cond
          (= ball-in-frame 0) (range 11)
          (and (= ball-in-frame 1) (= 10 (first balls))) (range 11)
          (= ball-in-frame 1) (range (- 11 (first balls)))
          (and (= ball-in-frame 2)
               (= 10 (+ (first balls) (second balls)))) (range 11)
          (and (= ball-in-frame 2)
               (= 10 (first balls))
               (= 10 (second balls))) (range 11)
          (= ball-in-frame 2) (range (- 11 (second balls)))
          :else (range 11)))
      ;; Frames 1–9
      (let [last-frame    (last frames)
            ball-in-frame (count (:balls last-frame))]
        (if (or (nil? last-frame)
                (= (:type last-frame) :strike)
                (= ball-in-frame 2))
          (range 11)
          (range (- 11 (first (:balls last-frame)))))))))
```

**Test in the REPL** (reload the namespace first):

```clojure
(require '[myorg.bowling-score.logic :as logic] :reload)

;; Perfect game = 300
(last (logic/running-totals
        (logic/frame-scores (vec (repeat 12 10)))))
;; => 300

;; All gutter = 0
(last (logic/running-totals
        (logic/frame-scores (vec (repeat 20 0)))))
;; => 0

;; All 5-spare = 150
(last (logic/running-totals
        (logic/frame-scores (vec (repeat 21 5)))))
;; => 150

;; Incomplete game — last entry is nil (not enough rolls yet for bonus)
(logic/running-totals
  (logic/frame-scores [10]))
;; => [nil]   ← strike in frame 1, bonus rolls not yet bowled
```

---

## 17. Wire logic into subscriptions

Now that the logic is verified, add derived subscriptions that call it.

**Update `subs.cljs`** — add these below the existing two subscriptions:

```clojure
(ns myorg.bowling-score.subs
  (:require [re-frame.core :as rf]
            [myorg.bowling-score.logic :as logic]))

;; ── Layer 2: raw values from app-db ──────────────────────────────────────────

(rf/reg-sub
  :rolls
  (fn [db _] (:rolls db)))

(rf/reg-sub
  :game-over?
  (fn [db _] (:game-over? db)))

;; ── Layer 3: derived/computed — take other subs as inputs via :<- ─────────────
;;
;; :<- [:rolls] means "my input is the value produced by the :rolls subscription".
;; These handlers receive the *value* of that sub, not the raw db.
;; They only recompute when :rolls changes.

(rf/reg-sub
  :frames
  :<- [:rolls]
  (fn [rolls _] (logic/rolls->frames rolls)))

(rf/reg-sub
  :frame-scores
  :<- [:rolls]
  (fn [rolls _] (logic/frame-scores rolls)))

(rf/reg-sub
  :running-totals
  :<- [:frame-scores]
  (fn [frame-scores _] (logic/running-totals frame-scores)))

(rf/reg-sub
  :current-frame
  :<- [:rolls]
  (fn [rolls _] (logic/current-frame-index rolls)))

(rf/reg-sub
  :valid-pins
  :<- [:rolls]
  (fn [rolls _] (logic/valid-pin-counts rolls)))
```

Also add `current-frame-index` to `logic.cljs` (append):

```clojure
(defn current-frame-index
  "1-based index of the frame currently being played (1–10)."
  [rolls]
  (min 10 (inc (count (rolls->frames rolls)))))
```

**Update `events.cljs`** to use the logic for game-over detection:

```clojure
(ns myorg.bowling-score.events
  (:require [re-frame.core :as rf]
            [myorg.bowling-score.db :as db]
            [myorg.bowling-score.logic :as logic]))

(rf/reg-event-db
  :initialize-db
  (fn [_db _event] db/default-db))

(rf/reg-event-db
  :roll-ball
  (fn [db [_event-id pins]]
    (let [new-rolls (conj (:rolls db) pins)]
      (assoc db
             :rolls      new-rolls
             :game-over? (boolean (logic/game-over? new-rolls))))))

(rf/reg-event-db
  :reset-game
  (fn [_db _event] db/default-db))
```

---

## ✅ Checkpoint 9 — running totals appear in the UI

**Update `app-view` in `core.cljs`** to show running totals alongside rolls:

```clojure
(ns myorg.bowling-score.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [myorg.bowling-score.events]
            [myorg.bowling-score.subs]))

(defn app-view []
  (let [rolls          @(rf/subscribe [:rolls])
        running-totals @(rf/subscribe [:running-totals])
        game-over?     @(rf/subscribe [:game-over?])]
    [:div
     [:h1 "🎳 Bowling Scorecard"]

     [:p (str "Frame: " @(rf/subscribe [:current-frame]))]
     [:p (str "Rolls: " (pr-str rolls))]
     [:p (str "Running totals: " (pr-str running-totals))]

     (when game-over?
       [:p {:style {:color "gold" :font-size "1.3rem"}}
        (str "Game over! Final score: " (last (filter some? running-totals)))])

     (when-not game-over?
       [:div
        [:p "Roll — select pins knocked down:"]
        (let [valid @(rf/subscribe [:valid-pins])]
          (for [n (range 11)]
            [:button
             {:key      n
              :disabled (not (contains? (set valid) n))
              :on-click #(rf/dispatch [:roll-ball n])
              :style    {:margin "4px" :padding "6px 12px" :cursor "pointer"
                         :opacity (if (contains? (set valid) n) "1" "0.3")}}
             n]))])

     [:button {:on-click #(rf/dispatch [:reset-game])
               :style    {:margin-top "1rem" :padding "6px 12px"}}
      "New Game"]]))

(defn mount-root []
  (rdom/render [app-view] (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))

(defn on-js-reload []
  (mount-root))
```

**What you should see and verify:**

1. Roll a strike (button "10") — the frame counter advances to 2, running totals shows `[nil]` (bonus not yet determined)
2. Roll 7, then 3 — running totals shows `[20 nil]` (strike bonus is now known: 10+7+3=20; frame 2 spare bonus pending)
3. Roll 9 — running totals shows `[20 nil]` then when you roll the next ball it completes
4. Bowl a perfect game (click 10 twelve times) — "Game Over! Final score: 300"
5. Click "New Game" — everything resets

The disabled/faded buttons prove `:valid-pins` is working — after rolling 7 you can't roll more than 3.

---

## 19. Build the full scorecard view

The previous view was debug-output style. Now we'll replace it with a proper scorecard table. This is mostly HTML/CSS work — the re-frame wiring is already complete.

**Create `src/myorg/bowling_score/views.cljs`:**

```clojure
(ns myorg.bowling-score.views
  (:require [re-frame.core :as rf]))

;; ── Per-frame display ─────────────────────────────────────────────────────────

(defn frame-balls-display
  "Render the ball results inside a frame cell."
  [{:keys [balls type frame-num]}]
  (cond
    ;; Frames 1–9 strike
    (and (not= frame-num 10) (= type :strike))
    "X"

    ;; Frames 1–9 spare
    (and (not= frame-num 10) (= type :spare))
    (str (first balls) " /")

    ;; 10th frame — show each ball
    (= frame-num 10)
    (let [b0 (get balls 0)
          b1 (get balls 1)
          b2 (get balls 2)]
      (str
       (if (nil? b0) "_" (if (= 10 b0) "X" b0))
       (when (some? b1)
         (str " " (cond (= 10 b1) "X"
                        (= 10 (+ b0 b1)) "/"
                        :else b1)))
       (when (some? b2)
         (str " " (cond (= 10 b2) "X"
                        (= 10 (+ b1 b2)) "/"
                        :else b2)))))

    ;; Open frame
    :else
    (str (or (first balls) "_")
         (when (second balls) (str " " (second balls))))))

;; ── Scorecard table ───────────────────────────────────────────────────────────

(defn scorecard []
  (let [frames         @(rf/subscribe [:frames])
        running-totals @(rf/subscribe [:running-totals])
        current-frame  @(rf/subscribe [:current-frame])
        game-over?     @(rf/subscribe [:game-over?])]
    [:div
     [:h2 "Scorecard"]
     [:table {:style {:border-collapse "collapse" :margin-top "1rem"}}
      [:thead
       [:tr
        (for [i (range 1 11)]
          [:th {:key   i
                :style {:border "2px solid #444"
                        :padding "0.4rem 0.8rem"
                        :background "#16213e"
                        :color "#e94560"
                        :min-width "60px"}}
           (str "F" i)])]]
      [:tbody
       ;; Row 1: ball results per frame
       [:tr
        (for [i (range 10)]
          (let [frame       (get frames i)
                is-current? (and (not game-over?) (= (inc i) current-frame))]
            [:td {:key   i
                  :style {:border     "2px solid #444"
                          :padding    "0.4rem 0.8rem"
                          :text-align "center"
                          :background (if is-current? "#1a3a5c" "transparent")
                          :color      (if is-current? "#ffd700" "#eee")}}
             (if frame
               (frame-balls-display frame)
               [:span {:style {:color "#555"}} "·"])]))]
       ;; Row 2: cumulative scores
       [:tr
        (for [i (range 10)]
          [:td {:key   i
                :style {:border     "2px solid #444"
                        :padding    "0.4rem 0.8rem"
                        :text-align "center"
                        :background "#0f3460"
                        :font-weight "bold"}}
           (or (get running-totals i) "")])]]]]))

;; ── Pin buttons ───────────────────────────────────────────────────────────────

(defn pin-buttons []
  (let [valid      (set @(rf/subscribe [:valid-pins]))
        game-over? @(rf/subscribe [:game-over?])]
    [:div {:style {:margin-top "1.5rem"}}
     [:h3 "Select pins knocked down:"]
     [:div
      (for [n (range 11)]
        (let [enabled? (and (not game-over?) (contains? valid n))]
          [:button
           {:key      n
            :disabled (not enabled?)
            :on-click #(rf/dispatch [:roll-ball n])
            :style    {:margin       "4px"
                       :padding      "8px 14px"
                       :background   (if enabled? "#e94560" "#555")
                       :color        "#fff"
                       :border       "none"
                       :border-radius "4px"
                       :cursor       (if enabled? "pointer" "not-allowed")
                       :font-size    "1rem"}}
           n]))]]))

;; ── Game-over banner ──────────────────────────────────────────────────────────

(defn game-over-banner []
  (let [totals @(rf/subscribe [:running-totals])
        final  (last (filter some? totals))]
    [:div {:style {:margin-top   "1rem"
                   :padding      "1rem 1.5rem"
                   :background   "#e94560"
                   :border-radius "8px"
                   :font-size    "1.4rem"}}
     (str "🎳 Game Over! Final Score: " (or final 0))]))

;; ── Root component ────────────────────────────────────────────────────────────

(defn app-view []
  (let [game-over? @(rf/subscribe [:game-over?])]
    [:div
     [:h1 "🎳 Bowling Scorecard"]
     [scorecard]
     (if game-over?
       [game-over-banner]
       [pin-buttons])
     [:div {:style {:margin-top "1.5rem"}}
      [:button {:on-click #(rf/dispatch [:reset-game])
                :style    {:padding      "8px 16px"
                           :background   "#333"
                           :color        "#eee"
                           :border       "1px solid #555"
                           :border-radius "4px"
                           :cursor       "pointer"}}
       "New Game"]]]))
```

**Update `core.cljs`** to use the new views namespace:

```clojure
(ns myorg.bowling-score.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [myorg.bowling-score.events]
            [myorg.bowling-score.subs]
            [myorg.bowling-score.views :refer [app-view]]))

(defn mount-root []
  (rdom/render [app-view] (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))

(defn on-js-reload []
  (mount-root))
```

**Update `resources/public/index.html`** with final styles:

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Bowling Scorecard</title>
    <style>
      body {
        font-family: monospace;
        background: #1a1a2e;
        color: #eee;
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 2rem;
      }
      h1 { color: #e94560; }
      #app { max-width: 900px; width: 100%; }
    </style>
  </head>
  <body>
    <div id="app">Loading...</div>
    <script src="/js/compiled/main.js"></script>
  </body>
</html>
```

---

## ✅ Checkpoint 10 — complete bowling app

**Final file tree:**

```
bowling-score/
├── deps.edn
├── shadow-cljs.edn
├── package.json
├── resources/public/
│   ├── index.html
│   └── js/compiled/        ← generated by shadow-cljs
└── src/myorg/bowling_score/
    ├── core.cljs            ← entry point
    ├── db.cljs              ← default-db
    ├── events.cljs          ← reg-event-db handlers
    ├── subs.cljs            ← reg-sub subscriptions
    ├── logic.cljs           ← pure bowling logic
    └── views.cljs           ← Reagent components
```

**Verification checklist — play through these scenarios:**

- [ ] Fresh page load shows empty scorecard, all pins 0–10 are enabled
- [ ] Roll a strike (10) — "X" appears in frame 1, frame counter moves to 2, running total for frame 1 shows blank (bonus pending)
- [ ] Roll 7, then 3 — frame 1 total is now 20 (10+7+3), frame 2 shows "7 /" with blank total (spare bonus pending)
- [ ] Roll 4 — frame 2 total becomes 34 (20+10+4), frame 3 starts
- [ ] After rolling 7 in a frame, only buttons 0–3 are enabled (can't knock down more than remaining pins)
- [ ] Bowl 12 strikes in a row — game over, final score 300
- [ ] "New Game" resets everything

---

## 21. Production build & next steps

### Production build

```bash
npx shadow-cljs release app
```

Outputs a minified `resources/public/js/compiled/main.js`. Deploy the `resources/public/` directory to any static host.

### Concepts to explore next (in order)

1. **`reg-event-fx`** — event handlers that produce side effects (HTTP calls, timers, localStorage writes). Needed the moment you want to persist a game score.

2. **`re-frame-10x`** — a dev-time panel that shows every event dispatched, every db snapshot before and after, and every subscription value. Add it in one line; it's the most useful re-frame debugging tool.

3. **Interceptors** — middleware that wraps event handlers. Used for cross-cutting concerns like logging every event or validating the db shape after every transition.

4. **Malli schema validation** — define a schema for `default-db` and add an interceptor that checks it after every event in development. Catches shape errors immediately.

5. **Testing event handlers** — since `reg-event-db` handlers are pure functions `(db, event) → new-db`, they need no test harness beyond `cljs.test`. Write tests the same way you tested `logic.cljs` in Checkpoints 7 and 8.

---

*Happy bowling — and happy hacking!* 🎳
