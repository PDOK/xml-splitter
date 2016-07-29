(ns xml-splitter.core
  (:require [clojure.java.io :as io]
            [xml-splitter.zip :as zip]
            [xml-splitter.analyze :as analyze]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [nl.pdok BoyerMoorePatternMatcher]
           (java.io ByteArrayOutputStream))
  (:gen-class))

(def UTF-8-Encoding "UTF-8")

(defn find-next
  "Returns vector with on first positon the result to the next match or the result to end within machter
   and on second position of a match is found"
  [matcher id]
  (let [result (.flushToNextMatch matcher id)]
    [result (.currentPositionIsMatch matcher)]))

(defn- bytes-of [content start end]
  (-> content
      (subs start end)
      clojure.string/trim
      (.getBytes UTF-8-Encoding)))

(defn- last-part [content tag]
  (let [content (String. content)
        end-position-tag (+ (clojure.string/last-index-of content tag)
                            (count tag)
                            1)]
    {:last-element (bytes-of content 0 end-position-tag)
     :footer       (bytes-of content end-position-tag (count content))}))


(defn- make-file [target-dir file-base file-ext nr]
  (clojure.java.io/file target-dir (str file-base "-" nr "." file-ext)))

(defn- append-to-file [file-name content]
  (with-open [o (io/output-stream file-name :append true)]
    (.write o content)))

(defn- append-stream-to-file [file-name stream]
  (when-not (nil? stream)
    (with-open [o (io/output-stream file-name :append true)]
      (.writeTo stream o)
      file-name)))

(defn start-and-end-tag [splitter]
  [(str "<" splitter ">")
   (str "</" splitter ">")])

(defn- nr-and-stream
  "Returns a new/stream starting with the header when needed
   otherwise the orignal stream will be returend."
  [header new-item-needed? orig-nr orig-stream]
  (if new-item-needed?
    (do
      (let [nr (inc orig-nr)
            stream (ByteArrayOutputStream.)]
        (println "File#" nr)
        (.write stream header)
        [nr stream]))
    [orig-nr orig-stream]))

(defn create-files [source target-dir file-base file-ext split-on max-elements]
  (let [[start-tag end-tag] (start-and-end-tag split-on)
        id-start "s"]
    (with-open [input-stream (io/input-stream (io/file source))]
      (let [matcher (BoyerMoorePatternMatcher. input-stream)
            _ (.setPattern matcher id-start (.getBytes start-tag UTF-8-Encoding))
            [header _] (find-next matcher id-start)]
        (loop [header header
               count 0
               orig-file-nr 0
               orig-output-stream nil
               files []]
          (let [[file-nr output-stream] (nr-and-stream header
                                           (= (mod count max-elements) 0)
                                           orig-file-nr
                                           orig-output-stream)
                [result has-match?] (find-next matcher id-start)
                orig-name (when-not (= orig-file-nr file-nr)
                            (append-stream-to-file (make-file target-dir file-base file-ext orig-file-nr) orig-output-stream))
                files (cond-> files orig-name (conj orig-name))]
            (if has-match?
              (do
                (.write output-stream result)
                (recur header (inc count) file-nr output-stream files))
              (do
                (let [{:keys [last-element footer]} (last-part result end-tag)
                      name (make-file target-dir file-base file-ext file-nr)
                      files (cond-> files name (conj name))]
                  (.write output-stream last-element)
                  (append-stream-to-file name output-stream)
                  [files footer])))))))))

(defn- add-footer [names footer]
  (doseq [n names]
    (append-to-file n footer)))

(defn split-xml
  "Split a xml-file (source) in small xml-files, the file will be splitted on the element splitter.
   Eache file will have the header and footer of the original file (source).
   The size of the new xml-files will be based on max-elements in one file.
   The parameter target will be used to name the new xml-files.
   Returns names of the new-files"
  [source target-dir file-base file-ext splitter max-elements]
  (let [ _ (println "Split files")
        [file-names footer] (create-files source target-dir file-base file-ext splitter max-elements)]
    (do (println "Adding footer to files")
        (add-footer file-names footer)
        (println "")
        file-names)))

(def cli-options
  [["-h" "--help"]
   [nil "--auto" "automatic splitting"]
   ["-n" "--split-size MAXELEMENTS" "Defaults to 20000"]
   ["-p" "--split ELEMENT" "Element used for splitting, ie: ns:element"]
   ["-o" "--output DIR" "output directory, defaults to pwd/splitted"]
   ["-z" "--zip" "zip output files"]])

(defn help [summary] (str "\nUsage: prog [OPTIONS] file\n" summary))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn file-name->name+ext [filename]
  (let [idx (clojure.string/last-index-of filename ".")]
    [(subs filename 0 (or idx (count filename)))
     (if idx (subs filename (inc idx)) "")]))

(defn split-file [out-dir options file]
  (let [split-on (or (:split options)
                     (with-open [r (clojure.java.io/reader file)]
                       (analyze/find-recurring-element r)))
        file-name (.getName (clojure.java.io/file file))
        [name ext] (file-name->name+ext file-name)
        split-size (Integer/parseInt (or (:split-size options) "20000"))]
    (println "Splitting on element" split-on)
    (println "Writing to" out-dir (str "[" name "-i." ext "]"))
    (println "Split size" split-size)
    (let [file-names (split-xml file out-dir name ext split-on split-size)]
      (when (:zip options)
        (doseq [n file-names] (zip/make-zip-and-delete n))
        ))))

(defn -main [& args]
  (let [{:keys [options arguments summary]} (parse-opts args cli-options)
        pwd (System/getProperty  "user.dir")]
    (cond
      (:help options) (exit 0 (help summary))
      :else
      (when (and (not (:auto options)) (not (:split options)))
        (exit 1 "--auto OR --split REQUIRED, use --help for more info")))
    (when (> 1 (count arguments)) (exit 0 ""))
    (let [out-dir (or (:output options) (str pwd "/splitted/"))
          _ (.mkdirs (clojure.java.io/file out-dir))]
      (doseq [f (if (:auto options) arguments (take 1 arguments))]
        (println "Transforming" f)
        (split-file out-dir options f)
        (println "\n")))))

