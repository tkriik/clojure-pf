(ns clojure-pf.core.binary
  "Packet serialization and deserialization functions."
  (:require [clojure-pf.core.buffer :as     buffer]
            [clojure-pf.core.form   :refer  :all]
            [clojure-pf.core.packet :refer  :all])
  (:import  (java.nio ByteBuffer)
            (clojure_pf.core.form  ScalarForm ArrayForm SubForm)))

; Deserialization utilities

(defn- form->array [form size]
  "Returns a Java array associated with a form."
  (case (:kind form)
    :byte   (byte-array size)
    :short  (short-array size)
    :int    (int-array size)
    :long   (long-array size)
    :float  (float-array size)
    :double (double-array size)))

(defmulti deserialize-form class)

(defmethod deserialize-form ScalarForm [form]
  (fn [buffer]
   (let [buffer'        (buffer/with-form buffer form)
         quantity-left  (buffer/quantity-left buffer' form)]
     (if (pos? quantity-left)
       (let [value (.get buffer')]
         (buffer/next-form buffer form)
         value)))))

(defmethod deserialize-form ArrayForm [form]
  (fn [buffer]
    (let [buffer'       (buffer/with-form buffer form)
          quantity-left (buffer/quantity-left buffer' form)]
      (if (pos? quantity-left)
        (let [array (form->array form quantity-left)
              _     (.get buffer' array)]
          (buffer/next-form buffer form)
          (vec array))))))

(declare deserialize-forms)

(defmethod deserialize-form SubForm [form]
  (fn [buffer]
    (deserialize-forms buffer (:forms form))))

(defn deserialize-forms [buffer forms]
  "Deserializes a list of forms from a buffer."
  (let [fields  (map :field forms)
        values  (map #((deserialize-form %) buffer) forms)]
    (zipmap fields values)))

(defn deserialize [raw-input-packet forms]
  "Deserializes one or more packets from a RawInputPacket
  according to a list of forms.
  Returns a list of destructured packets on success."
  (let [timestamps      (:timestamps raw-input-packet)
        buffer          (:buffer raw-input-packet)
        payload-regions (:payload-regions raw-input-packet)
        payload-buffers (map (partial buffer/with-region buffer)
                             payload-regions)
        payloads        (map deserialize-forms payload-buffers (repeat forms))]
    (map ->Packet timestamps payloads)))

(defn serialize [payload forms]
  nil)

;(defn to-raw [payload form]
;  "Serializes a payload according to form.
;  Returns a RawOutputPacket on success."
;  (let [payload-size  (size form)
;        buffer        (->> (ByteBuffer/allocate payload-size)
;                           (serialize form payload))]
;    (if buffer
;      (let [data            (.array buffer)
;            payload-region  (->RawPacketRegion 0 (.position buffer))]
;        (->RawOutputPacket data payload-region)))))
