(ns filesplitter.pattern
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import [nl.pdok BoyerMoorePatternMatcher]
           (java.io ByteArrayOutputStream)
           (java.util.zip ZipOutputStream ZipEntry)))

(defn find-next
  "Returns vector with on first positon the result to next match or result to end
    and on second position of a match is found"
  [matcher id]
  (let [result (.flushToNextMatch matcher id)]
    [result (.currentPositionIsMatch matcher)]))

(defn last-part [content tag]
  (let [content (String. content)
        end-position-tag (+ (clojure.string/last-index-of content tag)
                            (count tag)
                            1)]
    {:last-element (.getBytes (clojure.string/trim (subs content 0 end-position-tag)) "UTF-8")
     :footer       (.getBytes (clojure.string/trim (subs content end-position-tag (count content))) "UTF-8")}))


(defn remove-files [names]
  (doseq [n names]
    (.delete (io/file n))))

(defn file-name [target nr]
  (str target "-" nr ".gml"))

(defn append-to-file [file-name content]
  (with-open [o (io/output-stream file-name :append true)]
    (.write o content)))

(defn append-stream-to-file [file-name stream]
  (when-not (nil? stream)
    (with-open [o (io/output-stream file-name :append true)]
      (.writeTo stream o)
      file-name)))

(defn nr-and-stream [header new-item-needed? orig-nr orig-stream]
  (if new-item-needed?
    (do
      (let [nr (inc orig-nr)
            stream (ByteArrayOutputStream.)]
        (.write stream header)
        [nr stream]))
    [orig-nr orig-stream]))

(defn add-footer [names footer]
  (doseq [n names]
    (append-to-file n footer)))

(defn create-zip [names target]
  (with-open [zip (ZipOutputStream. (io/output-stream (str target ".zip")))]
    (doseq [n names]
      (let [f (io/file n)]
        (.putNextEntry zip (ZipEntry. (.getPath f)))
        (io/copy f zip)
        (.closeEntry zip)))))

(defn create-files [source target splitter max-elements]
  (let [start-tag (.getBytes (str "<" splitter ">") "UTF-8")
        end-tag (str "</" splitter ">")
        id-start "s"]
    (with-open [stream (io/input-stream (io/file source))]
      (let [matcher (BoyerMoorePatternMatcher. stream)
            _ (.setPattern matcher id-start start-tag)
            [header _] (find-next matcher id-start)]
        (loop [header header count 0 orig-nr 0 orig-stream nil file-names #{}]
          (let [[nr stream] (nr-and-stream header
                                           (= (mod count max-elements) 0)
                                           orig-nr
                                           orig-stream)
                [result has-match?] (find-next matcher id-start)
                orig-name (when-not (= orig-nr nr)          ;persist orig-stream in a file
                            (append-stream-to-file (file-name target orig-nr) orig-stream))
                file-names (if (nil? orig-name) file-names (conj file-names orig-name))]
            (if has-match?
              (do
                (.write stream result)
                (recur header (inc count) nr stream file-names))
              (do
                (let [{:keys [last-element footer]} (last-part result end-tag)
                      name (file-name target nr)
                      file-names (if (nil? name) file-names (conj file-names name))]
                  (.write stream last-element)
                  (append-stream-to-file name stream)
                  [file-names footer])))))))))

(defn splitted-zip [source target splitter max-elements]
  (let [_ (println "create-files")
        [names footer] (time (create-files source target splitter max-elements))]
    (do
      (println "create footer")
      (time (add-footer names footer))
      (println "create zip")
      (time (create-zip names target))
      (println "remove files")
      (remove-files names))))



