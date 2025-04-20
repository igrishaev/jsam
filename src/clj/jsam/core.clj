(ns jsam.core
  (:refer-clojure :exclude [read read-string])
  (:import
   [clojure.lang Keyword Symbol]
   [java.io StringWriter]
   [java.time.temporal Temporal]
   [java.util List Map UUID Date]
   [java.util.function Supplier]
   [java.util.regex Pattern]
   [org.jsam JsonParser JsonWriter Config])
  (:use criterium.core)
  (:require
   [clojure.java.io :as io]))


(set! *warn-on-reflection* true)


(defmacro error!
  ([message]
   `(throw (org.jsam.Error/error ~message)))

  ([template & args]
   `(throw (org.jsam.Error/error (format ~template ~@args)))))


(defmacro supplier [& body]
  `(reify Supplier
     (get [this#]
       ~@body)))


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
                  array-builder-supplier
                  object-builder-supplier
                  pretty?
                  pretty-indent]}
          opt]

      (cond-> (Config/builder)

        read-buf-size
        (.readBufSize read-buf-size)

        temp-buf-scale-factor
        (.tempBufScaleFactor temp-buf-scale-factor)

        temp-buf-size
        (.tempBufSize temp-buf-size)

        parser-charset
        (.parserCharset parser-charset)

        writer-charset
        (.writerCharset writer-charset)

        array-builder-supplier
        (.arrayBuilderSupplier array-builder-supplier)

        object-builder-supplier
        (.objectBuilderSupplier object-builder-supplier)

        (some? pretty?)
        (.isPretty pretty?)

        pretty-indent
        (.prettyIndent pretty-indent)

        :finally
        (.build)))))


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


;; TODO:
(defn read-file
  ([file]
   (read-file file nil))

  ([file opt]
   (with-open [p (JsonParser/fromFile file (->config opt))]
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
     (.parse p))))


;;
;; Writer
;;

(defprotocol IJSON
  (-encode [this writer]))


(defn write
  "
  Write data into a destination that can be a file path, a file,
  an output stream, a writer, etc. The data is arbitrary Clojure
  or Java value. Accepts an optional map of preferences.
  "
  ([dest data]
   (write dest data nil))

  ([dest data opt]
   (with-open [out (io/writer dest)
               jwr (JsonWriter/create out -encode (->config opt))]
     (.write jwr data))))


(defn write-string
  "
  Like `write` but the output is a `StringWriter` which gets
  turned into a string afterwards.
  "
  (^String [data]
   (write-string data nil))

  (^String [data opt]
   (with-open [out (new StringWriter)
               jwr (JsonWriter/create out -encode (->config opt))]
     (.write jwr data)
     (.toString out))))


;;
;; Writer extensions
;;

(defmacro extend-as-string [Type]
  (let [writer (with-meta (gensym "writer") {:tag `JsonWriter})]
    `(extend-protocol IJSON
       ~Type
       (-encode [this# ~writer]
         (.writeString ~writer (str this#))))))


(defmacro extend-custom [[Type value ^JsonWriter writer] & body]
  `(extend-protocol IJSON
     ~Type
     (-encode [~value ~writer]
       ~@body)))


(extend-as-string Pattern)
(extend-as-string UUID)
(extend-as-string Temporal)
(extend-as-string Date)
(extend-as-string Symbol)


(extend-protocol IJSON

  nil
  (-encode [this ^JsonWriter writer]
    (.writeNull writer nil))

  Object
  (-encode [this ^JsonWriter writer]
    (error! "don't know how to JSON-encode an object, type: %s, value: %s"
            (type this) this))

  String
  (-encode [this ^JsonWriter writer]
    (.writeString writer this))

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
    (.writeString writer (-> this str (subs 1)))))


(comment

  (require '[clojure.data.json :as data.json])
  (require '[jsonista.core :as json])
  (require '[cheshire.core :as cheshire])

  ;; jsam
  (quick-bench
      (read-file (io/file "100mb.json")))

  ;; jsonista
  (quick-bench
      (json/read-value (io/file "100mb.json")))

  ;; data.json
  (quick-bench
      (with-open [r (io/reader (io/file "100mb.json"))]
        (data.json/read r)))

  ;; cheshire
  (quick-bench
      (with-open [r (io/reader (io/file "100mb.json"))]
        (cheshire/parse-stream r)))

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
