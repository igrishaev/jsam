(ns jsam.core-test
  (:require
   [clojure.test :refer [deftest is]]
   [jsam.core :as jsam]))

(deftest test-it-works!
  (let [data {:foo 1
              :bar 2
              :test [1 {:nested true} 3]
              :hello true
              :lol false
              :missing nil
              :array [[[[1]]]]}

        string
        (jsam/write-string data)

        data2
        (jsam/read-string string)]

    (is (= data data2))))


(deftest test-string-keys
  (let [string (jsam/write-string {:a 1 "b" 2})]
    (is (= "{\"a\":1,\"b\":2}" string))))


(deftest test-keywords-as-values
  (let [string (jsam/write-string {:a/c :b :c [:foo :aaa/bbb :lol]})]
    (is (= "{\"a\\/c\":\"b\",\"c\":[\"foo\",\"aaa\\/bbb\",\"lol\"]}"
           string))
    (is (= {:a/c "b", :c ["foo" "aaa/bbb" "lol"]}
           (jsam/read-string string)))))


(deftest test-number-short
  (let [num (jsam/read-string " -1 ")]
    (is (= -1 num))
    (is (instance? Short num))))


(deftest test-number-integer
  (let [num (jsam/read-string " 11111 ")]
    (is (= 11111 num))
    (is (instance? Integer num))))


(deftest test-number-long
  (let [num (jsam/read-string " 11111111111111111 ")]
    (is (instance? Long num))))


(deftest test-number-bigint
  (let [num (jsam/read-string " 111111111111111111 ")]
    (is (instance? BigInteger num))))


(deftest test-number-float
  (let [num (jsam/read-string " -0.0 ")]
    (is (= -0.0 num))
    (is (instance? Float num))))


;; TODO: always Double
;; parse bigdecimal flag

#_
(deftest test-number-double
  (let [num (jsam/read-string " 1.0+e100 ")]
    (is (= -0.0 num))
    (is (instance? Float num))))


#_
(extend-custom [UUID uuid writer]
  (.writeRaw writer "uuid:")
  (.writeRaw writer (str uuid)))



;; test float
;; test double
;; test bigdecimal

;; test fn-string
;; test fn-number
;; test fn-key

;; test cunstom arr supplier
;; test cunstom obj supplier

;; test read config props
;; test read-write charset?

;; test read file
;; test read reader
;; test read input-stream
;; test read string

;; test write file
;; test write string
;; test write input-stream

;; test write pretty + indent

;; test regex uuid date instant symbol

;; test error cases

;; generative tests (with jsonista)
