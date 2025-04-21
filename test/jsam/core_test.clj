(ns jsam.core-test
  (:import
   (java.io File
            Reader
            StringReader))
  (:require
   [clojure.test :refer [deftest is testing]]
   [jsam.core :as jsam]))

(defn get-temp-file
  "
  Return an temporal file, an instance of java.io.File class.
  "
  (^File []
   (get-temp-file "tmp" ".tmp"))
  (^File [prefix suffix]
   (File/createTempFile prefix suffix)))


;;
;; READ
;;

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
    (is (instance? Double num))))


(deftest test-read-multi
  (testing "numbers"
    (let [reader
          (new StringReader "1 2 3")

          iter
          (jsam/parse-multi reader)]

      (is (= [1 2 3]
             (seq iter)))))

  (testing "trailing item"
    (let [reader
          (new StringReader "{\"foo\": 123}\n42 \n null\ntrue  [1, 2 ,3]")

          iter
          (jsam/parse-multi reader)]

      (is (= [{:foo 123} 42 nil true [1 2 3]]
             (seq iter)))))

  (testing "empty"
    (testing "trailing item"
    (let [reader
          (new StringReader "")

          iter
          (jsam/parse-multi reader)]

      (is (nil? (seq iter)))))))


(deftest test-bigdec-flag
  (let [res (jsam/read-string "42e999")]
    (is (= ##Inf res)))
  (let [res (jsam/read-string "42e999" {:bigdec? true})]
    (is (= 4.2E+1000M res))
    (is (instance? BigDecimal res))))


(deftest test-number-double
  (let [num (jsam/read-string " 1.0+e100 ")]
    (is (= 1.0 num))
    (is (instance? Double num))))


(deftest test-custom-suppliers
  (let [arr-supplier
        (reify java.util.function.Supplier
          (get [this]
            (let [state (atom [])]
              (reify org.jsam.IArrayBuilder
                (conj [this el]
                  (swap! state clojure.core/conj (* el 10)))
                (build [this]
                  @state)))))

        obj-supplier
        (jsam/supplier
          (let [state (atom {})]
            (reify org.jsam.IObjectBuilder
              (assoc [this k v]
                (swap! state clojure.core/assoc k (* v 10)))
              (build [this]
                @state))))

        res1
        (jsam/read-string "[1, 2, 3]"
                          {:arr-supplier arr-supplier})

        res2
        (jsam/read-string "{\"test\": 1}"
                          {:obj-supplier obj-supplier})]

    (is (= [10 20 30] res1))
    (is (= {:test 10} res2))))


;;
;; WRITE
;;

(deftest test-write-multi

  (let [file (get-temp-file)
        coll (for [x (range 0 3)]
               {:x x})

        _
        (jsam/write-multi file coll)

        items
        (jsam/parse-multi file)]

    (is (= [{:x 0} {:x 1} {:x 2}]
           (vec items)))))


(deftest test-write-simple
  (let [data1 {:foo 1
               "bar" 1
               "test" false
               :arr [1 2 nil "hello"]
               'foo 123.456}

        string
        (jsam/write-string data1)]

    (is (= {:foo 123.456,
            :bar 1,
            :test false,
            :arr [1 2 nil "hello"]}
           (jsam/read-string string)))))


(defrecord SomeRecord [a b c])

(deftest test-write-extended-types
  (let [data1 {:foo 1
               "bar" #uuid "39c70c7d-2af8-48da-82b3-8b528d878a9f"
               "test" (java.time.LocalDate/parse "2025-03-03")
               :arr #"kek|lol"
               :rec (new SomeRecord 'A 'B (new SomeRecord 1 2 3))
               'foo (/ 3 4)
               "dunno" ['lol (atom (atom 42)) (ref 100500)]}

        string
        (jsam/write-string data1)]

    (is (= {:foo "3/4",
            :bar "39c70c7d-2af8-48da-82b3-8b528d878a9f",
            :test "2025-03-03",
            :arr "kek|lol",
            :rec {:a "A"
                  :b "B"
                  :c {:a 1 :b 2 :c 3}}
            :dunno ["lol" 42 100500]}
           (jsam/read-string string)))))


(deftype SneakyType [a b c]
  jsam/IJSON
  (-encode [this writer]
    (jsam/-encode ["I used to be a SneakyType" a b c] writer)))


(deftest test-write-extended-types
  (let [data1 {:foo (new SneakyType :a "b" 42)}

        string
        (jsam/write-string data1)]

    (is (= {:foo ["I used to be a SneakyType" "a" "b" 42]}
           (jsam/read-string string)))))


(deftest test-write-pretty
  (let [data1 {:arr [1 [1 {:hello 1} 3] 3]}

        string
        (jsam/write-string data1
                           {:pretty? true
                            :pretty-indent 3})]

    (is (= data1
           (jsam/read-string string)))

    (is (= "{\r
   \"arr\": [\r
      1,\r
      [\r
         1,\r
         {\r
            \"hello\": 1\r
         },\r
         3\r
      ],\r
      3\r
   ]\r
}"
           string))))



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
