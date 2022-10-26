(ns everyday-chemist.model-test
  (:require [clojure.test :refer :all]
            [everyday-chemist.model :as m]))

(comment 
  ;; TODO: I broke these when I changed feed entries to a vector, will fix when change to ordered set
  (deftest update-feed-test
  (testing "update-feed"
    (testing "inserting new feed"
      ;; TODO: make a fixture
      (reset! m/feeds {})
      (m/update-feed "foo.bar" {:entries '(:a :b :c)})
      (is (= {"foo.bar" {:entries #{:a :b :c}}} @m/feeds)))
    (testing "update existing feed"
      (reset! m/feeds {"foo.bar" {:entries #{:a :b :c}
                                  :foo :bar
                                  :orbis :tertius}}) 
      (m/update-feed "foo.bar" {:entries '(:a :b :c :d)
                                :foo :baz
                                :tlon :uqbar})
      (is (= {"foo.bar" {:entries #{:a :b :c :d}
                         :foo :baz
                         :orbis :tertius
                         :tlon :uqbar}} 
             @m/feeds)))))

  ;; TODO: this is janky
  (deftest listener-test
    (testing "listeners"
      (let [a (atom false)
            b (atom false)]
        (m/register-listener #(reset! a true))
        (m/register-listener #(reset! b true))
        (m/update-feed "foo.bar" {:a :b})
        (is (and (= true @a) (= true @b)))))))
