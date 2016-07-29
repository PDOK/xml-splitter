(ns xml-splitter.zip
  (:require [clojure.java.io :as io])
  (:import (java.util.zip ZipOutputStream ZipEntry)))

(defn zip! [target]
  (with-open [zip (ZipOutputStream. (io/output-stream (str target ".zip")))]
    (let [f (io/file target)]
      (.putNextEntry zip (ZipEntry. (.getName f)))
      (io/copy f zip)
      (.closeEntry zip))))

(defn make-zip-and-delete
  "Generates a zip-file (with the name target) from the sequence of file-names.
   After generating the zip-file the original files will be removed."
  [file]
  (do  (println "Creating zip for" (str file))
       (zip! file)
       (.delete (io/file file))))
