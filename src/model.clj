(ns model
  (:require [clojure.core.async :refer [thread]]
            [clojure.java.browse :refer [browse-url]]
            [config]
            [utils]
            [remus :refer [parse-url]]
            [view]
            [clojure.string :as string]))

(def screen (atom nil))
(def screen-size (atom []))
(def active-line (atom 0))
(def offset (atom 0))
(def feeds (atom {}))
(def current-menu (atom :main-menu))
(def keymap (atom {\h :back
                   \j :scroll-down
                   \k :scroll-up
                   \l :select
                   \q :quit}))
(def colors (atom {:fg          :white
                   :bg          :black
                   :fg-selected :black
                   :bg-selected :yellow}))

; TODO: This is probably a control function
(defn quit []
  (.setCursorVisible (.getTerminal @screen) false)
  (view/stop @screen)
  (java.lang.System/exit 0))

(defn redraw
  []
  (view/display {:screen @screen
                 :screen-size @screen-size
                 :offset @offset
                 :active-line @active-line
                 :colors @colors
                 :feeds @feeds
                 :current-menu @current-menu}))

; would it be better to do this in bulk?
(defn add-entry
  [feed-url entry]
  (get @feeds feed-url)
  (let [curr (get @feeds feed-url)] ; TODO: only add unique entries
    (swap! feeds assoc feed-url (conj curr entry))))
 
; maybe make this a util?
(defn fetch-entries
  [url]
  (let [f (:feed (parse-url url))]
    (doseq [entry (:entries f)]
      (add-entry url entry))))

(defn add-feed
  ; TODO: merge feed instead of overwrite, feeds should be a map
  [url]
  ; Add feed to feeds immediately so it shows up in menu
  (when-not (get @feeds url)
    (swap! feeds assoc url [])
    (redraw))
  (thread 
    (fetch-entries url)
    (redraw)))

(defn select []
  (if (= :main-menu @current-menu) 
    (let [selection (nth (keys @feeds) (+ @active-line @offset))] 
      (reset! active-line 0)
      (reset! offset 0)
      (reset! current-menu selection)
      (redraw))
    ;; TODO: downloading / browsing needs to be more robust
    (let [selection (nth (get @feeds @current-menu) (+ @active-line @offset))
          enclosure (first (get selection :enclosures))] ; TODO: download all enclosures?
      (if (and enclosure (:type enclosure) (string/includes? (:type enclosure) "audio"))
        (thread (utils/download (:url enclosure)))
        (browse-url (:uri selection))))))

(defn back []
  (when-not (= :main-menu @current-menu)
    (reset! active-line 0) ;; TODO: remember place in main-menu
    (reset! current-menu :main-menu)
    (redraw)))

(defn move [dir]
  ;; TODO: this is disgusting, write predicates to clean this up
  (let [buffer (if (= :main-menu @current-menu)
                 @feeds
                 (get @feeds @current-menu))
        buf-size (count buffer)]
    (cond
      (and (= :up dir) (not= 0 @active-line))
      (swap! active-line dec)
      (and (= :up dir) (= 0 @active-line) (not= 0 @offset))
      (swap! offset dec)
      (and (= :up dir) (= 0 @active-line) (= 0 @offset))
      nil
      (and (= :down dir) (or (= (dec (second @screen-size)) buf-size) (= @active-line (dec buf-size))))
      nil
      (and (= :down dir) (not= (dec (second @screen-size)) @active-line) (< @active-line (dec buf-size)))
      (swap! active-line inc)
      (and (= :down dir) (= (dec (second @screen-size)) @active-line) (not= (+ @active-line @offset) (dec buf-size)))
      (swap! offset inc)
      (and (= :down dir) (= (dec (second @screen-size)) @active-line) (= (+ @active-line @offset) (dec buf-size)))
      nil
      :else
      nil))
  (redraw))

; TODO: should we be wrapping all this?
(defn get-key []
  (get @keymap
       (view/get-key-blocking @screen)))

(defn init [& conf-name]
  (reset! screen
          ;; TODO: use config?
          (view/init-screen :text
                            (fn [x y]
                              (reset! screen-size [x y])
                              (when (> @active-line (dec y))
                                (reset! active-line (dec y)))
                              (view/clear-screen @screen)
                              (redraw))))
  (view/start-screen @screen)
  (reset! screen-size (view/get-size @screen))
  (view/clear-screen @screen)
  ;; probably need update feed function
  (let [conf-loc (if (System/getenv "XDG_CONFIG_HOME")
                 (System/getenv "XDG_CONFIG_HOME")
                 (System/getenv "HOME"))
      conf (if (first conf-name)
             (config/load-config (first conf-name))
             (config/load-config (str conf-loc "/pigeon/config.edn")))]
    ;; TODO: should thread be moved around get-feed and db/instert-feed in model.clj?
    (doseq [url (:urls conf)]
      (add-feed url))))
