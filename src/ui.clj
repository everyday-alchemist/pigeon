(ns ui
  (:require [clojure.java.browse :refer [browse-url]]
            [lanterna.screen :as screen]
            [rss]))

(def scr (atom nil)) ;TODO: refactor to local?
(def scr-size (atom []))
(def feeds (atom {}))
(def offset (atom 0))
(def active-line (atom 0))
(def fg-color :black)
(def bg-color :yellow)

;; buf is a list of strings representing the text on the screen, indexed by line number
(def buf (atom []))

(defn fill-buff
  "Helper function to put some junk data in the buffer"
  []
  (loop [i 0
         n (rand-int 10000000)]
    (when (< i 50)
      (swap! buf conj {:text (str n) :fn #(println i)})
      (recur (inc i) (rand-int 10000000)))))

(defn quit []
  (.setCursorVisible (.getTerminal @scr) false)
  (screen/stop @scr)
  (java.lang.System/exit 0))

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
  [scr]
  (screen/redraw scr)
  (.setCursorVisible (.getTerminal scr) false))

(defn draw-buffer
  [scr b]
  (screen/clear scr)

  ;; TODO: This can probably be done without loop
  (loop [b (drop @offset b)
         i 0]
    (when-not (empty? b)
      (let [s (if (= i @active-line)
                (pad-str (:text (first b)) (first @scr-size))
                (:text (first b)))]
        ;(println s)
        (screen/put-string scr 0 i s
                           {:fg (if (= i @active-line) fg-color :default)
                            :bg (if (= i @active-line) bg-color :default)}))
      (recur (rest b) (inc i))))

  (refresh scr))

(defn move [dir]
  ;; TODO: this is disgusting, write predicates to clean this up
  (cond
    (and (= :up dir) (not= 0 @active-line))
    (swap! active-line dec)
    (and (= :up dir) (= 0 @active-line) (not= 0 @offset))
    (swap! offset dec)
    (and (= :up dir) (= 0 @active-line) (= 0 @offset))
    nil
    (and (= :down dir) (= (dec (second @scr-size)) (count @buf)) (= @active-line (dec (count @buf))))
    nil
    (and (= :down dir) (not= (dec (second @scr-size)) @active-line))
    (swap! active-line inc)
    (and (= :down dir) (= (dec (second @scr-size)) @active-line) (not= (+ @active-line @offset) (dec (count @buf))))
    (swap! offset inc)
    (and (= :down dir) (= (dec (second @scr-size)) @active-line) (= (+ @active-line @offset) (dec (count @buf))))
    nil
    :else
    nil)
  (draw-buffer @scr @buf))

(defn listen []
  (let [keypress (screen/get-key-blocking @scr)]
    (case keypress
      \j (move :down)
      \k (move :up)
      \l ((-> @buf
              (nth (+ @active-line @offset))
              (get :fn)))
      \q (quit)
      :default)
    (listen)))

(defn add-feed [url]
  (swap! feeds assoc url (rss/get-feed url)))

(defn add-sample-feeds []
  (add-feed "http://planet.clojure.in/atom.xml")
  (add-feed "http://original.antiwar.com/feed")
  (add-feed "https://greenwald.substack.com/feed"))

(defn feed->buff [name]
  (reset! buf [])
  (let [entries (get-in @feeds [name :entries])]
    (doseq [entry entries]
      (swap! buf conj {:text (:title entry) :fn #(browse-url (:uri entry))})))
  (draw-buffer @scr @buf))

(defn -main [& _]
  (add-sample-feeds)
  (doseq [name (keys @feeds)]
    (swap! buf conj {:text name :fn #(feed->buff name)}))

  ;; TODO: this is buggy - junk data printed at top of terminal sometimes
  (reset! scr (screen/get-screen :unix {:resize-listener (fn [x y]
                                                           (reset! scr-size [x y])
                                                           (when (> @active-line (dec y))
                                                             (reset! active-line (dec y)))
                                                           (draw-buffer @scr @buf))}))
  (screen/start @scr)
  (reset! scr-size (screen/get-size @scr))

  (draw-buffer @scr @buf)
  (listen))
