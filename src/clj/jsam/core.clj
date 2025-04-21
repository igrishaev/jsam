(ns jsam.core
  (:refer-clojure :exclude
                  [read read-string])
  (:import
   (clojure.lang Keyword
                 Ratio
                 Atom
                 Ref)
   (java.io StringWriter)
   (java.time.temporal Temporal)
   (java.util List
              Map
              UUID
              Date)
   (java.util.function Supplier)
   (java.util.regex Pattern)
   (org.jsam JsonParser
             JsonWriter
             Config
             Suppliers))
  (:require
   [clojure.java.io :as io]))


(set! *warn-on-reflection* true)


(defmacro error!
  ([message]
   `(throw (org.jsam.Error/error ~message)))

  ([template & args]
   `(throw (org.jsam.Error/error (format ~template ~@args)))))


;;
;; Suppliers
;;

(defmacro supplier [& body]
  `(reify Supplier
     (get [this#]
       ~@body)))


(def ^Supplier sup-arr-java
  "
  A supplier that reads a JSON array into
  a mutable Java ArrayList instance.
  "
  Suppliers/ARR_JAVA_LIST)


(def ^Supplier sup-obj-java
  "
  A supplier that reads a JSON object into
  a mutable Java HashMap instance.
  "
  Suppliers/OBJ_JAVA_MAP)


(def ^Supplier sup-arr-clj
  "
  A supplier that reads a JSON array into
  a persistent Clojure vector
  "
  Suppliers/ARR_CLJ_VEC)


(def ^Supplier sup-obj-clj
  "
  A supplier that reads a JSON object into
  a persistent Clojure map.
  "
  Suppliers/OBJ_CLJ_MAP)



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
                  arr-supplier
                  obj-supplier
                  pretty?
                  pretty-indent
                  bigdec?
                  fn-key
                  multi-separator]}
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

        arr-supplier
        (.arrayBuilderSupplier arr-supplier)

        obj-supplier
        (.objectBuilderSupplier obj-supplier)

        (some? pretty?)
        (.isPretty pretty?)

        pretty-indent
        (.prettyIndent pretty-indent)

        (some? bigdec?)
        (.useBigDecimal bigdec?)

        fn-key
        (.fnKey fn-key)

        multi-separator
        (.multiSeparator multi-separator)

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


(defn read-multi
  "
  Get a lazy sequence of JSON parsed items written one by one.
  Should be used under the `with-open` macro.
  "
  ([src]
   (read-multi src nil))

  ([src opt]
   (-> (JsonParser/fromReader (io/reader src) (->config opt))
       (.parseMulti))))


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


(defn write-multi
  ([dest coll]
   (write-multi dest coll nil))

  ([dest coll opt]
   (with-open [out (io/writer dest)
               jwr (JsonWriter/create out -encode (->config opt))]
     (.writeMulti jwr coll))))


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


(extend-protocol IJSON

  nil
  (-encode [this ^JsonWriter writer]
    (.writeNull writer nil))

  Object
  (-encode [this ^JsonWriter writer]
    (.writeString writer (str this)))

  String
  (-encode [this ^JsonWriter writer]
    (.writeString writer this))

  Boolean
  (-encode [this ^JsonWriter writer]
    (.writeBoolean writer this))

  Ratio
  (-encode [this ^JsonWriter writer]
    (.writeString writer (str this)))

  Number
  (-encode [this ^JsonWriter writer]
    (.writeNumber writer this))

  Atom
  (-encode [this ^JsonWriter writer]
    (-encode @this writer))

  Ref
  (-encode [this ^JsonWriter writer]
    (-encode @this writer))

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

  (:use criterium.core)
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
