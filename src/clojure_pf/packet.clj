(ns clojure-pf.packet
  "Packet type definitions.")

; Destructured packet used in the application layer.
(defrecord Packet [timestamp payload])

; Packet-specific timestamp.
(defrecord Timestamp [seconds microseconds])

; Raw packet type returned from socket/device reads, containing
; data for one or more packets.
(defrecord RawPacket [data
                      header-regions
                      payload-regions])

; Region type for denoting the start index and size of a
; packet header or payload.
(defrecord RawPacketRegion [index size])
