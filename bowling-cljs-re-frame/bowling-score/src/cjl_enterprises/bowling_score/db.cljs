(ns cjl-enterprises.bowling-score.db)

(def default-db
  {:rolls [] ;; A vector of pins knocked down during each roll
   :game-over? false})
