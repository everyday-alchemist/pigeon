(ns control
  (:require [config]
            [db]
            [model]))

(defn listen
  []
  (let [action (model/get-key)]
    (case action
      :back        (model/back)
      :scroll-down (model/move :down)
      :scroll-up   (model/move :up)
      :select      (model/select)
      :quit        (model/quit)
      nil))
  (listen))

(defn -main [& conf-name]
  ;; TODO: find out a way to use optional args w/o using first here
  (model/init (first conf-name))
  (listen))

