(ns xml-splitter.analyze
  (:require [clojure.data.xml :as xml]))

(defn event->element [event]
  (if (= clojure.data.xml.event.StartElementEvent (type event))
    (let [qname (:tag event)]
      {:tag (.getLocalPart qname)
       :prefix (.getPrefix qname)
       :type :start})
    {:tag (name (:tag event))
     :type :end}))

(defn rolling-average [old-average new-value n]
  (-> old-average (* (dec n)) (+ new-value) (/ n)))

(defn update-statistics [stats [i event]]
  (if (= clojure.data.xml.event.StartElementEvent (type event))
    (let [key (.getLocalPart (:tag event))
          {:keys [freq avg-length prefix]}
          (get stats key {:last-start i :freq 0 :avg-length 0 :prefix (.getPrefix (:tag event))})]
      (assoc! stats key
              {:freq (inc freq) :last-start i :avg-length avg-length :prefix prefix}))
    (let [key (name (:tag event))]
      (if-let [{:keys [last-start freq avg-length prefix]} (get stats key)]
        (assoc! stats key
                {:freq       freq
                 :last-start last-start
                 :avg-length (rolling-average avg-length (- i last-start) freq)
                 :prefix     prefix})
        stats))))

(defn calculate-frequencies [events]
  (persistent!
    (reduce update-statistics
            (transient {}) events)))

(defn find-recurring-element [input-stream]
  (let [events (xml/event-seq input-stream {})
        start+end-events
        (->> events
             (filter #(or (= clojure.data.xml.event.StartElementEvent (type %))
                          (= clojure.data.xml.event.EndElementEvent (type %))))
             (map (fn [i e] [i e]) (range))
             (take 10000))
        freq (calculate-frequencies start+end-events)
        recurring-only (filter (fn [[k v]] (< 1 (:freq v))) freq)
        weighted-elements (map (fn [[k v]] {:element (str (:prefix v) ":" k) :weight (* (:freq v) (:avg-length v))}) recurring-only)]
    (:element (first (reverse (sort-by :weight weighted-elements))))))