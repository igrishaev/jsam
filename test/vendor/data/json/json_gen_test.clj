(ns vendor.data.json.json-gen-test
  "
  An adapted clojure.data.json test suite:
  https://github.com/clojure/data.json/blob/master/src/test/clojure/clojure/data/json_gen_test.clj
  "
  (:require [jsam.core :as jsam]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]))

(s/def ::json-number
  (s/with-gen
    number?
    #(sgen/one-of [(sgen/large-integer) (sgen/double* {:infinite? false :NaN? false})])))

(s/def ::json-scalar (s/or :boolean boolean?
                           :number ::json-number
                           :string string?
                           :nil nil?))

(s/def ::json-value (s/or :object ::json-object
                          :array ::json-array
                          :scalar ::json-scalar))

(s/def ::json-array (s/coll-of ::json-value :gen-max 12))
(s/def ::json-object (s/map-of string? ::json-value
                               :gen-max 10))

(s/fdef jsam/write-string
  :args (s/cat :json ::json-value)
  :ret string?
  :fn #(= (->> % :args :json (s/unform ::json-value))
          (jsam/read-string (-> % :ret))))

(deftest roundtrip
  (let [results (stest/check `json/write-string)]
    (is (every? nil? (mapv :failure results)))))
