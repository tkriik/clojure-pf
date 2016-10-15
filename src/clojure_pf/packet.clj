(ns clojure-pf.packet
  "Packet type definitions.")

; Destructured packet used in the application layer.
(defrecord Packet [timestamp payload])

; Packet-specific timestamp.
(defrecord Timestamp [seconds microseconds])

; Record type returned from socket/device reads, containing data
; for one or more packets.
(defrecord RawPacket [data
                      timestamps
                      payload-regions])

; Record type for denoting the start index and size of a packet payload.
(defrecord RawPacketRegion [index size])
