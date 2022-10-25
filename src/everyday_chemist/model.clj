(ns everyday-chemist.model
  (:require [clojure.set :refer [union]]))

;; feeds are of form {"" : [{},{},...]} 
(defonce feeds (atom {}))
(defonce listeners (atom #{}))

(defn register-listener
  "Listeners should be functions that 1 argument to be called when feeds is updated"
  [l]
  (swap! listeners conj l))

(defn execute-listeners
  []
  ;; TODO: this looks dumb, find the idiomatic way to do this
  (doseq [l @listeners]
    (l)))

(defn update-feed
  [url f]
  (let [old-entries (get-in @feeds [url :entries])
        new-entries (get f :entries)
        comb-entries (union (set old-entries) (set new-entries))
        new-feed (assoc f :entries comb-entries)
        comb-feed (merge (get @feeds url) new-feed)]
    (swap! feeds assoc url comb-feed)
    (execute-listeners)))
