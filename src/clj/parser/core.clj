(ns parser.core
  (:import
   [me.ivan Parser JsonWriter]
   [java.io StringWriter]
   [java.util List Map UUID Date]
   [java.util.regex Pattern]
   [java.time.temporal Temporal]
   [clojure.lang Keyword Symbol])
  (:use criterium.core)
  (:require
   [clojure.data.json :as data.json]
   [jsonista.core :as json]
   [clojure.java.io :as io]))


(defprotocol IJSON
  (-encode [this writer]))


(extend-protocol IJSON

  nil
  (-encode [this ^JsonWriter writer]
    (.writeNull writer nil))

  Object
  (-encode [this ^JsonWriter writer]
    (throw (ex-info "cannot json-encode" {:this this})))

  Pattern
  (-encode [this ^JsonWriter writer]
    (.writeString writer (str this)))

  String
  (-encode [this ^JsonWriter writer]
    (.writeString writer this))

  UUID
  (-encode [this ^JsonWriter writer]
    (.writeString writer (str this)))

  Temporal
  (-encode [this ^JsonWriter writer]
    (.writeString writer (str this)))

  Date
  (-encode [this ^JsonWriter writer]
    (.writeString writer (str this)))

  Boolean
  (-encode [this ^JsonWriter writer]
    (.writeBoolean writer this))

  Number
  (-encode [this ^JsonWriter writer]
    (.writeNumber writer this))

  List
  (-encode [this ^JsonWriter writer]
    (.writeArray writer this))

  Map
  (-encode [this ^JsonWriter writer]
    (.writeMap writer this))

  Keyword
  (-encode [this ^JsonWriter writer]
    (.writeString writer (-> this str (subs 1))))

  Symbol
  (-encode [this ^JsonWriter writer]
    (.writeString writer (str this)))



  )


(defn write-to []
  )

(defn write-to-string [value]
  (with-open [out (new StringWriter)
              jwr (JsonWriter/create out -encode)]
    (.write jwr value)
    (.toString out)))



(set! *warn-on-reflection* true)


#_
(defn parse [src]
  (with-open [in (-> src io/reader)]
    (-> (new Parser in)
        (.parse))))

(defn parse2 [^String content]
  (-> (new Parser content)
      (.parse)))

(defn parse3 [^java.io.File file]
  (-> (new Parser file)
      (.parse)))

(comment

  ;; file
  (quick-bench
      (parse3 (io/file "100mb.json")))

  ;; file
  (quick-bench
      (json/read-value (io/file "100mb.json")))

  (quick-bench
      (with-open [r (io/reader (io/file "100mb.json"))]
        (data.json/read r)))

  (def content
    (slurp "data.json"))

  (def content-2
    (slurp "data2.json"))

  (def content-100mb
    (slurp "100mb.json"))

  (def data-100mb
    (json/read-value content))

  (quick-bench
      (json/write-value-as-string data-100mb))

  (quick-bench
      (write-to-string data-100mb))

  ;; string
  (quick-bench
      (parse2 content))

  (quick-bench
      (json/read-value content))

  (quick-bench
      (data.json/read-str content))


  (json/write-value (io/file "data2.json")
                    (vec
                     (for [x (range 1000)]
                       [true false "hello"])))




  )
