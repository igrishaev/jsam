(def MIN_JAVA_VERSION "17")

(defproject com.github.igrishaev/jsam "0.1.0-SNAPSHOT"

  :managed-dependencies
  [[org.clojure/clojure "1.11.1"]
   [metosin/jsonista "0.3.8"]
   [org.clojure/data.json "2.5.1"]
   [criterium "0.4.6"]]

  :dependencies
  [[org.clojure/clojure]]

  :profiles
  {:dev
   {:dependencies
    [[metosin/jsonista]
     [org.clojure/data.json]
     [criterium]]

    :global-vars
    {*warn-on-reflection* true
     *assert* true}}}

  :pom-addition
  [:properties
   ["maven.compiler.source" ~MIN_JAVA_VERSION]
   ["maven.compiler.target" ~MIN_JAVA_VERSION]]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"
                  "-Xlint:preview"
                  "--release" ~MIN_JAVA_VERSION])
