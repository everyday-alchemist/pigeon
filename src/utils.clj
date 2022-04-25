(ns utils)

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
