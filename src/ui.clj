(ns ui
  (:require [lanterna.screen :as screen]
            [rss]))

(def scr (atom nil)) ;TODO: refactor to local?
(def scr-size (atom []))
(def offset (atom 0))
(def active-line (atom 0))
(def colors (atom {:fg :white
                   :bg :black
                   :fg-selected :black
                   :bg-selected :yellow}))
(def keymap (atom {\h :back
                   \j :scroll-down
                   \k :scroll-up
                   \l :select
                   \q :quit}))

;; buf is a list of strings representing the text on the screen, indexed by line number
(def buf (atom []))

(defn quit []
  (.setCursorVisible (.getTerminal @scr) false)
  (screen/stop @scr)
  (java.lang.System/exit 0))

;; TODO: these can probably be removed but its late and I want to make this commit
(defn set-colors [m]
  (doseq [[k v] m]
    (swap! colors assoc k v)))

(defn set-keymap [m]
  (map #(swap! keymap assoc % (get m %)) (keys m)))

(defn reset-active []
  (reset! active-line 0))

(defn pad-str [s size]
  (if (<= (count s) size)
    (str s
         (.repeat " " (- size (count s))))
    (str
     (subs s 0 (- size 3))
     "...")))

(defn refresh
  "wrap redraw so we hide the cursor every time
   Cursors? Where we're going we don't need cursors."
  []
  (screen/redraw @scr)
  (.setCursorVisible (.getTerminal @scr) false))

(defn ->buffer
  [text f back]
  (swap! buf conj {:text text :fn f :back back}))

(defn clear-buffer
  []
  (reset! buf []))

(defn draw-buffer
  []
  (screen/clear @scr)

  ;; TODO: This can probably be done without loop
  (loop [b (drop @offset @buf)
         i 0]
    (when-not (empty? b)
      (screen/put-string @scr 0 i (pad-str (:text (first b)) (first @scr-size))
                         {:fg (if (= i @active-line) (:fg-selected @colors) (:fg @colors))
                          :bg (if (= i @active-line) (:bg-selected @colors) (:bg @colors))})
      (recur (rest b) (inc i))))

  (refresh))

(defn move [dir]
  ;; TODO: this is disgusting, write predicates to clean this up
  (let [buf-size (count @buf)]
    (cond
      (and (= :up dir) (not= 0 @active-line))
      (swap! active-line dec)
      (and (= :up dir) (= 0 @active-line) (not= 0 @offset))
      (swap! offset dec)
      (and (= :up dir) (= 0 @active-line) (= 0 @offset))
      nil
      (and (= :down dir) (or (= (dec (second @scr-size)) buf-size) (= @active-line (dec buf-size))))
      nil
      (and (= :down dir) (not= (dec (second @scr-size)) @active-line) (< @active-line (dec buf-size)))
      (swap! active-line inc)
      (and (= :down dir) (= (dec (second @scr-size)) @active-line) (not= (+ @active-line @offset) (dec buf-size)))
      (swap! offset inc)
      (and (= :down dir) (= (dec (second @scr-size)) @active-line) (= (+ @active-line @offset) (dec buf-size)))
      nil
      :else
      nil))
  (draw-buffer))

(defn listen []
  (let [keypress (screen/get-key-blocking @scr)
        action (get @keymap keypress)]
    (case action
      :back        ((-> @buf
                        (nth (+ @active-line @offset))
                        (get :back)))
      :scroll-down (move :down)
      :scroll-up   (move :up)
      :select      ((-> @buf
                        (nth (+ @active-line @offset))
                        (get :fn)))
      :quit        (quit)
      :default)
    (listen)))

(defn init-screen []
  ;; TODO: this is buggy - junk data printed at top of terminal sometimes
  (reset! scr (screen/get-screen :unix {:resize-listener (fn [x y]
                                                           (reset! scr-size [x y])
                                                           (when (> @active-line (dec y))
                                                             (reset! active-line (dec y)))
                                                           (draw-buffer))}))
  (screen/start @scr)
  (reset! scr-size (screen/get-size @scr))

  (draw-buffer))
