(ns everyday-chemist.view
  (:require [lanterna.screen :as s]
            [clojure.core.async :as a]
            [clojure.java.browse :refer [browse-url]]
            [clojure.string :as string]
            [everyday-chemist.model :as m]
            [everyday-chemist.utils :as utils]))

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
  [{:keys [screen
           screen-size
           current-menu
           active-line
           offset
           colors]
    :as state}]
  (let [buffer (if (= current-menu :main-menu)
                 (keys @m/feeds)
                 (map :title (get-in @m/feeds [current-menu :entries])))]
    (s/clear screen)
    (loop [b (drop offset buffer)
           i 0]
      ;; if we still have strings, write them to the screen, else write blank line
      (if b
        (s/put-string screen 0 i (fmt-line (first b) (first screen-size))
                           {:fg (if (= i active-line) (:fg-selected colors) (:fg colors))
                            :bg (if (= i active-line) (:bg-selected colors) (:bg colors))})
        (s/put-string screen 0 i (fmt-line "" (first screen-size))
                      {:fg (:fg colors)
                       :bg (:bg colors)}))
      ;; continue looping until reaching the max line on screen
      (when (< i (second screen-size)) 
        (recur (next b) (inc i)))))

  (s/redraw screen)
  (.setCursorVisible (.getTerminal screen) false)
  state)

;; TODO: remember position in main-menu
(defn back [state]
  (-> state
      (assoc :current-menu :main-menu)
      (assoc :active-line 0)
      (assoc :offset 0)
      (refresh)))

(defn move 
  [{:keys [screen
           screen-size
           current-menu
           active-line
           offset
           colors]
    :as state}
   dir]
  ;; TODO: this is disgusting, write predicates to clean this up
  (let [buffer (if (= :main-menu current-menu)
                 @m/feeds
                 (get-in @m/feeds [current-menu :entries]))
        buf-size (count buffer)
    ; TODO: this is just ridiculous now
        fun (cond
              (and (= :up dir) (not= 0 active-line))
              #(update % :active-line dec)
              (and (= :up dir) (= 0 active-line) (not= 0 offset))
              #(update % :offset dec)
              (and (= :up dir) (= 0 active-line) (= 0 offset))
              identity
              (and (= :down dir) (or (= (dec (second screen-size)) buf-size) (= active-line (dec buf-size))))
              identity
              (and (= :down dir) (not= (dec (second screen-size)) active-line) (< active-line (dec buf-size)))
              #(update % :active-line inc)
              (and (= :down dir) (= (dec (second screen-size)) active-line) (not= (+ active-line offset) (dec buf-size)))
              #(update % :offset inc)
              (and (= :down dir) (= (dec (second screen-size)) active-line) (= (+ active-line offset) (dec buf-size)))
              identity
              :else
              identity)]
    (-> state
      (fun)
      (refresh))))


(defn init []
  (let [screen (s/get-screen :text)]
    (s/start screen)
    {:screen screen
     :screen-size (s/get-size screen)
     :current-menu :main-menu
     :active-line 0
     :offset 0
     :colors {:fg :white :bg :black :fg-selected :black :bg-selected :yellow}}))

(defn select
  [{:keys [screen
           screen-size
           current-menu
           active-line
           offset
           colors]
    :as state}]
  (if (= :main-menu current-menu) 
    (let [selection (nth (keys @m/feeds) (+ active-line offset))] 
      (-> state 
          (assoc :active-line 0)
          (assoc :offset 0)
          (assoc :current-menu selection)
          (refresh)))
    ;; TODO: downloading / browsing needs to be more robust
    (let [selection (nth (get-in @m/feeds [current-menu :entries]) (+ active-line offset))
          enclosure (first (get selection :enclosures))] ; TODO: download all enclosures?
      (if (and enclosure (:type enclosure) (string/includes? (:type enclosure) "audio"))
        (a/thread (utils/download (:url enclosure)))
        (browse-url (:uri selection)))
      state)))

(defn stop [{:keys [screen] :as state}]
  (s/stop screen)
  state)

(defn get-key-blocking 
  [{:keys [screen]}]
  (s/get-key-blocking screen))

(defn add-resize-listener
  [screen f]
  (s/add-resize-listener screen f))

(defn handle-resize [state x y]
  (swap! state assoc :screen-size [x y])
  (when (> (get @state :active-line) (dec y))
    (swap! state assoc :active-line (dec y)))
  (refresh @state))
