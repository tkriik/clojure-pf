(ns clojure-pf.packet
  "Packet type definitions.")

; Destructured packet constructor used in the application layer.
(defn ->Packet [timestamp payload]
  {:timestamp timestamp
   :payload   payload})

; Packet-specific timestamp constructor.
(defn ->Timestamp [seconds microseconds]
  {:seconds       seconds
   :microseconds  microseconds})

; Raw packet type returned, from socket/device reads, which contains
; data for one or more packets.
(deftype RawPacket [data
                    header-regions
                    payload-regions])

; Region type for denoting the start index and size of a
; packet header or payload.
(deftype RawPacketRegion [index size])
