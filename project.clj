(defproject xml-splitter "0.1.0-SNAPSHOT"
  :description "XML-Splitter"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]]
  :source-paths ["src/clojure"]
  :resource-paths ["src/resources"]
  :test-paths ["src/test/clojure"]
  :java-source-paths ["src/java"]
  :target-path "target/%s"
  :main ^:skip-aot xml-splitter.core
  :profiles {:uberjar {:aot :all}})
