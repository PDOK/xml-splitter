(ns xml-splitter.core
  (:require [clojure.java.io :as io]
            [xml-splitter.zip :as zip]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [nl.pdok BoyerMoorePatternMatcher]
           (java.io ByteArrayOutputStream))
  (:gen-class))

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
      (.getBytes "UTF-8")))

(defn- last-part [content tag]
  (let [content (String. content)
        end-position-tag (+ (clojure.string/last-index-of content tag)
                            (count tag)
                            1)]
    {:last-element (bytes-of content 0 end-position-tag)
     :footer       (bytes-of content end-position-tag (count content))}))


(defn- file-name [target nr]
  (str target "-" nr ".gml"))

(defn- append-to-file [file-name content]
  (with-open [o (io/output-stream file-name :append true)]
    (.write o content)))

(defn- append-stream-to-file [file-name stream]
  (when-not (nil? stream)
    (with-open [o (io/output-stream file-name :append true)]
      (.writeTo stream o)
      file-name)))

(defn- start-and-end-tag [splitter]
  [(.getBytes (str "<" splitter ">") "UTF-8")
   (str "</" splitter ">")])

(defn- nr-and-stream
  "Returns a new/stream starting with the header when needed
   otherwise the orignal stream will be returend."
  [header new-item-needed? orig-nr orig-stream]
  (if new-item-needed?
    (do
      (let [nr (inc orig-nr)
            stream (ByteArrayOutputStream.)]
        (.write stream header)
        [nr stream]))
    [orig-nr orig-stream]))

(defn create-files [source target splitter max-elements]
  (let [[start-tag end-tag] (start-and-end-tag splitter)
        id-start "s"]
    (with-open [input-stream (io/input-stream (io/file source))]
      (let [matcher (BoyerMoorePatternMatcher. input-stream)
            _ (.setPattern matcher id-start start-tag)
            [header _] (find-next matcher id-start)]
        (loop [header header
               count 0
               orig-file-nr 0
               orig-output-stream nil
               file-names #{}]
          (let [[file-nr output-stream] (nr-and-stream header
                                           (= (mod count max-elements) 0)
                                           orig-file-nr
                                           orig-output-stream)
                [result has-match?] (find-next matcher id-start)
                orig-name (when-not (= orig-file-nr file-nr)
                            (append-stream-to-file (file-name target orig-file-nr) orig-output-stream))
                file-names (cond-> file-names orig-name (conj orig-name))]
            (if has-match?
              (do
                (.write output-stream result)
                (recur header (inc count) file-nr output-stream file-names))
              (do
                (let [{:keys [last-element footer]} (last-part result end-tag)
                      name (file-name target file-nr)
                      file-names (cond-> file-names name (conj name))]
                  (.write output-stream last-element)
                  (append-stream-to-file name output-stream)
                  [file-names footer])))))))))

(defn- add-footer [names footer]
  (doseq [n names]
    (append-to-file n footer)))

(defn split-xml
  "Split a xml-file (source) in small xml-files, the file will be splitted on the element splitter.
   Eache file will have the header and footer of the original file (source).
   The size of the new xml-files will be based on max-elements in one file.
   The parameter target will be used to name the new xml-files.
   Returns names of the new-files"
  [source target splitter max-elements]
  (let [ _ (println "Split files")
        [file-names footer] (time (create-files source target splitter max-elements))
        _ (println "Number of files:" (count file-names) "\n")]
    (do (println "Add footer to files")
        (println (String. footer))
        (time (add-footer file-names footer))
        (println "")
        file-names)))

(def cli-options
  [["-h" "--help"]])

(def help "\nUsage: program-name source-file target-name splitter max-element")

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 help)
      (not= (count arguments) 4) (exit 1 (str "Wrong number of (" (count arguments) ") arguments\n" help)))
    (apply split-xml (assoc arguments 3 (Integer/parseInt (get arguments 3))))))

(defn split-and-zip [source target splitter max-elements]
  (let [file-names (split-xml source target splitter max-elements)]
    (zip/make-zip file-names target)))

