(ns everyday-chemist.view
  (:require [lanterna.screen :as s]
            [clojure.core.async :as a]
            [clojure.java.browse :refer [browse-url]]
            [clojure.string :as string]
            [everyday-chemist.model :as m]
            [everyday-chemist.utils :as utils]))

(defonce screen-size (atom [0 0]))
(defonce current-menu (atom :main-menu))
(defonce active-line (atom 0))
(defonce offset (atom 0))
(def screen (s/get-screen :text))
(defonce colors  {:fg          :white
                  :bg          :black
                  :fg-selected :black
                  :bg-selected :yellow})

(defn fmt-line
  "line: line to format
   max-len: maximum length of line 
   formats line by padding with spaces or truncating with ..."
  [line max-len]
  (if (<= (count line) max-len)
    (str line
         (.repeat " " (- max-len (count line))))
    (str
     (subs line 0 (- max-len 3))
     "...")))

(defn refresh
  "scr: screen on which to display
  state: current app model
  feed: display the entries in current feed if present"
  []
  (let [buffer (if (= @current-menu :main-menu)
                 (keys @m/feeds)
                 (map :title (get @m/feeds @current-menu)))]
    (s/clear screen)
    (loop [b (drop @offset buffer)
           i 0]
      (when-not (empty? b)
        (s/put-string screen 0 i (fmt-line (first b) (first @screen-size))
                           {:fg (if (= i @active-line) (:fg-selected colors) (:fg colors))
                            :bg (if (= i @active-line) (:bg-selected colors) (:bg colors))})
        (recur (rest b) (inc i)))))

  (s/redraw screen)
  (.setCursorVisible (.getTerminal screen) false))

;; TODO: remember position in main-menu
(defn back []
  (reset! current-menu :main-menu)
  (reset! active-line 0)
  (refresh))

(defn move [dir]
  ;; TODO: this is disgusting, write predicates to clean this up
  (let [buffer (if (= :main-menu @current-menu)
                 @m/feeds
                 (get @m/feeds @current-menu))
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
  (refresh))

(defn select []
  (if (= :main-menu @current-menu) 
    (let [selection (nth (keys @m/feeds) (+ @active-line @offset))] 
      (reset! active-line 0)
      (reset! offset 0)
      (reset! current-menu selection)
      (refresh))
    ;; TODO: downloading / browsing needs to be more robust
    (let [selection (nth (get-in @m/feeds [@current-menu :entries]) (+ @active-line @offset))
          enclosure (first (get selection :enclosures))] ; TODO: download all enclosures?
      (if (and enclosure (:type enclosure) (string/includes? (:type enclosure) "audio"))
        (a/thread (utils/download (:url enclosure)))
        (browse-url (:uri selection))))))

(defn stop []
  (s/stop screen))

(defn handle-resize [x y]
  (reset! screen-size [x y])
  (when (> @active-line (dec y))
    (reset! active-line (dec y)))
  (refresh))

(defn get-key-blocking []
  (s/get-key-blocking screen))

(defn init []
  ; TODO: find out why this causes swing term to hang
  (s/add-resize-listener screen handle-resize)
  (s/start screen)
  (reset! screen-size (s/get-size screen))
  (m/register-listener refresh))
