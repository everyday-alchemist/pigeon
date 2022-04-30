(ns pigeon
  (:require [clojure.java.browse :refer [browse-url]]
            [clojure.string :as string]
            [rss]
            [utils]
            [ui]
            [config]
            [db])
  (:import [java.text DateFormat]))

(def feeds (atom {}))

(defn add-feed [url]
  (let [feed (rss/get-feed url)]
    (db/insert-feed-if-unique  url)
    (db/insert-entry-if-unique {:title (:title feed)
                                :uri (:uri feed)
                                :feed_url url})
    (swap! feeds assoc url feed)))

;; TODO: refactor to somewhere else
(defn format-date [date]
  ; change this to something more elegant
  (when date
    (let [s (.format (DateFormat/getDateInstance) date)]
      (if (< (count s) 12)
        (str s " ")
        s))))

;; TODO: This should be a ui function
(defn feed->buff [name back]
  (ui/clear-buffer)
  (ui/reset-active)
  (let [entries (get-in @feeds [name :entries])]
    (doseq [entry entries]
      (let [action (if (and (:type entry) (string/includes? (:type entry) "audio"))
                     #(utils/download (:uri entry))
                     #(browse-url (:uri entry)))]
        (ui/->buffer (str (format-date (:published-date entry)) " " (:title entry))
                     action
                     back))))
  (ui/draw-buffer))

(defn menu->buff []
  (ui/clear-buffer)
  (ui/reset-active)
  (doseq [name (keys @feeds)]
    (ui/->buffer name #(feed->buff name (fn [] (menu->buff))) #(ui/quit)))
  (ui/draw-buffer))

(defn -main [& conf-name]
  (let [conf-loc (if (System/getenv "XDG_CONFIG_HOME")
                   (System/getenv "XDG_CONFIG_HOME")
                   (System/getenv "HOME"))
        conf (if conf-name
               (config/load-config (first conf-name))
               (config/load-config (str conf-loc "/pigeon/config.edn")))]
    (db/create-tables)
    (doseq [url (:urls conf)]
      (add-feed url))
    (ui/set-colors (:colors conf))
    (ui/set-keymap (:keymap conf))
    (ui/init-screen (:terminal conf))

    (menu->buff)
    ;; should probably refactor so this is called by ui (or make it its own thread)
    (ui/listen)))
