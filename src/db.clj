(ns db
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]))

(def db "jdbc:h2:./resources/db/pigeon")
(def ds (jdbc/get-datasource db))

(def init [(sql/format {:create-table [:feed :if-not-exists]
                        :with-columns
                        [[:feed_url [:varchar 512] :primary :key]]})
           (sql/format  {:create-table [:entry :if-not-exists]
                         :with-columns
                         [[:id :int :auto-increment :primary :key]
                          [:title [:varchar 256] [:not nil]]
                          [:uri [:varchar 512]]
                          [:feed_url [:varchar 512] [:not nil]]
                          [[:foreign-key :feed_url] :references [:feed :feed_url]]]})])

(defn create-tables []
  (doseq [s init]
    (jdbc/execute! ds s)))

(defn insert-feed
  [url]
  (jdbc/execute! ds (sql/format {:insert-into :feed
                                 :columns [:feed_url]
                                 :values [[url]]})))

(defn insert-entry
  [{:keys [title uri feed_url]}]
  (jdbc/execute! ds (sql/format {:insert-into :entry
                                 :columns [:title :uri :feed_url]
                                 :values [[title uri feed_url]]})))
(defn get-feed
  [& url]
  (jdbc/execute! ds (if url
                      (sql/format {:select [:*] :from [:feed] :where [:= :feed_url url]})
                      (sql/format {:select [:*] :from [:feed]}))))

(defn get-entries
  ([]
   (jdbc/execute! ds (sql/format {:select [:*] :from [:entry]})))
  ([{:keys [title uri feed_url]}]
   (jdbc/execute! ds (sql/format {:select [:*] :from [:entry] :where [:and [:= :title title]
                                                                      [:= :uri uri]
                                                                      [:= :feed_url feed_url]]}))))

(defn get-entries-for-url
  [url]
  (jdbc/execute! ds (sql/format {:select [:*] :from [:entry] :where [:= :feed_url url]})))

(defn insert-feed-if-unique
  [url]
  (when (empty? (get-feed url))
    (insert-feed url)))

(defn insert-entry-if-unique
  [attrs]
  (when (empty? (get-entries attrs))
    (insert-entry attrs)))
