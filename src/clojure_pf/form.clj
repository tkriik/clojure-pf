(ns clojure-pf.form
  "Module to retrieve useful information from user-defined packet forms")

; Packet field entry that defines the order, type and size of a
; single field in a packet.
(defrecord Entry [field
                  order
                  kind
                  size])

; Validation utilities

(defn- field->valid? [field]
  "Tests whether a field is valid."
  (keyword? field))

(defn- order->valid? [order]
  "Tests whether a field order is valid."
  (contains? #{:le :be} order))

(defn- kind->valid? [kind]
  "Tests whether a field kind is valid."
  (contains? #{:byte :short :int :long :float :double} kind))

(defn- size->valid? [size]
  "Tests whether a field size is valid."
  (and (number? size)
       (pos? size)))

(defn- entry->valid? [entry]
  "Tests whether a form entry is valid."
  (and (field->valid? (:field entry))
       (order->valid? (:order entry))
       (kind->valid?  (:kind entry))
       (size->valid?  (:size entry))))

; Form token utilities

(defn- pull-token [raw-form]
  "Pulls the next token from a raw form.
  A list containing the token and the rest of the form is returned."
  (let [token (first raw-form)]
    (if token
      [token (rest raw-form)])))

(defn- pull-order [raw-form]
  "Pulls a keyword denoting an order from a raw form.
  If the order is valid, it's returned along with the rest of the form.
  Otherwise a default order and the original form is returned."
  (let [[order raw-form-tail] (pull-token raw-form)]
    (if (order->valid? order)
      [order raw-form-tail]
      [:be raw-form])))

(defn- pull-kind [raw-form]
  "Pulls a keyword denoting the kind of a field from a raw form.
  If the kind is valid, it's returned along with the rest of the form.
  Otherwise nil is returned."
  (let [[kind raw-form-tail] (pull-token raw-form)]
    (if (kind->valid? kind)
      [kind raw-form-tail])))

(defn- pull-size [raw-form]
  "Pulls an optional number denoting the size of a field from a raw form.
  If the size is valid, it's returned along with the rest of the form.
  Otherwise one is returned along with the original form."
  (let [[size raw-form-tail] (pull-token raw-form)]
    (if (size->valid? size)
      [size raw-form-tail]
      [1 raw-form])))

(defn- pull-entry [raw-form]
  "Parses an entry from a raw form.
  If successful, returns an entry and the rest of the form in a list.
  Otherwise nil is returned along with the original form."
  (let [[field raw-form-tail] (pull-token raw-form)
        [order raw-form-tail] (pull-order raw-form-tail)
        [kind raw-form-tail]  (pull-kind raw-form-tail)
        [size raw-form-tail]  (pull-size raw-form-tail)
        entry                 (Entry. field order kind size)]
    (if (entry->valid? entry)
      [entry raw-form-tail]
      [nil raw-form])))

; Exports

(defn to-entries [raw-form]
  "Parses a list of packet field entries from a form."
  (loop [entries  []
         raw-form raw-form]
    (let [[entry raw-form-tail] (pull-entry raw-form)]
      (if-not entry
        entries
        (recur (conj entries entry)
               raw-form-tail)))))
