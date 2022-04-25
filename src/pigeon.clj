(ns pigeon
  (:require [clojure.java.browse :refer [browse-url]]
            [rss]
            [ui]
            [config]))

(def feeds (atom {}))

(defn add-feed [url]
  (swap! feeds assoc url (rss/get-feed url)))

(defn feed->buff [name back]
  (ui/clear-buffer)
  (ui/reset-active)
  (let [entries (get-in @feeds [name :entries])]
    (doseq [entry entries]
      (ui/->buffer (:title entry) #(browse-url (:uri entry)) back)))
  (ui/draw-buffer))

(defn menu->buff []
  (ui/clear-buffer)
  (ui/reset-active)
  (doseq [name (keys @feeds)]
    (ui/->buffer name #(feed->buff name (fn [] (menu->buff))) #(ui/quit)))
  (ui/draw-buffer))

(defn -main [& _]
  (let [conf-loc (if (System/getenv "XDG_CONFIG_HOME")
                   (System/getenv "XDG_CONFIG_HOME")
                   (System/getenv "HOME"))
        conf (config/load-config (str conf-loc "/pigeon/config.edn"))]
    (spit "log.txt" conf)
    (doseq [url (:urls conf)]
      (add-feed url))
    (ui/set-colors (:colors conf))
    (ui/set-keymap (:keymap conf))

    (ui/init-screen)
    (menu->buff)
    ;; should probably refactor so this is called by ui (or make it its own thread)
    (ui/listen)))
