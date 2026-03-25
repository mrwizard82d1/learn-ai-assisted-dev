(ns cjl-enterprises.bowling-score.core
  (:require [reagent.dom :as rdom]))

;; A Reagent component is just a function that return hiccup.
(defn app-view []
  [:div
   [:h1 "🎳 Bowling Scorecard"]
   [:p "Reagent is still rendering this HTML."]])

(defn mount-root []
  ;; rdom/render takes a component and a DOM node
  (rdom/render [app-view]
               (.getElementById js/document "app")))

(defn init []
  (mount-root))

(defn ^:dev/after-load start []
  ;; Re-mount after hot reload so new component code takes effect
  (mount-root))
