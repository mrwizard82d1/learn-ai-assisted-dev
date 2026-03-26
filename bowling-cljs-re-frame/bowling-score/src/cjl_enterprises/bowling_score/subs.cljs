(ns cjl-enterprises.bowling-score.subs
  (:require [re-frame.core :as rf]))

;; Subscriptions

;; Layer 2 subscription: extracts data directly from `app-db`.
;; The handler receives the whole db map and returns whatever the
;; view needs.
(rf/reg-sub
 :rolls
 (fn [db _query]
     (:rolls db)))

(rf/reg-sub
 :game-over?
 (fn [db _query]
   (:game-over? db)))
