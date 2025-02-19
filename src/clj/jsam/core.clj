(ns jsam.core
  (:refer-clojure :exclude [read read-string])
  (:import
   [org.jsam JsonParser JsonWriter Config]
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

(set! *warn-on-reflection* true)

;;
;; Config
;;


(defn ->config ^Config [opt]
  (if (empty? opt)
    Config/DEFAULTS

    (let [{:keys [read-buf-size
                  temp-buf-scale-factor
                  temp-buf-size
                  parser-charset
                  writer-charset
                  array-builder-factory
                  object-builder-factory
                  pretty?
                  pretty-indent]}
          opt
          ]

      (cond-> (Config/builder)

        read-buf-size
        (.readBufSize read-buf-size)

        temp-buf-scale-factor
        (.tempBufScaleFactor temp-buf-scale-factor)

        temp-buf-size
        (.tempBufSize temp-buf-size)

        ;; parser-charset
        ;; writer-charset
        ;; array-builder-factory
        ;; object-builder-factory
        ;; pretty?
        ;; pretty-indent

        ;; parserCharset
        ;; writerCharset
        ;; arrayBuilderFactory
        ;; objectBuilderFactory
        ;; isPretty
        ;; prettyIndent

        :finally
        (.build))

      ))

  )


;;
;; Reader
;;

(defn read
  "
  Read data from a source that can be a file, a file path,
  an input stream, a writer, etc. The source gets transformed
  to the `Reader` instance. The reader gets closed afterwards.
  Accepts an optional map of settings.
  "
  ([src]
   (read src nil))

  ([src opt]
   (with-open [r (io/reader src)
               p (JsonParser/fromReader r (->config opt))]
     (.parse p))))


(defn read-string
  "
  Read data from a string. Works a bit faster than `read` as
  the entire data sits in memory and no IO is performed.
  "
  ([^String string]
   (read-string string nil))

  ([^String string opt]
   (with-open [p (JsonParser/fromString string (->config opt))]
     (.read p))))


;;
;; Writer
;;


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

(def CFG
  (-> (Config/builder)
      (.isPretty true)
      (.prettyIndent 4)
      (.build)))

(defn write-to-string [value]
  (with-open [out (new StringWriter)
              jwr (JsonWriter/create out -encode CFG)]
    (.write jwr value)
    (.toString out)))






#_
(defn parse [src]
  (with-open [in (-> src io/reader)]
    (-> (new Parser in)
        (.parse))))

(defn parse2 [^String content]
  (-> content
      (JsonParser/fromString)
      (.parse)))

(defn parse3 [^java.io.File file]
  (-> file
      (JsonParser/fromFile)
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
