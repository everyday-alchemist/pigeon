(ns everyday-chemist.pigeon
  (:require [remus]
            [everyday-chemist.model :as m]
            [everyday-chemist.config :as config]
            [everyday-chemist.view :as v])
  (:gen-class))

(defn quit []
  (v/stop)
  (System/exit 0))

(defn listen
  []
  (let [action (v/get-key-blocking)]
    (case action
      ;:back   (v/back)
      \j       (v/move :down)
      \k       (v/move :up)
      ;:select (v/select)
      \q       (quit)
      nil))
  (listen))


(defn -main
  ;; TODO: come up with a more graceful way to handle command line args
  [& conf-name]
  (v/init)
  (let [conf-loc (if (System/getenv "XDG_CONFIG_HOME")
                   (System/getenv "XDG_CONFIG_HOME")
                   (System/getenv "HOME"))
        conf (if (first conf-name)
               (config/load-config (first conf-name))
               (config/load-config (str conf-loc "/pigeon/config.edn")))
        urls (get conf :urls)]
    (doseq [url urls]
      (m/update-feed url (-> url
                             (remus/parse-url)
                             (get :feed)))))
  ;; TODO: why does it hang if I move the listen call inside the let
  (listen))
