(ns clojure-pf.form
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

(defn- pull-token [form]
  "Pulls the next token from a form.
  A list containing the token and the rest of the form is returned."
  (let [token (first form)
        tail  (rest form)]
    [token tail]))

(defn- pull-entry [form]
  "Parses an entry from a form.
  If successful, returns an entry and the rest of the form in a list."
  (let [[field tail]  (pull-token form)
        [kind tail]   (pull-token tail)
        [size tail]   (pull-token tail)
        entry         (if-not size
                        (->ScalarEntry field kind)
                        (->ArrayEntry field kind size))]
    (if (entry->valid? entry)
      [entry tail])))

; Exports

(defn to-entries [form]
  "Parses a list of packet field entries from a form."
  (loop [entries  []
         form form]
    (let [[entry tail] (pull-entry form)]
      (if-not entry
        entries
        (recur (conj entries entry) tail)))))
