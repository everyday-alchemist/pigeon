(ns config
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [utils]))

(def default-config {:urls   ["http://planet.clojure.in/atom.xml"]

                     :keymap {\h :back
                              \j :scroll-down
                              \k :scroll-up
                              \l :select
                              \q :quit}

                     :ui     {:fg          :white
                              :bg          :black
                              :fg-selected :black
                              :bg-selected :yellow}})

(defn load-config
  "read .edn from config file"
  [source]
  (try
    (let [user-conf (with-open [r (io/reader source)]
                      (edn/read (java.io.PushbackReader. r)))]
      (utils/superimpose user-conf default-config))

    (catch java.io.IOException e
      (printf "Couldn't open pigeon config '%s': %s\n" source (.getMessage e))
      default-config)
    (catch RuntimeException e
      (printf "Error parsing pigeon config '%s': %s\n" source (.getMessage e))
      default-config)))
