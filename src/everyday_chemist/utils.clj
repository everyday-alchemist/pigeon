(ns everyday-chemist.utils
  (:require [clojure.java.io :refer [input-stream output-stream copy]]
            [everyday-chemist.config :as config]))

(defn url->name
  "get filename from a url using evil regex modified from stackoverflow lmao
   need to implement a check for a unique name and make it elegant"
  [url]
  (let [n (first (re-find #"[^\/\\&\?]+\.\w+(?=([\?&].*$|$))" url))]
    (if n
      n
      (str "pigeon-download-" (rand-int 100000)))))

;; TODO: make config work well
(defn download [url]
  (with-open [in (input-stream url)
              out (output-stream (str (:download-directory config/default-config)
                                      "/"
                                      (url->name url)))]
    (copy in out)))
