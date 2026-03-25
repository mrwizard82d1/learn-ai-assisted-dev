(ns cjl-enterprises.bowling-score.events
  (:require [re-frame.core :as rf]
            [cjl-enterprises.bowling-score.db :as db]))

;; `reg-event-db` registers a handler for an event id. The handler is a
;; pure function mapping (current-db, event-vector) => new-db.
(rf/reg-event-db
 :initialize-db
 (fn [_db _event]
   db/default-db))
