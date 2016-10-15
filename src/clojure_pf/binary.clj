(ns clojure-pf.binary
  "Packet serialization and deserialization functions."
  (:import  (java.nio ByteBuffer ByteOrder))
  (:require [clojure-pf.form    :as     form]
            [clojure-pf.packet  :refer  :all]))

; Deserialization utilities

(defn- bounded-buffer [buffer boundary]
  "Returns a buffer with the position and limit set according to a boundary."
  (let [index (.index boundary)
        size  (.size boundary)]
    (-> (.asReadOnlyBuffer buffer)
        (.position index)
        (.limit (+ index size)))))

(defn- deserialize-timestamp [buffer boundary]
  "Deserializes a BPF header timestamp if boundary is defined."
  (if boundary
    (let [buffer (-> (bounded-buffer buffer boundary)
                     (.order ByteOrder/LITTLE_ENDIAN))]
      (try
        (let [seconds       (-> (.getInt buffer)
                                (bit-and 0xFFFFFFFF))
              microseconds  (-> (.getInt buffer)
                                (bit-and 0xFFFFFFFF)
                                (mod (* 1000 1000)))]
          {:seconds       seconds
           :microseconds  microseconds})
        (catch Exception e
          (println "deserialize-timestamp: " (.getMessage e)))))))

(defn- deserialize-value [buffer entry]
  "Deserializes a value from a buffer according to a form entry."
  (try
    (case (:kind entry)
      :buf (let [value (byte-array (:size entry))]
             (.get buffer value)
             vec value)
      :int (case (:size entry)
             1 (.get buffer)
             2 (.getShort buffer)
             4 (.getInt buffer)
             8 (.getLong buffer)))
    (catch Exception e
      (println "deserialize-value: " (.getMessage e)))))

(defn- deserialize-payload [buffer boundary entries]
  "Deserializes a packet payload according to given boundary and form entries."
  (let [buffer (bounded-buffer buffer boundary)]
    (loop [entries entries
           payload {}]
      (if (empty? entries)
        payload
        (let [entry (first entries)
              value (deserialize-value buffer entry)]
          (recur (rest entries)
                 (assoc payload (:field entry) value)))))))

; Deserialization exports

(defn deserialize [raw-packet entries]
  "Deserializes one or more packets from a RawPacket according
  to given entries. Returns a list of Packet objects on success."
  (let [buffer (-> (.data raw-packet)
                   (ByteBuffer/wrap)
                   .asReadOnlyBuffer)]
    (loop [header-boundaries  (.header-boundaries raw-packet)
           payload-boundaries (.payload-boundaries raw-packet)
           packets []]
      (if (empty? payload-boundaries)
        packets
        (let [header-boundary   (first header-boundaries)
              payload-boundary  (first payload-boundaries)
              timestamp         (deserialize-timestamp buffer
                                                       header-boundary)
              payload           (deserialize-payload buffer
                                                     payload-boundary
                                                     entries)
              packet            {:time    timestamp
                                 :payload payload}]
          (recur (rest header-boundaries)
                 (rest payload-boundaries)
                 (conj packets packet)))))))

; Serialization utilities

(defn- serialize-value [value buffer entry]
  "Serializes a value to a buffer according to a form entry."
  (try
    (case (:kind entry)
      :buf (-> (take (:size entry) value)
               byte-array
               (.put buffer))
      :int (case (:size entry)
             1 (-> (byte value)
                   (.put buffer))
             2 (-> (short value)
                   (.putShort buffer))
             4 (-> (int value)
                   (.putInt buffer))
             8 (-> (long value)
                   (.putLong buffer))))
    (catch Exception e
      (println "serialize-value: " (.getMessage e)))))

; Serializtion exports

(defn serialize [packet form capacity]
  "Serializes a packet map according to a form."
  (loop [buffer   (ByteBuffer/allocate capacity)
         entries  (form/to-entries form)]
    (if (empty? entries)
      (.array buffer)
      (let [entry   (first entries)
            value   (get packet (:field entry))
            buffer  (serialize-value value buffer entry)]
        (if buffer
          (recur buffer (rest entries)))))))
