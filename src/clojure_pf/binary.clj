(ns clojure-pf.binary
  "Packet serialization and deserialization functions."
  (:import  (java.nio ByteBuffer ByteOrder))
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

(defn- deserialize-timestamp [buffer region]
  "Deserializes a BPF header timestamp."
  (let [buffer (-> (regional-buffer buffer region)
                   (.order ByteOrder/LITTLE_ENDIAN))]
    (try
      (let [seconds       (-> (.getInt buffer)
                              (bit-and 0xFFFFFFFF))
            microseconds  (-> (.getInt buffer)
                              (bit-and 0xFFFFFFFF)
                              (mod (* 1000 1000)))]
        (->Timestamp seconds microseconds)))))

(defn- deserialize-value [buffer entry]
  "Deserializes a value from a buffer according to a form entry."
  (try
    (let [kind            (:kind entry)
          size            (:size entry)
          [buffer array]  (case kind
                            :byte  [buffer (byte-array size)]
                            :short [(.asShortBuffer buffer) (short-array size)]
                            :int   [(.asIntBuffer buffer) (int-array size)]
                            :long  [(.asLongBuffer buffer) (long-array size)])]
      (if (= size 1)
        (.get buffer)
        (do
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

(defn deserialize [raw-packet entries]
  "Deserializes one or more packets from a RawPacket according
  to given entries. Returns a list of destructured packets on success."
  (let [buffer (-> raw-packet
                   :data
                   ByteBuffer/wrap
                   .asReadOnlyBuffer)]
    (loop [header-regions  (:header-regions raw-packet)
           payload-regions (:payload-regions raw-packet)
           packets []]
      (if (empty? payload-regions)
        packets
        (let [header-region   (first header-regions)
              payload-region  (first payload-regions)
              timestamp       (if header-region
                                (deserialize-timestamp buffer header-region))
              payload         (deserialize-payload buffer
                                                   payload-region
                                                   entries)
              packet          (->Packet timestamp payload)]
          (recur (rest header-regions)
                 (rest payload-regions)
                 (conj packets packet)))))))
