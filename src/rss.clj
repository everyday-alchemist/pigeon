(ns rss
  (:require [remus :refer [parse-url]]))

(defn example [url]
  (map :title (-> (parse-url url)
                  :feed
                  :entries)))

(defn get-feed [url]
  (:feed (parse-url url)))

(defn -main [& _]
  (println (parse-url "http://planet.clojure.in/atom.xml")))
