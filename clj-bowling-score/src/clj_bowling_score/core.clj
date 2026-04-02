(ns clj-bowling-score.core)

(def frames {:frames [{:rolls [0 0]}
                      {:rolls [0 7]}
                      {:rolls [3 0]}]})

(defn get-frame [frames frame-ndx]
  (get (:frames frames) frame-ndx))

(defn score-frame [frame]
  (let [rolls (:rolls frame)]
    (apply + rolls)))

(defn spare? [frame]
  false)
