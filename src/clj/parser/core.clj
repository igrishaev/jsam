(ns parser.core
  (:import me.ivan.Parser)
  (:use criterium.core)
  (:require
   [jsonista.core :as json]
   [clojure.java.io :as io]))


(set! *warn-on-reflection* true)


(defn parse [src]
  (with-open [in (-> src io/reader)]
    (-> (new Parser in)
        (.readAny))))

(comment

  (quick-bench
      (parse (io/file "data2.json")))

  (quick-bench
      (json/read-value (io/file "data2.json")))

  (json/write-value (io/file "data2.json")
                    (vec
                     (for [x (range 1000)]
                       [true false "hello"])))




  )
