(ns cjl-enterprises.bowling-score.core
  (:require [goog.dom :as gdom]
            [reagent.core :as r]
            [reagent.dom.client :as rdc]
            [re-frame.core :as rf]
            [cjl-enterprises.bowling-score.events]
            [cjl-enterprises.bowling-score.subs]))

;; This implementation is based upon the prompt to Chrome / Gemini:
;; "clojurescript create a simple reagent application example.
;; use shadow-cljs, React 18, reagent.dom.client/create-root and after-load"
;;
;; To test live reloading, change the content of th paragraph tag (`:p`)
;; in `app-view` and see the new text rendered in the browser with no
;; additional intervention

;; A Reagent component is just a function that return hiccup.
(defn app-view []
  ;; `@(rf/subscripb [...])` returns the current value.
  ;; Reagent tracks this dereference and re-renders when the value
  ;; changes.
  (let [rolls @(rf/subscribe [:rolls])]
    [:div
     [:h1 "🎳 Bowling Scorecard"]
     [:p "Click a button to roll the ball"]
     ;; one can invoke the function
     ;; ```clojure
     ;; cljs.core.clj__gt_js(re_frame.db.app_db.state)
     ;; ```
     ;; in the javascript debug console or one can add the following
     ;; debug output line.
     ;;
     ;; note that directly dereferencing the `app-db` atom is fine for
     ;; debugging, it is **not** how to read state in a production
     ;; environment. in a production environment, use a subscription
     ;; but we've not (yet) learned about subscriptions (upcoming
     ;; attractions).
     [:p (str "Rolls so far: " (pr-str rolls))]
     ;; render one button for each possible pin count (0 - 10)
     (for [n (range 11)]
       [:button
        {;; react requires a unique `:key` in lists
         :key n
         ;; dispatch the `:roll-ball` event [with the number of pins
         ;; knocked down)
         :on-click #(rf/dispatch [:roll-ball n])
         :style {:margin "4px"
                 :padding "6px 12px" ;; top-bottom and left-right?
                 :cursor "pointer"}}
        n])]))

;; Define root renderer for React18 using `defonce` so it is only created
;; one time
(defonce root
  (rdc/create-root (gdom/getElement "app")))

(defn render-app []
  (.render root (r/as-element [app-view])))

(defn init
  "The entry point for the application. Called once on initial load"
  ;; `dispatch-sync`: runs the handler immediately (synchronously).
  ;; Use this function at start-up so that the database is available
  ;; before the first render.
  []
  (rf/dispatch-sync [:initialize-db])
  (render-app))

(defn ^:dev/after-load re-render
  "Called by shadow-cljs after code hot-reloads during development."
  []
  (render-app)
  (js/console.log "App re-rendered due to code change."))
