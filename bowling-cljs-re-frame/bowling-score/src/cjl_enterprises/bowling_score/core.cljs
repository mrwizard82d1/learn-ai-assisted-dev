(ns cjl-enterprises.bowling-score.core
  (:require [goog.dom :as gdom]
            [reagent.core :as r]
            [reagent.dom.client :as rdc]))

;; This implementation is based upon the prompt to Chrome / Gemini:
;; "clojurescript create a simple reagent application example.
;; use shadow-cljs, React 18, reagent.dom.client/create-root and after-load"
;;
;; To test live reloading, change the content of th paragraph tag (`:p`)
;; in `app-view` and see the new text rendered in the browser with no
;; additional intervention

;; A Reagent component is just a function that return hiccup.
(defn app-view []
  [:div
   [:h1 "🎳 Bowling Scorecard"]
   [:p "Reagent is again rendering this HTML."]])

;; Define root renderer for React18 using `defonce` so it is only created
;; one time
(defonce root
  (rdc/create-root (gdom/getElement "app")))

(defn render-app []
  (.render root (r/as-element [app-view])))

(defn init
  "The entry point for the application. Called once on initial load"
  []
  (render-app))

(defn ^:dev/after-load re-render
  "Called by shadow-cljs after code hot-reloads during development."
  []
  (render-app)
  (js/console.log "App re-rendered due to code change."))
