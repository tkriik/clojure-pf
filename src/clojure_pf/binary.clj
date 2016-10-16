(ns clojure-pf.binary
  "Packet serialization and deserialization functions."
  (:import (java.nio ByteBuffer ByteOrder))
  (:require [clojure-pf.form    :as     form]
            [clojure-pf.packet  :refer  :all]))

; Deserialization utilities

(defn- regional-buffer [buffer region]
  "Returns a buffer with the position and limit set according to a region."
  (let [index (:index region)
        size  (:size region)]
    (-> (.asReadOnlyBuffer buffer)
        (.position index)
        (.limit (+ index size)))))

(defn- deserialize-value [buffer entry]
  "Deserializes a value from a buffer according to a form entry."
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
      (case (form/entry->type entry)
        :scalar (.get buffer)
        :array  (let [size  (min (:size entry) remaining)
                      array (array-ctor size)]
                  (.get buffer array)
                  array)))))

(defn- deserialize-payload [buffer region entries]
  "Deserializes a packet payload according to a given region and form entries."
  (let [buffer (regional-buffer buffer region)]
    (loop [entries entries
           payload {}]
      (if (empty? entries)
        payload
        (let [entry (first entries)
              value (deserialize-value buffer entry)]
          (recur (rest entries)
                 (assoc payload (:field entry) value)))))))

; Deserialization exports

(defn deserialize [raw-input-packet entries]
  "Deserializes one or more packets from a RawInputPacket according
  to given entries. Returns a list of destructured packets on success."
  (let [buffer (-> raw-input-packet
                   :data
                   ByteBuffer/wrap
                   .asReadOnlyBuffer)]
    (loop [payload-regions  (:payload-regions raw-input-packet)
           timestamps       (:timestamps raw-input-packet)
           packets          []]
      (if (empty? payload-regions)
        packets
        (let [payload-region  (first payload-regions)
              timestamp       (first timestamps)
              payload         (deserialize-payload buffer
                                                   payload-region
                                                   entries)
              packet          (->Packet timestamp payload)]
          (recur (rest payload-regions)
                 (rest timestamps)
                 (conj packets packet)))))))

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
