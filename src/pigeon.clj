(ns pigeon
  (:require [clojure.java.browse :refer [browse-url]]
            [rss]
            [ui]))

(def feeds (atom {}))

(defn add-feed [url]
  (swap! feeds assoc url (rss/get-feed url)))

(add-feed "http://planet.clojure.in/atom.xml")

(defn feed->buff [name back]
  (ui/clear-buffer)
  (let [entries (get-in @feeds [name :entries])]
    (doseq [entry entries]
      (ui/->buffer (:title entry) #(browse-url (:uri entry)) back)))
  (ui/draw-buffer))

(defn menu->buff []
  (ui/clear-buffer)
  (doseq [name (keys @feeds)]
    (ui/->buffer name #(feed->buff name (fn [] (menu->buff))) #(ui/quit)))
  (ui/draw-buffer))

(defn -main [& _]
  (ui/init-screen)
  (menu->buff)
  ;; should probably refactor so this is called by ui (or make it its own thread)
  (ui/listen))
