(ns parser.core
  (:import me.ivan.Parser)
  (:use criterium.core)
  (:require
   [clojure.data.json :as data.json]
   [jsonista.core :as json]
   [clojure.java.io :as io]))


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
