(ns clojure-pf.core.binary
  "Packet serialization and deserialization functions."
  (:import (java.nio ByteBuffer ByteOrder))
  (:require [clojure-pf.core.form    :as     form]
            [clojure-pf.core.packet  :refer  :all]))

; Deserialization utilities

(defn- regional-buffer [buffer region]
  "Returns a buffer with the position and limit set according to a region."
  (let [index (:index region)
        size  (:size region)]
    (-> (.asReadOnlyBuffer buffer)
        (.position index)
        (.limit (+ index size)))))

(defn- deserialize-pair [buffer entry]
  "Deserializes a key-value pair from a buffer according to a form entry."
  (let [kind          (:kind entry)
        [buffer
         array-ctor]  (case kind
                        :byte   [buffer                   byte-array]
                        :short  [(.asShortBuffer buffer)  short-array]
                        :int    [(.asIntBuffer buffer)    int-array]
                        :long   [(.asLongBuffer buffer)   long-array]
                        :float  [(.asFloatBuffer buffer)  float-array]
                        :double [(.asDoubleBuffer buffer) double-array])
        remaining     (.remaining buffer)]
    (if (pos? remaining)
      (let [field (:field entry)
            value (case (form/entry->type entry)
                    :scalar (.get buffer)
                    :array  (let [size  (min (:size entry) remaining)
                                  array (array-ctor size)]
                              (.get buffer array)
                              array))]
        {field value}))))

(defn- deserialize-payload [buffer entries region]
  "Deserializes a packet payload according to a form entry list and region."
  (let [buffer (regional-buffer buffer region)]
    (->> entries
         (map (partial deserialize-pair buffer))
         (apply merge))))

; Deserialization exports

(defn deserialize [raw-input-packet entries]
  "Deserializes one or more packets from a RawInputPacket according
  to given entries. Returns a list of destructured packets on success."
  (let [buffer          (-> (:data raw-input-packet)
                            ByteBuffer/wrap
                            .asReadOnlyBuffer)
        timestamps      (:timestamps raw-input-packet)
        payloads        (->> (:payload-regions raw-input-packet)
                             (map (partial deserialize-payload
                                           buffer
                                           entries)))]
    (map ->Packet timestamps payloads)))

; Serialization utiliies

(defn- serialize-value [value buffer entry]
  "Serializes a value to a buffer according to an entry."
  (let [kind      (:kind entry)
        buffer    (case kind
                    :byte   buffer
                    :short  (.asShortBuffer buffer)
                    :int    (.asIntBuffer buffer)
                    :long   (.asLongBuffer buffer)
                    :float  (.asFloatBuffer buffer)
                    :double (.asDoubleBuffer buffer))
        remaining (.remaining buffer)]
    (if (pos? remaining)
      (case (form/entry->type entry)
        :scalar (.put buffer value)
        :array  (let [size (min (:size entry) remaining)]
                  (.put buffer value 0 size))))))

; Serialization exports

(defn serialize [packet entries]
  "Serializes a packet payload according to a list of entries.
  Returns a RawOutputPacket on success."
  (let [packet-size (reduce + (map :size entries))
        buffer      (ByteBuffer/allocate packet-size)]
    (loop [entries entries]
      (if-not (empty? entries)
        (let [entry (first entries)
              field (:field entry)
              value (get packet field)]
          (if (serialize-value value buffer entry)
            (recur (rest entries))))))
    (let [data          (.array buffer)
          packet-region (->RawPacketRegion 0 (.position buffer))]
      (->RawOutputPacket data packet-region))))
