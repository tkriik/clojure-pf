(ns clojure-pf.packet
  "Packet serialization and deserialization functions."
  (:require [clojure-pf.form :as form])
  (:import  (java.nio ByteBuffer)))

; Packet definition used in the application layer.
(deftype Packet [header
                 data])

; BPF header information.
(deftype Header [timestamp
                 capture-size
                 data-size
                 header-size])

; BPF header timestamp.
(deftype Timestamp [seconds
                    microseconds])

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
           (value))
    :int (num (case size
                1 (.get buffer)
                2 (.getShort buffer)
                4 (.getInt buffer)
                8 (.getLong buffer)))))

(defn- deserialize-header [buffer]
  "Deserializes a BPF header timestamp and packet size
  information from the given buffer."
  (let [seconds       (.getInt buffer)
        microseconds  (.getInt buffer)
        timestamp     (->Timestamp seconds microseconds)
        capture-size  (.getInt buffer)
        data-size     (.getInt buffer)
        header-size   (.getShort buffer)]
    (->Header timestamp
              capture-size
              data-size
              header-size)))

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

(defn- deserialize-packet [buffer form]
  "Deserializes a packet from a buffer according to the given form."
  (loop [buffer   buffer
         entries  (form/entry-list form)
         packet   {}]
    (if (empty? entries)
      packet
      (let [entry (first entries)
            field (:field entry)
            _type (:type entry)
            size  (:size entry)
            value (deserialize-value buffer _type size)]
        (recur buffer
               (rest entries)
               (assoc packet field value))))))

(defn deserialize [data form]
  "Deserializes multiple packets along with their BPF headers
  according to the given form."
  (loop [data     data
         packets  []]
    (if (empty? data)
      packets
      (let [header-buffer (ByteBuffer/wrap data)
            header        (deserialize-header header-buffer)
            header-size   (.header-size header)
            capture-size  (.capture-size header)
            packet-size   (+ header-size capture-size)
            packet-buffer (ByteBuffer/wrap data header-size capture-size)
            packet-data   (deserialize-packet packet-buffer form)
            packet        (->Packet header packet-data)]
        (recur (drop packet-size data)
               (conj packets packet))))))
