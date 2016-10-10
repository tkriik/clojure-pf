(ns clojure-pf.structure
  "Module to retrieve useful information from user-defined 
  packet structures.")

(def ^:const entry->width 3)

(defn- field->valid? [field]
  "Tests whether a structure entry field is valid."
  (keyword? field))

(defn- type->valid? [_type]
  "Tests whether a structure entry type is valid."
  (and (keyword? _type)
       (contains? #{:int :buf} _type)))

(defn- size->valid? [size]
  "Tests whether a structure entry size is valid."
  (number? size))

(defn- entry->valid? [entry]
  "Tests whether a structure entry is valid."
  (and (field->valid? (:field entry))
       (type->valid?  (:type entry))
       (size->valid?  (:size entry))))

(defn- structure->next-entry [structure]
  "Returns the next entry of a packet structure along
  with the rest of it in a pair."
  (let [[field _type size] (take entry->width structure)
        entry {:field field :type _type :size  size}]
    (when (entry->valid? entry)
      [entry (drop entry->width structure)])))

(defn- structure->entry-list [structure]
  "Returns a list containing the field, type, size and offset
  of each entry in a packet structure."
  (loop [[entry entries]  (structure->next-entry structure)
         entry-list       []
         offset           0]
    (if-not entry
      entry-list
      (let [next-entry  (structure->next-entry entries)
            entry       (assoc entry :offset offset)
            entry-list  (conj entry-list entry)
            offset      (+ offset (:size entry))]
        (recur next-entry entry-list offset)))))

(defn- structure->index-by [structure field]
  "Transforms a packet structure into a entry map keyed by
  the given field."
  (let [entry-list  (structure->entry-list structure)
        fields      (map field entry-list)
        values      (map #(dissoc % field) entry-list)]
    (zipmap fields values)))

(defn field-info [structure]
  "Generates a field-indexed map from a packet structure
  where each field points to a type, size and offset."
  (structure->index-by structure :field))

(defn offset-info [structure]
  "Generates an offset-indexed map from a packet structure
  where each offset points to a field, type and size."
  (structure->index-by structure :offset))
