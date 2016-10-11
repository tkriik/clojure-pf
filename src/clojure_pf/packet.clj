(ns clojure-pf.packet
  "Packet serialization and deserialization functions."
  (:require [clojure-pf.form :as form])
  (:import  (java.nio ByteBuffer)))

; Utilities

(defn- serialize-value [buffer value _type size]
  "Serializes a value with a type and size to the given buffer."
  (case _type
    :buf (.put buffer (->> value (take size) (map byte) byte-array))
    :int (case size
           1 (.put buffer (byte value))
           2 (.putShort buffer (short value))
           4 (.putInt buffer (int value))
           8 (.putLong buffer (long value))
           _ buffer)
    _    buffer))

(defn- deserialize-value [buffer _type size]
  "Deserializes a value with a type and size from the given buffer."
  (case _type
    :buf (let [value (byte-array size)]
           (.get buffer value)
           (vec value))
    :int (num (case size
                1 (.get buffer)
                2 (.getShort buffer)
                4 (.getInt buffer)
                8 (.getLong buffer)))))

; Exports

(defn serialize [packet form capacity]
  "Serializes a packet map according to the given form."
  (loop [buffer   (ByteBuffer/allocate capacity)
         entries  (form/entry-list form)]
    (if (empty? entries)
      (.array buffer)
      (let [entry   (first entries)
            field   (:field entry)
            _type   (:type entry)
            size    (:size entry)
            value   (field packet)]
        (recur (serialize-value buffer value _type size)
               (rest entries))))))

(defn deserialize [data form]
  "Deserializes a packet according to the given form."
  (loop [buffer   (ByteBuffer/wrap data)
         entries  (form/entry-list form)
         packet   {}]
    (if (empty? entries)
      packet
      (let [entry   (first entries)
            field   (:field entry)
            _type   (:type entry)
            size    (:size entry)
            value   (deserialize-value buffer _type size)]
        (recur buffer
               (rest entries)
               (assoc packet field value))))))
