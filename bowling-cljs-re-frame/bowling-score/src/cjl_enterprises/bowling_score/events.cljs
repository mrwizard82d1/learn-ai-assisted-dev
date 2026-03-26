(ns cjl-enterprises.bowling-score.events
  (:require [re-frame.core :as rf]
            [cjl-enterprises.bowling-score.db :as db]))

;; `reg-event-db` registers a handler for an event id. The handler is a
;; pure function mapping (current-db, event-vector) => new-db.
(rf/reg-event-db
 :initialize-db
 (fn [_db _event]
   db/default-db))

;; [:roll-bal n] - record that the player knocked down n pins.
;; No bowling logic yet; we simply append the number of pins knocked
;; down to the `:rolls` vector in the application database.
(rf/reg-event-db
 :roll-ball
 (fn [db [_event_id pins]]
   (update db :rolls conj pins)))
