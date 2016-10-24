(ns clojure-pf.core.form
  "Packet form types and parsing."
  (:require [clojure.algo.monads :refer :all]))

(defprotocol Form
  "Packet form methods."
  (octets   [this] "Returns the memory size reserved by form.")
  (quantity [this] "Returns the quantity of values in a form"))

; Type definitions

(defn- kind->octets [kind]
  "Returns the memory size reserved by a kind."
  (case kind
    :byte   1
    :short  2
    :int    4
    :float  4
    :long   8
    :double 8))

(defrecord ScalarForm [field kind]
  Form
  (octets   [this] (kind->octets (:kind this)))
  (quantity [this] 1))

(defrecord ArrayForm [field kind quantity]
  Form
  (octets   [this] (* (kind->octets (:kind this)) (:quantity this)))
  (quantity [this] (:quantity this)))

(defrecord SubForm [field forms]
  Form
  (octets   [this] (->> (:forms this) (map octets) (reduce +)))
  (quantity [this] (->> (:forms this) (map quantity) (reduce +))))

; Form parsing utilities

(defn- get-token [[token & tokens] pred]
  "Reads a token by applying a predicate."
  (if (pred token)
    [token tokens]))

(def ^:private get-field
  "Reads a field from a token list."
  #(get-token % keyword?))

(def ^:private get-kind
  "Reads a data kind from a token list."
  #(get-token % (partial contains? #{:byte :short :int :long :float :double})))

(def ^:private get-quantity
  "Reads a data quantity from a token list."
  #(get-token % (fn [n] (and (number? n) (pos? n)))))

(def ^:private get-vector
  "Reads a vector from a token list."
  #(get-token % vector?))

(def ^:private maybe-state-m (state-t maybe-m))

(declare get-forms)

(with-monad maybe-state-m
  (def ^:private get-scalar
    "Returns a scalar form from a token list along with the updated list."
    (domonad [field  get-field
              kind   get-kind]
      (->ScalarForm field kind)))
  (def ^:private get-array
    "Returns an array form from a token list along with the updated list."
    (domonad [field    get-field
              kind     get-kind
              quantity get-quantity]
      (->ArrayForm field kind quantity)))
  (def ^:private get-subform
    "Returns a subform from a token list along with the updated list."
    (domonad [field   get-field
              subform get-vector
              tokens  (set-state subform)
              forms   get-forms
              _       (set-state tokens)]
      (->SubForm field forms))))

(with-monad maybe-m
  (defn- get-form [tokens]
    "Returns one form from a token list along with the updated list."
    (m-plus (get-array tokens)
            (get-scalar tokens)
            (get-subform tokens))))

(with-monad state-m
  (def ^:private get-forms
    "Returns a list of forms from a token list along with an empty token list."
    (domonad [form  get-form
              :when form
              forms get-forms]
      (conj forms form))))

(defn to-forms [tokens]
  "Parses a list of packet field forms from a vector of form tokens.

  A packet field form specifies how values are:
    1. Serialized and deserialized when reading and writing packets.
    2. Handled when compiling packet filtering programs.

  There are 3 types of field forms:
    1. Scalar forms, which associate a scalar value with a field.
    2. Array forms, which associate an array of scalar values with a field.
    3. Subforms, which associate a structure with a field.

  Every field form must start with a keyword denothing the field name.
  The type of a field form is described by the subsequent tokens:
    1. An array field form is composed of, in addition to a field name,
       a keyword denoting a data kind and a number denoting a quantity.
    2. A subform is composed of, in addition to a field name,
       a vector, which must contain one or more field forms.

  The tokens are additionally subject to following constraints:
    1. A data kind must be one of the following keywords:
       :byte, :short, :int, :long, :float, :double
    2. A quantity must be 1 or greater.
    3. A subform must be a list of valid forms according to these same rules.

  The following packet field form list specifies the structure of
  an L2 IPv4-over-Ethernet ARP packet, which contains two subforms
  denoting the structure of an Etherned header and ARP payload, respectively:

  [:eth [:dst   :byte   6 ; array of 6 bytes (MAC address)
         :src   :byte   6
         :type  :short]   ; 16-bit integer
   :arp [:htype :short
         :ptype :short
         :hlen  :byte     ; single byte
         :plen  :byte
         :oper  :short
         :sha   :byte   6
         :spa   :byte   4 ; array of 4 bytes (IPv4 address)
         :tha   :byte   6
         :tpa   :byte   4]]"
  (-> (get-forms tokens)
      first))
