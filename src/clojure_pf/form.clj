(ns clojure-pf.form
  "Module to retrieve useful information from user-defined packet form")

; Utilities

(def ^:private ^:const entry->size 3)

(defn- entry->valid? [entry]
  "Tests whether a form entry is valid according to the following rules:
   * A form field must be a keyword.
   * The kind must be a keyword denoting a number or a buffer.
    - If it's a number, check if the size is either 1, 2, 4 or 8.
    - If it's a buffer, check if the size is above zero."
  (let [size (:size entry)]
    (and (keyword? (:field entry))
         (case (:kind entry)
           :int (contains? #{1 2 4 8} size)
           :buf (pos? size)))))

(defn- entry->parse [form]
  "Parses an entry from a form."
  (let [[field kind size] (take entry->size form)
        entry {:field field
               :kind kind
               :size size}]
    (if (entry->valid? entry)
      entry)))

; Exports

(defn to-entries [form]
  "Returns a list of form entries, each containing the following information:
   * field name
   * value kind (type)
   * value size
   * value offset"
  (loop [entries  []
         form     form
         offset   0]
    (let [entry (entry->parse form)]
      (if-not entry
        entries
        (let [entry   (assoc entry :offset offset)
              entries (conj entries entry)
              form    (drop entry->size form)
              offset  (+ offset (:size entry))]
          (recur entries form offset))))))

(defn size [form]
  "Returns the total packet size defined by a form."
  (->> (to-entries form)
       (map :size)
       (reduce +)))
