(ns everyday-chemist.model)

;; feeds are of form {"" : [{},{},...]} 
(defonce feeds (atom {}))
(defonce listeners (atom #{}))

;; TODO: is this what agents are for?
(defn register-listener
  "Listeners should be functions that 1 argument to be called when feeds is updated"
  [l]
  (swap! listeners conj l))

(defn execute-listeners
  []
  ;; TODO: this looks dumb, find the idiomatic way to do this
  (doseq [l @listeners]
    (l)))

;; TODO: find a way to only add new entries to the feed
(defn update-feed
  [url f]
  (swap! feeds assoc url f)
  (execute-listeners))
