(ns clj-bowling-score.core-test
  (:require [clojure.test :refer :all]
            [clj-bowling-score.core :refer :all]))

(def test-frames
  {:frames [{:rolls [0 0]}   ; index 0 – gutter ball
            {:rolls [0 7]}   ; index 1 – partial frame
            {:rolls [3 0]}]}) ; index 2 – partial frame

;; --- get-frame ---

(deftest get-frame-returns-correct-frame
  (testing "returns the frame at the given index"
    (is (= {:rolls [0 0]} (get-frame test-frames 0)))
    (is (= {:rolls [0 7]} (get-frame test-frames 1)))
    (is (= {:rolls [3 0]} (get-frame test-frames 2)))))

(deftest get-frame-returns-nil-for-out-of-bounds
  (testing "returns nil when index is out of bounds"
    (is (nil? (get-frame test-frames 99)))
    (is (nil? (get-frame test-frames -1)))))

(deftest get-frame-returns-nil-for-empty-game
  (testing "returns nil when frames list is empty"
    (is (nil? (get-frame {:frames []} 0)))))

;; --- score-frame ---

(deftest score-frame-gutter-ball
  (testing "scores zero for two gutter balls"
    (is (= 0 (score-frame {:rolls [0 0]})))))

(deftest score-frame-partial
  (testing "scores the sum of two rolls"
    (is (= 7 (score-frame {:rolls [0 7]})))
    (is (= 3 (score-frame {:rolls [3 0]})))
    (is (= 9 (score-frame {:rolls [4 5]})))))

(deftest score-frame-max-non-bonus
  (testing "scores 10 for a spare (no bonus applied here)"
    (is (= 10 (score-frame {:rolls [7 3]})))))

;; --- spare? ---

(deftest spare-returns-true-for-spare
  (testing "returns true when two rolls sum to 10"
    (is (true? (spare? {:rolls [7 3]})))))


(run-tests)
