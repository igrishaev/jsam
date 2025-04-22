(def MIN_JAVA_VERSION "17")

(defproject com.github.igrishaev/jsam "0.1.1-SNAPSHOT"

  :license
  {:name "The Unlicense"
   :url "https://unlicense.org/"}

  :description
  "A lightweight, zero-deps JSON parser and writer"

  :deploy-repositories
  {"releases"
   {:url "https://repo.clojars.org"
    :creds :gpg}
   "snapshots"
   {:url "https://repo.clojars.org"
    :creds :gpg}}

  :release-tasks
  [["vcs" "assert-committed"]
   ["test"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["deploy"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :managed-dependencies
  [[org.clojure/clojure "1.11.1"]
   [metosin/jsonista "0.3.8"]
   [cheshire "6.0.0"]
   [org.clojure/data.json "2.5.1"]
   [criterium "0.4.6"]]

  :dependencies
  [[org.clojure/clojure :scope "provided"]]

  :profiles
  {:dev
   {:dependencies
    [[metosin/jsonista]
     [cheshire]
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
