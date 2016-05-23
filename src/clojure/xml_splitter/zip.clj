(ns xml-splitter.zip
  (:require [clojure.java.io :as io])
  (:import (java.util.zip ZipOutputStream ZipEntry)))

(defn add-to-zip [names target]
  (with-open [zip (ZipOutputStream. (io/output-stream (str target ".zip")))]
    (doseq [n names]
      (let [f (io/file n)]
        (.putNextEntry zip (ZipEntry. (.getPath f)))
        (io/copy f zip)
        (.closeEntry zip)))))

(defn remove-files [names]
  (doseq [n names]
    (.delete (io/file n))))

(defn make-zip
  "Generates a zip-file (with the name target) from the sequence of file-names.
   After generating the zip-file the original files will be removed."
  [file-names target]
  (do  (println "Create zip")
       (time (add-to-zip file-names target))
       (println "\nRemove files")
       (time (remove-files file-names))))
