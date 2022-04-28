(ns utils
 (:require [clojure.java.io :refer [input-stream output-stream copy]]))

(defn download [url]
  (with-open [in (input-stream url)
              out (output-stream "file")]
    (copy in out)))

;; TODO: is this even necessary anymore?
(defn superimpose
  [from to]
  (reduce (fn [acc k]
            (if (and (= (type {}) (type (get to k)))
                     (contains? to k))
              (assoc acc k (superimpose (get from k) (get to k)))
              (assoc acc k (get from k))))
          to
          (keys from)))
