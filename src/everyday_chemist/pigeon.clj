(ns everyday-chemist.pigeon
  (:require [remus]
            [everyday-chemist.model :as m]
            [everyday-chemist.config :as config]
            [everyday-chemist.view :as v])
  (:gen-class))

(defn quit [state]
  (v/stop state)
  (System/exit 0))

(defn listen
  [state]
  (let [action (v/get-key-blocking @state)]
    ;; TODO: is there any reason to use swap! here?
    (case action
      \h (reset! state (v/back @state))
      \j (reset! state (v/move @state :down))
      \k (reset! state (v/move @state :up))
      \l (reset! state (v/select @state))
      \q (quit @state)
      nil)
    (listen state)))

(defn -main
  ;; TODO: come up with a more graceful way to handle command line args
  [& conf-name]
  (let [conf-loc (if (System/getenv "XDG_CONFIG_HOME")
                   (System/getenv "XDG_CONFIG_HOME")
                   (System/getenv "HOME"))
        conf (if (first conf-name)
               (config/load-config (first conf-name))
               (config/load-config (str conf-loc "/pigeon/config.edn")))
        urls (get conf :urls)
        state (atom (v/refresh (v/init)))]
    (v/add-resize-listener (:screen @state) 
                           #(v/handle-resize state %1 %2))
    (m/register-listener #(v/refresh (deref state)))
    (doseq [url urls]
      (m/update-feed url (-> url
                             (remus/parse-url)
                             (get :feed))))
    (listen state)))
