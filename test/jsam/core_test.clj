(ns jsam.core-test
  (:require
   [clojure.test :refer [deftest is]]
   [jsam.core :as jsam]))


(deftest test-simple-ok
  (let [data {:foo 1
              "bar" 2.002
              :test [1 {:nested true} 3]
              :hello true
              :lol false
              :missing nil
              :array [[[[1]]]]}

        string
        (jsam/write-string data)

        data2
        (jsam/read-string string)]

    (is (= data data2))





    )
  )
