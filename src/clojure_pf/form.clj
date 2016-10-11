(ns clojure-pf.form
  "Module to retrieve useful information from user-define packet form")

; Utilities

(def ^:private ^:const entry->width 3)

(defn- entry->valid? [entry]
  "Tests whether a form entry is valid according to the following rules:
   * A form field must be a keyword.
   * The type must be a keyword denoting a number or a buffer.
    - If it's a number, check if the size is either 1, 2, 4 or 8.
    - If it's a buffer, check if the size is above zero."
  (and (keyword? (:field entry))
       (case (:type entry)
         :int (contains? #{1 2 4 8} (:size entry))
         :buf (pos? (:size entry)))))

(defn- form->next-entry [form]
  "Returns the next entry of a packet form along
  with the rest of it in a pair."
  (let [[field _type size] (take entry->width form)
        entry {:field field :type _type :size size}]
    (when (entry->valid? entry)
      [entry (drop entry->width form)])))

; Exports

(defn entry-list [form]
  "Returns a list containing the field, type, size and offset
  of each entry in a packet form."
  (loop [[entry form] (form->next-entry form)
         entries      []
         offset       0]
    (if-not entry
      entries
      (let [next-entry  (form->next-entry form)
            entry       (assoc entry :offset offset)
            entries     (conj entries entry)
            offset      (+ offset (:size entry))]
        (recur next-entry entries offset)))))

(defn size [form]
  "Returns the sum of all the sizes defined in the packet form."
  (->> (entry-list form)
       (map :size)
       (reduce +)))
