(ns clojure-pf.core.form
  "Module to retrieve useful information from user-defined packet forms")

; Validation utilities

(defn- field->valid? [field]
  "Tests whether a field is valid."
  (keyword? field))

(defn- kind->valid? [kind]
  "Tests whether a field kind is valid."
  (contains? #{:byte :short :int :long :float :double} kind))

(defn- size->valid? [size]
  "Tests whether a field size is valid."
  (and (number? size)
       (pos? size)))

; Entry type definitions

(defprotocol Entry
  (entry->valid? [this])
  (entry->type   [this]))

(defrecord ScalarEntry [field kind]
  Entry
  (entry->valid? [this]
    (and (field->valid? (:field this))
         (kind->valid?  (:kind this))))
  (entry->type [_] :scalar))

(defrecord ArrayEntry [field kind size]
  Entry
  (entry->valid? [this]
    (and (field->valid? (:field this))
         (kind->valid?  (:kind this))
         (size->valid?  (:size this))))
  (entry->type [_] :array))

; Form token utilities

(defn- pull-token [form pred]
  "Pulls the next token from a form.
  If the predicate returns true for the token, it is returned along
  with the rest of the form. Otherwise, nil is returned along
  with the original form."
  (let [token (first form)
        tail  (next form)]
    (if (pred token)
      [token tail]
      [nil form])))

(defn- pull-entry [entries form]
  "Parses an entry to a list of entries from a form.
  If successful, returns an entry appended to the given entries
  and the rest of the form in a list."
  (let [[field tail]  (pull-token form keyword?)
        [kind tail]   (pull-token tail keyword?)
        [size tail]   (pull-token tail number?)
        entry         (if-not size
                        (->ScalarEntry field kind)
                        (->ArrayEntry field kind size))]
    (if (entry->valid? entry)
      [(conj entries entry) tail])))

; Exports

(defn to-entries [form]
  "Parses a list of packet field entries from a form."
  (let [[entries _] (->> [[] form]
                         (iterate (partial apply pull-entry))
                         (take-while some?)
                         last)]
    entries))
