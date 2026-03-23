# A Re-frame Tutorial: Scoring a Bowling Game

**Audience:** Junior Clojure/ClojureScript developer, new to re-frame  
**Goal:** Build a working bowling scorecard app using re-frame, from project scaffolding to a complete UI

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Mental Model: What Is re-frame?](#2-mental-model-what-is-re-frame)
3. [Scaffold the Project](#3-scaffold-the-project)
4. [Add shadow-cljs and Re-frame Dependencies](#4-add-shadow-cljs-and-re-frame-dependencies)
5. [Configure shadow-cljs](#5-configure-shadow-cljs)
6. [Create the HTML Entry Point](#6-create-the-html-entry-point)
7. [Bootstrap the ClojureScript Application](#7-bootstrap-the-clojurescript-application)
8. [Re-frame Core Concepts (with Bowling Examples)](#8-re-frame-core-concepts-with-bowling-examples)
9. [The App-DB: Application State](#9-the-app-db-application-state)
10. [Events: How State Changes](#10-events-how-state-changes)
11. [Subscriptions: Derived Data for the View](#11-subscriptions-derived-data-for-the-view)
12. [Bowling Scoring Logic](#12-bowling-scoring-logic)
13. [Views: Reagent Components](#13-views-reagent-components)
14. [Wiring It All Together](#14-wiring-it-all-together)
15. [Running and Testing the App](#15-running-and-testing-the-app)
16. [Summary and Next Steps](#16-summary-and-next-steps)

---

## 1. Prerequisites

Make sure the following are installed on your machine:

| Tool | Minimum Version | Check With |
|------|----------------|------------|
| Java (JDK) | 11+ | `java -version` |
| Clojure CLI (`clj`) | 1.11+ | `clj --version` |
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |

Install `deps-new`, a tool for generating Clojure project scaffolding:

```bash
clj -Ttools install io.github.seancorfield/deps-new '{:git/tag "v0.7.0"}' :as new
```

Verify it works:

```bash
clj -Tnew help/doc
```

---

## 2. Mental Model: What Is re-frame?

Before touching any code, let's anchor the concepts.

Re-frame is a **pattern** (and a small library) for building ClojureScript single-page applications on top of **Reagent** (a ClojureScript wrapper around React). It enforces a strict, unidirectional data flow:

```
View  ──dispatch──▶  Event Handler  ──updates──▶  App-DB
 ▲                                                    │
 └──────────────  Subscription  ◀────────────────────┘
```

The six building blocks are:

| Concept | One-liner | Bowling analogy |
|---------|-----------|-----------------|
| **app-db** | A single Clojure map holding all app state | The scoresheet for the whole game |
| **event** | A vector describing *what happened* | "Pin knocked down", "New frame started" |
| **event handler** (reg-event-db) | A pure function `(db, event) → new-db` | The scorer who updates the sheet |
| **subscription** (reg-sub) | A pure function that extracts/derives data from db | The announcer reading off the running total |
| **view** | A Reagent component that renders data | The scoreboard display on the wall |
| **effect** | A side effect triggered after an event | Saving the game to a server |

The golden rule: **data flows in one direction; the view never mutates state directly.**

---

## 3. Scaffold the Project

Use `deps-new` with the built-in `app` template:

```bash
clj -Tnew app :name myorg/bowling-score
```

This creates:

```
bowling-score/
├── deps.edn
├── build.clj
├── src/
│   └── myorg/
│       └── bowling_score.clj   ← we'll replace this
└── test/
    └── myorg/
        └── bowling_score_test.clj
```

> **Why deps-new?** It gives us a `deps.edn`-based project (the modern Clojure standard) and a clean directory structure. We will layer shadow-cljs on top of it.

Navigate into the project:

```bash
cd bowling-score
```

---

## 4. Add shadow-cljs and Re-frame Dependencies

### 4.1 Update `deps.edn`

Replace the generated `deps.edn` with the following:

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
  :test {:extra-paths ["test"]
         :extra-deps  {io.github.cognitect-labs/test-runner
                       {:git/tag "v0.5.1" :git/sha "dfb30dd"}}}}}
```

**Key deps explained:**

- `shadow-cljs` — the build tool that compiles ClojureScript and hot-reloads code during development
- `re-frame` — the state management library
- `reagent` — the React wrapper; re-frame depends on it but we pin it explicitly for clarity

### 4.2 Install Node.js packages

shadow-cljs uses npm for JS dependencies. Create `package.json`:

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

Install them:

```bash
npm install
```

---

## 5. Configure shadow-cljs

Create `shadow-cljs.edn` in the project root:

```clojure
{;; Use the deps.edn file for Clojure dependencies (instead of shadow's own :dependencies key)
 :deps     true

 ;; Where shadow-cljs will write its cache and compiled output
 :builds
 {:app
  {:target     :browser
   :output-dir "resources/public/js/compiled"
   :asset-path "/js/compiled"

   :modules
   {:main {:init-fn myorg.bowling-score.core/init}}

   :devtools
   {:http-root    "resources/public"
    :http-port    8080
    :after-load   myorg.bowling-score.core/on-js-reload
    :watch-dirs   ["src"]}}}}
```

**Breaking this down:**

- `:deps true` — tells shadow-cljs to pull Clojure deps from `deps.edn` instead of duplicating them here
- `:target :browser` — we're compiling for the browser (as opposed to Node.js or a library)
- `:modules {:main {:init-fn ...}}` — the entry point function shadow-cljs will call when the JS loads
- `:devtools` — enables a local HTTP server and hot code reloading during development

---

## 6. Create the HTML Entry Point

```bash
mkdir -p resources/public/js/compiled
```

Create `resources/public/index.html`:

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
      table { border-collapse: collapse; margin-top: 1.5rem; }
      td, th {
        border: 2px solid #444;
        padding: 0.5rem 1rem;
        text-align: center;
        min-width: 60px;
      }
      th { background: #16213e; color: #e94560; }
      .score-row td { background: #0f3460; font-weight: bold; font-size: 1.1rem; }
      button {
        margin: 0.25rem;
        padding: 0.5rem 1rem;
        background: #e94560;
        color: #fff;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-size: 1rem;
      }
      button:hover { background: #c73652; }
      button:disabled { background: #555; cursor: not-allowed; }
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

## 7. Bootstrap the ClojureScript Application

Create the source directory structure:

```bash
mkdir -p src/myorg/bowling_score
```

Create `src/myorg/bowling_score/core.cljs`:

```clojure
(ns myorg.bowling-score.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [myorg.bowling-score.events]   ;; side-effectful require: registers handlers
            [myorg.bowling-score.subs]     ;; side-effectful require: registers subscriptions
            [myorg.bowling-score.views :refer [app-view]]))

(defn mount-root []
  ;; Render the root Reagent component into the #app div
  (rdom/render [app-view] (.getElementById js/document "app")))

(defn init []
  ;; Called once when the page first loads.
  ;; dispatch-sync ensures the db is initialized before the first render.
  (rf/dispatch-sync [:initialize-db])
  (mount-root))

(defn on-js-reload []
  ;; Called by shadow-cljs after every hot reload.
  ;; Re-rendering picks up new view code without losing app state.
  (mount-root))
```

> **Why `dispatch-sync`?** Normally `dispatch` is asynchronous (scheduled on the next event loop tick). For initialization we use `dispatch-sync` so the app-db is populated *before* the first render, avoiding a flash of uninitialized state.

---

## 8. Re-frame Core Concepts (with Bowling Examples)

The next four sections each introduce one re-frame building block. We will build the bowling app incrementally — app-db first, then events, then subscriptions, then views.

### Bowling Rules Refresher

A bowling game has **10 frames**. In each frame a player has up to 2 rolls to knock down 10 pins.

- **Strike** (X): All 10 pins on the first roll. The frame is complete. Bonus = next 2 rolls.
- **Spare** (/): All remaining pins on the second roll. Bonus = next 1 roll.
- **Open frame**: Neither strike nor spare. No bonus.
- **10th frame special rule**: If you get a strike or spare in the 10th frame you get extra rolls (up to 3 total in frame 10).

---

## 9. The App-DB: Application State

The **app-db** is just a Clojure atom containing a map. Think of it as your application's entire "database" living in memory.

For our bowling app, we'll model state like this:

```clojure
{:rolls      []         ; all rolls recorded so far, e.g. [10 7 3 9 0 ...]
 :game-over? false}     ; true once 10 frames are complete
```

We keep it simple: just a flat vector of pin counts. The scoring algorithm will interpret the vector according to bowling rules.

Create `src/myorg/bowling_score/db.cljs`:

```clojure
(ns myorg.bowling-score.db)

;; The initial state of the application database.
;; This map is the single source of truth for the entire app.
(def default-db
  {:rolls      []
   :game-over? false})
```

---

## 10. Events: How State Changes

An **event** in re-frame is a vector whose first element is a keyword (the event id):

```clojure
[:roll-ball 7]           ;; "the player knocked down 7 pins"
[:initialize-db]         ;; "reset everything"
```

You dispatch events from views or effects. Re-frame routes each event to its registered handler.

An **event handler** is a pure function `(db, event-vector) → new-db`. It must be pure — no side effects, no I/O.

Create `src/myorg/bowling_score/events.cljs`:

```clojure
(ns myorg.bowling-score.events
  (:require [re-frame.core :as rf]
            [myorg.bowling-score.db :as db]
            [myorg.bowling-score.logic :as logic]))

;; ── :initialize-db ────────────────────────────────────────────────────────────
;; Reset the app to its initial state. Called once at startup.
(rf/reg-event-db
  :initialize-db
  (fn [_db _event]
    db/default-db))

;; ── :roll-ball ────────────────────────────────────────────────────────────────
;; Record a single roll. The event vector is [:roll-ball n]
;; where n is the number of pins knocked down.
(rf/reg-event-db
  :roll-ball
  (fn [db [_event-id pins]]
    (let [new-rolls (conj (:rolls db) pins)
          game-over? (logic/game-over? new-rolls)]
      (assoc db
             :rolls      new-rolls
             :game-over? game-over?))))

;; ── :reset-game ───────────────────────────────────────────────────────────────
;; Convenience event to start a new game.
(rf/reg-event-db
  :reset-game
  (fn [_db _event]
    db/default-db))
```

### Understanding `reg-event-db`

`reg-event-db` is the most common event registration function. Its handler signature is:

```
(fn [db event-vector] => new-db)
```

Re-frame passes the *current* app-db as the first arg, and the full event vector as the second. The return value becomes the *new* app-db. Re-frame handles the atom swap for you.

There's also `reg-event-fx` for handlers that need to cause **effects** (HTTP calls, localStorage writes, etc.). We'll stick to `reg-event-db` for now.

---

## 11. Subscriptions: Derived Data for the View

Views should never reach directly into app-db. Instead they **subscribe** to derived signals. This is powerful because:

1. Subscriptions are **memoized** — they only recompute when their inputs change
2. They decouple views from the shape of app-db

Create `src/myorg/bowling_score/subs.cljs`:

```clojure
(ns myorg.bowling-score.subs
  (:require [re-frame.core :as rf]
            [myorg.bowling-score.logic :as logic]))

;; ── Layer 2 subscriptions (directly from app-db) ──────────────────────────────

(rf/reg-sub
  :rolls
  (fn [db _query]
    (:rolls db)))

(rf/reg-sub
  :game-over?
  (fn [db _query]
    (:game-over? db)))

;; ── Layer 3 subscriptions (derived from layer 2) ──────────────────────────────
;; These take other subscriptions as inputs rather than app-db directly.
;; This is the recommended pattern for derived/computed data.

(rf/reg-sub
  :frame-scores
  ;<- is syntactic sugar for declaring input subscriptions
  :<- [:rolls]
  (fn [rolls _query]
    (logic/frame-scores rolls)))

(rf/reg-sub
  :running-totals
  :<- [:frame-scores]
  (fn [frame-scores _query]
    (logic/running-totals frame-scores)))

(rf/reg-sub
  :current-frame
  :<- [:rolls]
  (fn [rolls _query]
    (logic/current-frame-index rolls)))

(rf/reg-sub
  :valid-pins
  :<- [:rolls]
  (fn [rolls _query]
    (logic/valid-pin-counts rolls)))

(rf/reg-sub
  :frames
  :<- [:rolls]
  (fn [rolls _query]
    (logic/rolls->frames rolls)))
```

### Subscription Layers

Re-frame encourages a **two-layer** subscription architecture:

- **Layer 2** — extracts raw data directly from `db` (simple `get` operations)
- **Layer 3** — derives/computes from layer 2 subscriptions using `:<-`

This way, if you reshape `app-db`, you only update layer 2 subs. Views and layer 3 subs are insulated from the change.

Using a subscription in a view looks like:

```clojure
(let [rolls @(rf/subscribe [:rolls])]
  ...)
```

The `@` dereferences the subscription atom. Reagent automatically re-renders the component whenever the subscription's value changes.

---

## 12. Bowling Scoring Logic

This is pure Clojure — no re-frame involved yet. We isolate all domain logic here so it's easy to unit test.

Create `src/myorg/bowling_score/logic.cljs`:

```clojure
(ns myorg.bowling-score.logic)

;; ──────────────────────────────────────────────────────────────────────────────
;; Low-level predicates
;; ──────────────────────────────────────────────────────────────────────────────

(defn strike? [rolls idx]
  (= 10 (get rolls idx)))

(defn spare? [rolls idx]
  (and (not (strike? rolls idx))
       (= 10 (+ (get rolls idx 0)
                (get rolls (inc idx) 0)))))

;; ──────────────────────────────────────────────────────────────────────────────
;; Frame parsing
;;
;; We need to group a flat vector of rolls into 10 frames,
;; respecting the strike / spare / 10th-frame rules.
;;
;; A frame is represented as a map:
;;   {:balls [n ...]        ; pin counts for the rolls in this frame
;;    :roll-idx i           ; index in the flat rolls vector where this frame starts
;;    :type :strike|:spare|:open}
;; ──────────────────────────────────────────────────────────────────────────────

(defn rolls->frames
  "Parse a flat rolls vector into a seq of frame maps (up to 10 frames)."
  [rolls]
  (loop [idx    0
         frames []
         frame-num 1]
    (if (or (= frame-num 11)
            (>= idx (count rolls)))
      frames
      (cond
        ;; 10th frame: take up to 3 remaining rolls
        (= frame-num 10)
        (let [balls (subvec rolls idx (min (count rolls) (+ idx 3)))]
          (conj frames {:balls     balls
                        :roll-idx  idx
                        :frame-num 10
                        :type      (cond
                                     (= 10 (first balls)) :strike
                                     (= 10 (+ (first balls) (second balls))) :spare
                                     :else :open)}))

        ;; Strike: frame uses only 1 roll
        (strike? rolls idx)
        (recur (inc idx)
               (conj frames {:balls     [(get rolls idx)]
                             :roll-idx  idx
                             :frame-num frame-num
                             :type      :strike})
               (inc frame-num))

        ;; Normal frame: 2 rolls
        :else
        (let [b1 (get rolls idx 0)
              b2 (get rolls (inc idx) 0)]
          (recur (+ idx 2)
                 (conj frames {:balls     [b1 b2]
                               :roll-idx  idx
                               :frame-num frame-num
                               :type      (if (= 10 (+ b1 b2)) :spare :open)})
                 (inc frame-num)))))))

;; ──────────────────────────────────────────────────────────────────────────────
;; Scoring
;; ──────────────────────────────────────────────────────────────────────────────

(defn frame-score
  "Score a single frame given the flat rolls vector and the frame's starting index.
   Returns nil if there aren't enough rolls yet to determine the score."
  [rolls frame roll-idx]
  (let [frame-num (:frame-num frame)]
    (cond
      ;; 10th frame: just sum the balls actually recorded
      (= frame-num 10)
      (when (seq (:balls frame))
        (apply + (:balls frame)))

      ;; Strike: 10 + next 2 rolls
      (= :strike (:type frame))
      (when (>= (count rolls) (+ roll-idx 3))
        (+ 10
           (get rolls (+ roll-idx 1) 0)
           (get rolls (+ roll-idx 2) 0)))

      ;; Spare: 10 + next 1 roll
      (= :spare (:type frame))
      (when (>= (count rolls) (+ roll-idx 3))
        (+ 10
           (get rolls (+ roll-idx 2) 0)))

      ;; Open: sum of 2 balls
      :else
      (apply + (:balls frame)))))

(defn frame-scores
  "Return a vector of per-frame scores (or nil for frames not yet scoreable)."
  [rolls]
  (let [frames (rolls->frames rolls)]
    (mapv (fn [frame]
            (frame-score rolls frame (:roll-idx frame)))
          frames)))

(defn running-totals
  "Turn a vector of frame scores into a running cumulative total.
   Frames with nil scores (not yet complete) produce nil entries."
  [scores]
  (reduce (fn [totals score]
            (if (nil? score)
              (conj totals nil)
              (let [prev (or (last (filter some? totals)) 0)]
                (conj totals (+ prev score)))))
          []
          scores))

;; ──────────────────────────────────────────────────────────────────────────────
;; Game-state helpers used by events and subscriptions
;; ──────────────────────────────────────────────────────────────────────────────

(defn current-frame-index
  "Return the 1-based index of the frame currently being played (1–10)."
  [rolls]
  (let [frames (rolls->frames rolls)]
    (min 10 (inc (count frames)))))

(defn game-over?
  "True once all 10 frames have been fully scored."
  [rolls]
  (let [frames (rolls->frames rolls)]
    (when (= 10 (count frames))
      (let [last-frame (last frames)]
        (cond
          ;; 10th frame strike needs 3 balls
          (= :strike (:type last-frame))
          (= 3 (count (:balls last-frame)))

          ;; 10th frame spare needs 3 balls
          (= :spare (:type last-frame))
          (= 3 (count (:balls last-frame)))

          ;; open 10th frame is done after 2 balls
          :else
          (= 2 (count (:balls last-frame))))))))

(defn valid-pin-counts
  "Return the set of legal pin values for the next roll,
   given what has already been rolled."
  [rolls]
  (let [frames       (rolls->frames rolls)
        frame-count  (count frames)
        in-10th?     (= frame-count 10)]
    (if in-10th?
      (let [last-frame (:balls (last frames))
            ball-in-frame (count last-frame)]
        (cond
          ;; First ball of 10th: any value 0–10
          (= ball-in-frame 0) (range 11)

          ;; Second ball of 10th after a strike: 0–10
          (and (= ball-in-frame 1) (= 10 (first last-frame)))
          (range 11)

          ;; Second ball of 10th after non-strike: 0 to remaining pins
          (= ball-in-frame 1)
          (range (- 11 (first last-frame)))

          ;; Third ball of 10th after spare (first two sum to 10)
          (and (= ball-in-frame 2)
               (= 10 (+ (first last-frame) (second last-frame))))
          (range 11)

          ;; Third ball of 10th after two strikes
          (and (= ball-in-frame 2)
               (= 10 (first last-frame))
               (= 10 (second last-frame)))
          (range 11)

          ;; Third ball of 10th: remaining from second ball
          (= ball-in-frame 2)
          (range (- 11 (second last-frame)))

          :else (range 11)))
      ;; Frames 1–9
      (let [last-frame   (last frames)
            ball-in-frame (count (:balls last-frame 0))]
        (if (or (nil? last-frame)
                (= (:type last-frame) :strike)
                (= ball-in-frame 2))
          ;; Starting a new frame: any value 0–10
          (range 11)
          ;; Second ball: remaining pins
          (range (- 11 (first (:balls last-frame)))))))))
```

---

## 13. Views: Reagent Components

Views are Reagent components — functions that return hiccup (Clojure data structures representing HTML). They `subscribe` to data and `dispatch` events.

Create `src/myorg/bowling_score/views.cljs`:

```clojure
(ns myorg.bowling-score.views
  (:require [re-frame.core :as rf]))

;; ──────────────────────────────────────────────────────────────────────────────
;; Helper: render a single frame cell header
;; ──────────────────────────────────────────────────────────────────────────────

(defn frame-display
  "Render the roll balls inside a frame header cell."
  [frame is-current?]
  (let [{:keys [balls type frame-num]} frame
        style (when is-current? {:style {:color "#ffd700"}})]
    [:td (merge {:style {:background (if is-current? "#1a3a5c" "#16213e")}} style)
     [:div {:style {:font-size "0.75rem" :color "#aaa"}}
      (str "F" frame-num)]
     [:div
      (cond
        ;; Strike display
        (and (not= frame-num 10) (= type :strike))
        "X"

        ;; Spare display
        (and (not= frame-num 10) (= type :spare))
        (str (first balls) " /")

        ;; 10th frame: show each ball individually
        (= frame-num 10)
        (let [b0 (get balls 0)
              b1 (get balls 1)
              b2 (get balls 2)]
          (str
           (if (= 10 b0) "X" (or b0 "_"))
           (when b1
             (str " "
                  (cond
                    (= 10 b1) "X"
                    (= 10 (+ b0 b1)) "/"
                    :else b1)))
           (when b2
             (str " "
                  (cond
                    (= 10 b2) "X"
                    (= 10 (+ b1 b2)) "/"
                    :else b2)))))

        ;; Open frame
        :else
        (str (or (first balls) "_")
             (when (second balls) (str " " (second balls)))))]]))

;; ──────────────────────────────────────────────────────────────────────────────
;; Scorecard table
;; ──────────────────────────────────────────────────────────────────────────────

(defn scorecard []
  (let [frames        @(rf/subscribe [:frames])
        running-totals @(rf/subscribe [:running-totals])
        current-frame @(rf/subscribe [:current-frame])
        game-over?    @(rf/subscribe [:game-over?])]
    [:div
     [:h2 "Scorecard"]
     [:table
      [:thead
       [:tr
        (for [i (range 1 11)]
          [:th {:key i} (str "Frame " i)])]]
      [:tbody
       ;; Row 1: frame roll display
       [:tr
        (for [i (range 10)]
          (let [frame       (get frames i)
                is-current? (and (not game-over?)
                                 (= (inc i) current-frame))]
            [:td {:key i
                  :style {:background (if is-current? "#1a3a5c" "#16213e")
                          :color      (if is-current? "#ffd700" "#eee")}}
             (if frame
               [frame-display frame is-current?]
               [:span {:style {:color "#555"}} "—"])]))]
       ;; Row 2: running cumulative totals
       [:tr {:class "score-row"}
        (for [i (range 10)]
          [:td {:key i}
           (if-let [t (get running-totals i)]
             t
             "")])]]]]))

;; ──────────────────────────────────────────────────────────────────────────────
;; Pin button panel
;; ──────────────────────────────────────────────────────────────────────────────

(defn pin-buttons []
  (let [valid-pins @(rf/subscribe [:valid-pins])
        game-over? @(rf/subscribe [:game-over?])]
    [:div {:style {:margin-top "1.5rem"}}
     [:h3 "Roll the ball — select pins knocked down:"]
     [:div
      (for [n (range 11)]
        [:button
         {:key      n
          :disabled (or game-over?
                        (not (contains? (set valid-pins) n)))
          ;; dispatch sends the event to re-frame's event queue
          :on-click #(rf/dispatch [:roll-ball n])}
         n])]]))

;; ──────────────────────────────────────────────────────────────────────────────
;; Game-over banner
;; ──────────────────────────────────────────────────────────────────────────────

(defn game-over-banner []
  (let [totals    @(rf/subscribe [:running-totals])
        final     (last (filter some? totals))]
    [:div {:style {:margin-top "1rem"
                   :padding    "1rem"
                   :background "#e94560"
                   :color      "#fff"
                   :font-size  "1.4rem"
                   :border-radius "8px"}}
     (str "🎳 Game Over! Final Score: " (or final 0))]))

;; ──────────────────────────────────────────────────────────────────────────────
;; Root component
;; ──────────────────────────────────────────────────────────────────────────────

(defn app-view []
  (let [game-over? @(rf/subscribe [:game-over?])]
    [:div
     [:h1 "🎳 Bowling Scorecard"]
     [scorecard]
     (if game-over?
       [game-over-banner]
       [pin-buttons])
     [:div {:style {:margin-top "2rem"}}
      [:button {:on-click #(rf/dispatch [:reset-game])
                :style    {:background "#555"}}
       "New Game"]]]))
```

### Key View Patterns

**Subscribing to data:**
```clojure
;; @(rf/subscribe [...]) returns the current value and re-renders when it changes
(let [rolls @(rf/subscribe [:rolls])]
  ...)
```

**Dispatching events:**
```clojure
;; on-click dispatches to re-frame's event queue — never mutate state directly!
[:button {:on-click #(rf/dispatch [:roll-ball 7])} "Roll 7"]
```

**Composing components:**
```clojure
;; Use square brackets to embed a child component (Reagent hiccup syntax)
[scorecard]
[pin-buttons]
```

---

## 14. Wiring It All Together

Here's the final file tree:

```
bowling-score/
├── deps.edn
├── shadow-cljs.edn
├── package.json
├── package-lock.json
├── node_modules/             ← generated by npm install
├── resources/
│   └── public/
│       └── index.html
└── src/
    └── myorg/
        └── bowling_score/
            ├── core.cljs     ← app entry point, mounts Reagent
            ├── db.cljs       ← default-db definition
            ├── events.cljs   ← reg-event-db handlers
            ├── subs.cljs     ← reg-sub subscriptions
            ├── logic.cljs    ← pure bowling scoring logic
            └── views.cljs    ← Reagent components
```

### Data Flow Recap (Bowling Edition)

```
User clicks "7"
   │
   ▼
[:roll-ball 7] dispatched
   │
   ▼
:roll-ball event handler runs
  (assoc db :rolls [... 7] :game-over? false)
   │
   ▼
app-db atom is swapped to new value
   │
   ├──▶ :rolls subscription recalculates → [... 7]
   │       │
   │       ├──▶ :frames sub recalculates
   │       ├──▶ :frame-scores sub recalculates
   │       ├──▶ :running-totals sub recalculates
   │       └──▶ :valid-pins sub recalculates
   │
   └──▶ :game-over? subscription recalculates → false
   │
   ▼
Reagent re-renders only the components whose subscriptions changed
```

---

## 15. Running and Testing the App

### Start the development server

```bash
npx shadow-cljs watch app
```

shadow-cljs will:

1. Compile your ClojureScript
2. Start an HTTP server at `http://localhost:8080`
3. Enable hot code reloading (changes are pushed to the browser without a page refresh)

Open your browser to `http://localhost:8080`. You should see the bowling scorecard.

### Hot Reload in Action

Edit a component in `views.cljs` — for example change the `h1` text. Save the file. shadow-cljs recompiles and pushes the change. Notice that **app state is preserved** — the rolls you've entered remain. This is one of re-frame's biggest development-time advantages.

### Production Build

```bash
npx shadow-cljs release app
```

This produces an optimized, minified `resources/public/js/compiled/main.js` ready for deployment.

### Running Tests

For CLJS unit tests of the pure logic namespace, add a test build to `shadow-cljs.edn`:

```clojure
:test {:target    :node-test
       :output-to "out/test/test.js"
       :ns-regexp ".*-test$"}
```

Create `test/myorg/bowling_score/logic_test.cljs`:

```clojure
(ns myorg.bowling-score.logic-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [myorg.bowling-score.logic :as logic]))

(deftest perfect-game
  (testing "300 is a perfect game"
    (let [rolls (vec (repeat 12 10))]
      (is (= [300] (take-last 1 (filter some? (logic/running-totals (logic/frame-scores rolls)))))))))

(deftest all-gutter-game
  (testing "All gutter balls score 0"
    (let [rolls (vec (repeat 20 0))]
      (is (= 0 (last (filter some? (logic/running-totals (logic/frame-scores rolls)))))))))

(deftest all-spares
  (testing "5-spare game scores 150"
    (let [rolls (vec (repeat 21 5))]
      (is (= 150 (last (filter some? (logic/running-totals (logic/frame-scores rolls)))))))))
```

Run tests:

```bash
npx shadow-cljs compile test
node out/test/test.js
```

---

## 16. Summary and Next Steps

### What You've Learned

| re-frame Concept | Where It Appeared |
|-----------------|-------------------|
| `app-db` / `default-db` | `db.cljs` — single map representing all state |
| `reg-event-db` | `events.cljs` — `:initialize-db`, `:roll-ball`, `:reset-game` |
| `dispatch` / `dispatch-sync` | `core.cljs`, `views.cljs` |
| `reg-sub` (layer 2) | `subs.cljs` — `:rolls`, `:game-over?` |
| `reg-sub` with `:<-` (layer 3) | `subs.cljs` — `:frames`, `:running-totals`, `:valid-pins` |
| `subscribe` + deref | `views.cljs` — every component |
| Reagent hiccup + component composition | `views.cljs` |

### Concepts to Explore Next

1. **`reg-event-fx`** — event handlers that trigger side effects (HTTP, localStorage)
2. **`re-frame-http-fx`** — persist game scores to a server
3. **Interceptors** — middleware for events (logging, schema validation with Malli)
4. **`reg-fx` / `reg-cofx`** — define your own effects and coeffects
5. **`re-frame-10x`** — a development panel that shows every event and db snapshot, invaluable for debugging
6. **`spec` / `malli` validation** — validate app-db shape after every event in development
7. **Testing event handlers** — since handlers are pure functions, they're trivially unit-testable without any UI

### A Note on Architecture as the App Grows

As features accumulate, split your namespaces further:

```
src/myorg/bowling_score/
├── core.cljs
├── db.cljs
├── logic.cljs
├── events/
│   ├── game.cljs       ;; game-flow events
│   └── player.cljs     ;; player management events
├── subs/
│   ├── game.cljs
│   └── player.cljs
└── views/
    ├── scorecard.cljs
    ├── controls.cljs
    └── layout.cljs
```

Re-frame scales cleanly because each namespace has a single, clear responsibility and all coupling runs through the app-db + dispatch interface.

---

*Happy bowling — and happy hacking!* 🎳
