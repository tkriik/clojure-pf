(ns clojure-pf.core.buffer
  "Tailored buffer functions for I/O, serialization and deserialization."
  (:import  (java.nio ByteBuffer))
  (:require [clojure-pf.core.form   :refer :all]
            [clojure-pf.core.packet :refer :all]))

(defn allocate [size]
  "Returns a byte buffer of given size."
  (ByteBuffer/allocate size))

(defn quantity-left [buffer form]
  "Returns the quantity of values available in a buffer for a form."
  (min (quantity form) (.remaining buffer)))

(defn next-form [buffer form]
  "Returns a buffer with its position incremented by the memory size a form."
  (let [index (.position buffer)
        size  (min (.remaining buffer) (octets form))]
    (.position buffer (+ index size))))

(defn with-form [buffer form]
  "Returns a buffer associated with a form."
  (case (:kind form)
    :byte   (.duplicate buffer)
    :short  (.asShortBuffer buffer)
    :int    (.asIntBuffer buffer)
    :long   (.asLongBuffer buffer)
    :float  (.asFloatBuffer buffer)
    :double (.asDoubleBuffer buffer)))

(defn with-region [buffer region]
  "Returns a buffer with the position and limit set according
  to a raw packet region."
  (let [index (:index region)
        size  (:size region)
        limit (+ index size)]
    (-> (.duplicate buffer)
        (.position index)
        (.limit limit))))
