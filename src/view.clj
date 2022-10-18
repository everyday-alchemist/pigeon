(ns view
  (:require [lanterna.screen :as screen]
            [clojure.pprint :refer [pprint]]))

(defn init-screen
  "Returns a screen of type term"
  [term f]
  (screen/get-screen term f))

(defn stop
  [s]
  (screen/stop s))

(defn start-screen
  [s]
  (screen/start s))

(defn get-key-blocking
  [s]
  (screen/get-key-blocking s))

(defn add-resize-listener
  [scr f]
  (screen/add-resize-listener scr f))

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

(defn refresh-screen
  [scr]
  (screen/redraw scr)
  (.setCursorVisible (.getTerminal scr) false))

(defn display
  "scr: screen on which to display
  state: current app model
  feed: display the entries in current feed if present"
  [{:keys [screen screen-size offset active-line colors feeds current-menu]}]
  (let [buffer (if (= current-menu :main-menu)
                 (keys feeds)
                 (map :title (get feeds current-menu)))]
    (screen/clear screen)
    (loop [b (drop offset buffer)
           i 0]
      (when-not (empty? b)
        (screen/put-string screen 0 i (fmt-line (first b) (first screen-size))
                           {:fg (if (= i active-line) (:fg-selected colors) (:fg colors))
                            :bg (if (= i active-line) (:bg-selected colors) (:bg colors))})
        (recur (rest b) (inc i)))))

  (refresh-screen screen))

(defn get-size [scr]
  (screen/get-size scr))

(defn clear-screen
  [scr]
  (screen/clear scr))
