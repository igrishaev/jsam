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
        (.parse))))

(comment

  (quick-bench
      (parse (io/file "data.json")))

  (quick-bench
      (json/read-value (io/file "data.json")))


  )
